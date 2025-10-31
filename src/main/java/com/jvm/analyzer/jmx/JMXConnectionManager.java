package com.jvm.analyzer.jmx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages JMX connections to local and remote JVMs
 */
public class JMXConnectionManager {
    private static final Logger logger = LoggerFactory.getLogger(JMXConnectionManager.class);

    private MBeanServerConnection mbeanConnection;
    private JMXConnector jmxConnector;
    private boolean isLocalConnection = false;

    /**
     * Connect to local JVM using PID
     */
    public void connectLocal(long pid) throws IOException {
        try {
            // For local connection, we use the attach API
            String address = getLocalConnectorAddress(pid);

            if (address == null) {
                throw new IOException("Unable to get JMX connector address for PID: " + pid);
            }

            JMXServiceURL serviceURL = new JMXServiceURL(address);
            jmxConnector = JMXConnectorFactory.connect(serviceURL);
            mbeanConnection = jmxConnector.getMBeanServerConnection();
            isLocalConnection = true;

            logger.info("Successfully connected to local JVM (PID: {})", pid);
        } catch (Exception e) {
            logger.error("Failed to connect to local JVM", e);
            throw new IOException("Failed to connect to local JVM: " + e.getMessage(), e);
        }
    }

    /**
     * Connect to remote JVM using host and port
     */
    public void connectRemote(String host, int port) throws IOException {
        connectRemote(host, port, null, null);
    }

    /**
     * Connect to remote JVM with authentication
     */
    public void connectRemote(String host, int port, String username, String password)
            throws IOException {
        try {
            String url = String.format("service:jmx:rmi:///jndi/rmi://%s:%d/jmxrmi", host, port);
            JMXServiceURL serviceURL = new JMXServiceURL(url);

            Map<String, Object> environment = new HashMap<>();
            if (username != null && password != null) {
                String[] credentials = {username, password};
                environment.put(JMXConnector.CREDENTIALS, credentials);
            }

            jmxConnector = JMXConnectorFactory.connect(serviceURL, environment);
            mbeanConnection = jmxConnector.getMBeanServerConnection();
            isLocalConnection = false;

            logger.info("Successfully connected to remote JVM at {}:{}", host, port);
        } catch (Exception e) {
            logger.error("Failed to connect to remote JVM", e);
            throw new IOException("Failed to connect to remote JVM: " + e.getMessage(), e);
        }
    }

    /**
     * Connect to current JVM (useful for testing)
     */
    public void connectToCurrent() {
        mbeanConnection = ManagementFactory.getPlatformMBeanServer();
        isLocalConnection = true;
        logger.info("Connected to current JVM");
    }

    /**
     * Get the MBean server connection
     */
    public MBeanServerConnection getConnection() {
        return mbeanConnection;
    }

    /**
     * Check if connected
     */
    public boolean isConnected() {
        return mbeanConnection != null;
    }

    /**
     * Disconnect from JVM
     */
    public void disconnect() {
        if (jmxConnector != null) {
            try {
                jmxConnector.close();
                logger.info("Disconnected from JVM");
            } catch (IOException e) {
                logger.warn("Error closing JMX connector", e);
            }
        }
        mbeanConnection = null;
        jmxConnector = null;
    }

    /**
     * Get local connector address for a given PID
     * Uses the attach API to get the JMX service URL
     */
    private String getLocalConnectorAddress(long pid) throws IOException {
        try {
            // Use VirtualMachine.attach() to connect to the process
            Class<?> vmClass = Class.forName("com.sun.tools.attach.VirtualMachine");
            Object vm = vmClass.getMethod("attach", String.class)
                    .invoke(null, String.valueOf(pid));

            // Get the connector address
            String connectorAddress = (String) vmClass.getMethod("getAgentProperties")
                    .invoke(vm);

            if (connectorAddress == null) {
                // Start the management agent
                String agent = vmClass.getMethod("getSystemProperties")
                        .invoke(vm)
                        .toString();
                String home = System.getProperty("java.home");
                vmClass.getMethod("loadAgent", String.class)
                        .invoke(vm, home + "/lib/management-agent.jar");

                connectorAddress = (String) vmClass.getMethod("getAgentProperties")
                        .invoke(vm);
            }

            // Detach from the VM
            vmClass.getMethod("detach").invoke(vm);

            return connectorAddress;

        } catch (ClassNotFoundException e) {
            logger.warn("Attach API not available, trying alternative method");
            return tryAlternativeConnection(pid);
        } catch (Exception e) {
            logger.error("Error getting local connector address", e);
            throw new IOException("Failed to get local connector address", e);
        }
    }

    /**
     * Alternative connection method when attach API is not available
     */
    private String tryAlternativeConnection(long pid) throws IOException {
        // Try to read connector address from temp file
        String tmpDir = System.getProperty("java.io.tmpdir");
        String fileName = tmpDir + "/.java_pid" + pid;

        try {
            java.nio.file.Path path = java.nio.file.Paths.get(fileName);
            if (java.nio.file.Files.exists(path)) {
                String content = new String(java.nio.file.Files.readAllBytes(path));
                // Parse the file for connector address
                for (String line : content.split("\n")) {
                    if (line.startsWith("sun.management.JMXConnectorServer.address=")) {
                        return line.substring(line.indexOf('=') + 1).trim();
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Alternative connection method failed", e);
        }

        return null;
    }

    /**
     * Test the connection
     */
    public boolean testConnection() {
        if (!isConnected()) {
            return false;
        }

        try {
            // Try to get a simple metric to verify connection
            mbeanConnection.getMBeanCount();
            return true;
        } catch (Exception e) {
            logger.error("Connection test failed", e);
            return false;
        }
    }
}