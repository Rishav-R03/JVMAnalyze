package com.jvm.analyzer.jmx;

import com.jvm.analyzer.model.JVMMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.*;
import javax.management.openmbean.CompositeData;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Collects JVM performance metrics via JMX
 */
public class MetricCollector {
    private static final Logger logger = LoggerFactory.getLogger(MetricCollector.class);

    private final JMXConnectionManager connectionManager;
    private final MBeanServerConnection mbeanConnection;

    // MBean ObjectNames
    private static final String MEMORY_MXBEAN = "java.lang:type=Memory";
    private static final String THREADING_MXBEAN = "java.lang:type=Threading";
    private static final String CLASSLOADING_MXBEAN = "java.lang:type=ClassLoading";
    private static final String OPERATING_SYSTEM_MXBEAN = "java.lang:type=OperatingSystem";
    private static final String RUNTIME_MXBEAN = "java.lang:type=Runtime";

    public MetricCollector(JMXConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
        this.mbeanConnection = connectionManager.getConnection();

        if (mbeanConnection == null) {
            throw new IllegalStateException("Not connected to JVM");
        }
    }

    /**
     * Collect all JVM metrics
     */
    public JVMMetrics collectMetrics() throws IOException {
        JVMMetrics metrics = new JVMMetrics();
        metrics.setTimestamp(LocalDateTime.now());

        try {
            collectMemoryMetrics(metrics);
            collectGarbageCollectionMetrics(metrics);
            collectThreadMetrics(metrics);
            collectClassLoadingMetrics(metrics);
            collectCpuMetrics(metrics);
            collectRuntimeMetrics(metrics);

            logger.debug("Successfully collected all metrics");
        } catch (Exception e) {
            logger.error("Error collecting metrics", e);
            throw new IOException("Failed to collect metrics", e);
        }

        return metrics;
    }

    /**
     * Collect memory-related metrics
     */
    private void collectMemoryMetrics(JVMMetrics metrics) throws Exception {
        ObjectName memoryMBean = new ObjectName(MEMORY_MXBEAN);

        // Heap Memory
        CompositeData heapMemory = (CompositeData) mbeanConnection.getAttribute(
                memoryMBean, "HeapMemoryUsage");
        metrics.setHeapUsed((Long) heapMemory.get("used"));
        metrics.setHeapCommitted((Long) heapMemory.get("committed"));
        metrics.setHeapMax((Long) heapMemory.get("max"));

        // Non-Heap Memory
        CompositeData nonHeapMemory = (CompositeData) mbeanConnection.getAttribute(
                memoryMBean, "NonHeapMemoryUsage");
        metrics.setNonHeapUsed((Long) nonHeapMemory.get("used"));
        metrics.setNonHeapCommitted((Long) nonHeapMemory.get("committed"));

        // Object Pending Finalization
        Integer pendingFinalization = (Integer) mbeanConnection.getAttribute(
                memoryMBean, "ObjectPendingFinalizationCount");
        metrics.setObjectsPendingFinalization(pendingFinalization);

        logger.debug("Collected memory metrics: Heap={}/{} MB",
                metrics.getHeapUsed() / (1024*1024),
                metrics.getHeapMax() / (1024*1024));
    }

    /**
     * Collect garbage collection metrics
     */
    private void collectGarbageCollectionMetrics(JVMMetrics metrics) throws Exception {
        Set<ObjectName> gcMBeans = mbeanConnection.queryNames(
                new ObjectName("java.lang:type=GarbageCollector,*"), null);

        long youngGcCount = 0;
        long youngGcTime = 0;
        long oldGcCount = 0;
        long oldGcTime = 0;

        for (ObjectName gcMBean : gcMBeans) {
            String gcName = (String) mbeanConnection.getAttribute(gcMBean, "Name");
            Long gcCount = (Long) mbeanConnection.getAttribute(gcMBean, "CollectionCount");
            Long gcTime = (Long) mbeanConnection.getAttribute(gcMBean, "CollectionTime");

            // Classify GC as young or old generation
            if (isYoungGenCollector(gcName)) {
                youngGcCount += gcCount;
                youngGcTime += gcTime;
            } else {
                oldGcCount += gcCount;
                oldGcTime += gcTime;
            }

            logger.debug("GC Collector: {} - Count: {}, Time: {} ms", gcName, gcCount, gcTime);
        }

        metrics.setYoungGcCount(youngGcCount);
        metrics.setYoungGcTime(youngGcTime);
        metrics.setOldGcCount(oldGcCount);
        metrics.setOldGcTime(oldGcTime);
    }

    /**
     * Determine if a GC collector is for young generation
     */
    private boolean isYoungGenCollector(String gcName) {
        String lowerName = gcName.toLowerCase();
        return lowerName.contains("young") ||
                lowerName.contains("scavenge") ||
                lowerName.contains("parnew") ||
                lowerName.contains("copy");
    }

    /**
     * Collect thread-related metrics
     */
    private void collectThreadMetrics(JVMMetrics metrics) throws Exception {
        ObjectName threadingMBean = new ObjectName(THREADING_MXBEAN);

        Integer threadCount = (Integer) mbeanConnection.getAttribute(
                threadingMBean, "ThreadCount");
        Integer peakThreadCount = (Integer) mbeanConnection.getAttribute(
                threadingMBean, "PeakThreadCount");
        Integer daemonThreadCount = (Integer) mbeanConnection.getAttribute(
                threadingMBean, "DaemonThreadCount");
        Long totalStartedThreadCount = (Long) mbeanConnection.getAttribute(
                threadingMBean, "TotalStartedThreadCount");

        metrics.setThreadCount(threadCount);
        metrics.setPeakThreadCount(peakThreadCount);
        metrics.setDaemonThreadCount(daemonThreadCount);
        metrics.setTotalStartedThreadCount(totalStartedThreadCount);

        // Check for deadlocks
        long[] deadlockedThreads = (long[]) mbeanConnection.invoke(
                threadingMBean, "findDeadlockedThreads", null, null);

        if (deadlockedThreads != null && deadlockedThreads.length > 0) {
            logger.warn("DEADLOCK DETECTED! {} threads deadlocked", deadlockedThreads.length);
            metrics.setDeadlockedThreadCount(deadlockedThreads.length);
        } else {
            metrics.setDeadlockedThreadCount(0);
        }

        logger.debug("Collected thread metrics: {} threads ({} daemon)",
                threadCount, daemonThreadCount);
    }

    /**
     * Collect class loading metrics
     */
    private void collectClassLoadingMetrics(JVMMetrics metrics) throws Exception {
        ObjectName classLoadingMBean = new ObjectName(CLASSLOADING_MXBEAN);

        Integer loadedClassCount = (Integer) mbeanConnection.getAttribute(
                classLoadingMBean, "LoadedClassCount");
        Long totalLoadedClassCount = (Long) mbeanConnection.getAttribute(
                classLoadingMBean, "TotalLoadedClassCount");
        Long unloadedClassCount = (Long) mbeanConnection.getAttribute(
                classLoadingMBean, "UnloadedClassCount");

        metrics.setLoadedClassCount(loadedClassCount);
        metrics.setTotalLoadedClassCount(totalLoadedClassCount);
        metrics.setUnloadedClassCount(unloadedClassCount);

        logger.debug("Collected class loading metrics: {} classes loaded", loadedClassCount);
    }

    /**
     * Collect CPU usage metrics
     */
    private void collectCpuMetrics(JVMMetrics metrics) throws Exception {
        ObjectName osMBean = new ObjectName(OPERATING_SYSTEM_MXBEAN);

        try {
            // These attributes may not be available on all platforms
            Double processCpuLoad = (Double) mbeanConnection.getAttribute(
                    osMBean, "ProcessCpuLoad");
            Double systemCpuLoad = (Double) mbeanConnection.getAttribute(
                    osMBean, "SystemCpuLoad");

            metrics.setProcessCpuLoad(processCpuLoad != null ? processCpuLoad : 0.0);
            metrics.setSystemCpuLoad(systemCpuLoad != null ? systemCpuLoad : 0.0);

            logger.debug("Collected CPU metrics: Process={}%, System={}%",
                    processCpuLoad * 100, systemCpuLoad * 100);
        } catch (AttributeNotFoundException e) {
            logger.warn("CPU load attributes not available on this platform");
            metrics.setProcessCpuLoad(0.0);
            metrics.setSystemCpuLoad(0.0);
        }
    }

    /**
     * Collect runtime information
     */
    private void collectRuntimeMetrics(JVMMetrics metrics) throws Exception {
        ObjectName runtimeMBean = new ObjectName(RUNTIME_MXBEAN);

        String vmName = (String) mbeanConnection.getAttribute(runtimeMBean, "VmName");
        String vmVersion = (String) mbeanConnection.getAttribute(runtimeMBean, "VmVersion");
        Long uptime = (Long) mbeanConnection.getAttribute(runtimeMBean, "Uptime");

        metrics.setVmName(vmName);
        metrics.setVmVersion(vmVersion);
        metrics.setUptime(uptime);

        logger.debug("Collected runtime metrics: {} {} (uptime: {} ms)",
                vmName, vmVersion, uptime);
    }

    /**
     * Get detailed memory pool information
     */
    public void collectMemoryPoolMetrics(JVMMetrics metrics) throws Exception {
        Set<ObjectName> poolMBeans = mbeanConnection.queryNames(
                new ObjectName("java.lang:type=MemoryPool,*"), null);

        for (ObjectName poolMBean : poolMBeans) {
            String poolName = (String) mbeanConnection.getAttribute(poolMBean, "Name");
            CompositeData usage = (CompositeData) mbeanConnection.getAttribute(poolMBean, "Usage");

            long used = (Long) usage.get("used");
            long max = (Long) usage.get("max");

            logger.debug("Memory Pool: {} - Used: {} MB / Max: {} MB",
                    poolName, used / (1024*1024), max / (1024*1024));
        }
    }
}