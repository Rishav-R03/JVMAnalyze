package com.jvmprofiler.monitor;

import com.jvmprofiler.monitor.model.JVMMetrics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.management.*;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.openmbean.CompositeData;
import java.lang.management.MemoryUsage;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class JVMProfilerJMXConnector {
    private static final Logger logger = LogManager.getLogger(JVMProfilerJMXConnector.class);
    private JMXConnector jmxConnector;
    private MBeanServerConnection mBeanServerConnection;

    public boolean connect(int pid) {
        try {
            // For MVP, we'll try common JMX ports or use attach API simulation
            String urlString = "service:jmx:rmi:///jndi/rmi://" + "127.0.0.1:" + getJMXPort(pid) + "/jmxrmi";
            JMXServiceURL url = new JMXServiceURL(urlString);

            Map<String, Object> env = new HashMap<>();
            jmxConnector = JMXConnectorFactory.connect(url, env);
            mBeanServerConnection = jmxConnector.getMBeanServerConnection();

            logger.info("Successfully connected to JVM process: {}", pid);
            return true;

        } catch (Exception e) {
            logger.error("Failed to connect to JVM process {}: {}", pid, e.getMessage());
            return false;
        }
    }

    private String getJMXPort(int pid) {
        // Simple simulation - in real scenario, you'd parse from jps or use attach API
        // Common JMX ports: 9090, 9091, 9092, etc.
        return "9091";
    }

    public JVMMetrics collectMetrics() {
        if (mBeanServerConnection == null) {
            throw new IllegalStateException("Not connected to JVM");
        }

        try {
            JVMMetrics metrics = new JVMMetrics();
            collectMemoryMetrics(metrics);
            collectGCMetrics(metrics);
            collectThreadMetrics(metrics);
            metrics.setTimestamp(System.currentTimeMillis());

            return metrics;

        } catch (Exception e) {
            logger.error("Error collecting metrics: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to collect metrics", e);
        }
    }

    private void collectMemoryMetrics(JVMMetrics metrics) throws Exception {
        ObjectName memoryMBean = new ObjectName("java.lang:type=Memory");
        CompositeData heapMemoryUsage = (CompositeData) mBeanServerConnection.getAttribute(memoryMBean, "HeapMemoryUsage");
        CompositeData nonHeapMemoryUsage = (CompositeData) mBeanServerConnection.getAttribute(memoryMBean, "NonHeapMemoryUsage");

        // Use JDK's MemoryUsage class instead of custom one
        MemoryUsage heap = MemoryUsage.from(heapMemoryUsage);
        MemoryUsage nonHeap = MemoryUsage.from(nonHeapMemoryUsage);

        metrics.setHeapMemory(heap);
        metrics.setNonHeapMemory(nonHeap);
    }

    private void collectGCMetrics(JVMMetrics metrics) throws Exception {
        Set<ObjectName> gcMBeans = mBeanServerConnection.queryNames(new ObjectName("java.lang:type=GarbageCollector,*"), null);

        long totalGcCount = 0;
        long totalGcTime = 0;

        for (ObjectName gcMBean : gcMBeans) {
            Long collectionCount = (Long) mBeanServerConnection.getAttribute(gcMBean, "CollectionCount");
            Long collectionTime = (Long) mBeanServerConnection.getAttribute(gcMBean, "CollectionTime");

            if (collectionCount != null) totalGcCount += collectionCount;
            if (collectionTime != null) totalGcTime += collectionTime;
        }

        metrics.setGcCount(totalGcCount);
        metrics.setGcTime(totalGcTime);
    }

    private void collectThreadMetrics(JVMMetrics metrics) throws Exception {
        ObjectName threadMBean = new ObjectName("java.lang:type=Threading");

        Integer threadCount = (Integer) mBeanServerConnection.getAttribute(threadMBean, "ThreadCount");
        Integer peakThreadCount = (Integer) mBeanServerConnection.getAttribute(threadMBean, "PeakThreadCount");
        Long totalStartedThreadCount = (Long) mBeanServerConnection.getAttribute(threadMBean, "TotalStartedThreadCount");

        metrics.setThreadCount(threadCount != null ? threadCount : 0);
        metrics.setPeakThreadCount(peakThreadCount != null ? peakThreadCount : 0);
        metrics.setTotalStartedThreads(totalStartedThreadCount != null ? totalStartedThreadCount : 0);
    }

    public void disconnect() {
        if (jmxConnector != null) {
            try {
                jmxConnector.close();
                logger.info("Disconnected from JVM");
            } catch (Exception e) {
                logger.error("Error disconnecting: {}", e.getMessage());
            }
        }
    }
}