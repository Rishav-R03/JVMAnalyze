package com.jvm.analyzer.model;

import java.time.LocalDateTime;

/**
 * Data model for JVM performance metrics
 */
public class JVMMetrics {
    // Timestamp
    private LocalDateTime timestamp;

    // Memory Metrics
    private long heapUsed;
    private long heapCommitted;
    private long heapMax;
    private long nonHeapUsed;
    private long nonHeapCommitted;
    private int objectsPendingFinalization;

    // Garbage Collection Metrics
    private long youngGcCount;
    private long youngGcTime;
    private long oldGcCount;
    private long oldGcTime;

    // Thread Metrics
    private int threadCount;
    private int peakThreadCount;
    private int daemonThreadCount;
    private long totalStartedThreadCount;
    private int deadlockedThreadCount;

    // Class Loading Metrics
    private int loadedClassCount;
    private long totalLoadedClassCount;
    private long unloadedClassCount;

    // CPU Metrics
    private double processCpuLoad;
    private double systemCpuLoad;

    // Runtime Info
    private String vmName;
    private String vmVersion;
    private long uptime;

    // Constructors
    public JVMMetrics() {
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public long getHeapUsed() {
        return heapUsed;
    }

    public void setHeapUsed(long heapUsed) {
        this.heapUsed = heapUsed;
    }

    public long getHeapCommitted() {
        return heapCommitted;
    }

    public void setHeapCommitted(long heapCommitted) {
        this.heapCommitted = heapCommitted;
    }

    public long getHeapMax() {
        return heapMax;
    }

    public void setHeapMax(long heapMax) {
        this.heapMax = heapMax;
    }

    public long getNonHeapUsed() {
        return nonHeapUsed;
    }

    public void setNonHeapUsed(long nonHeapUsed) {
        this.nonHeapUsed = nonHeapUsed;
    }

    public long getNonHeapCommitted() {
        return nonHeapCommitted;
    }

    public void setNonHeapCommitted(long nonHeapCommitted) {
        this.nonHeapCommitted = nonHeapCommitted;
    }

    public int getObjectsPendingFinalization() {
        return objectsPendingFinalization;
    }

    public void setObjectsPendingFinalization(int objectsPendingFinalization) {
        this.objectsPendingFinalization = objectsPendingFinalization;
    }

    public long getYoungGcCount() {
        return youngGcCount;
    }

    public void setYoungGcCount(long youngGcCount) {
        this.youngGcCount = youngGcCount;
    }

    public long getYoungGcTime() {
        return youngGcTime;
    }

    public void setYoungGcTime(long youngGcTime) {
        this.youngGcTime = youngGcTime;
    }

    public long getOldGcCount() {
        return oldGcCount;
    }

    public void setOldGcCount(long oldGcCount) {
        this.oldGcCount = oldGcCount;
    }

    public long getOldGcTime() {
        return oldGcTime;
    }

    public void setOldGcTime(long oldGcTime) {
        this.oldGcTime = oldGcTime;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
    }

    public int getPeakThreadCount() {
        return peakThreadCount;
    }

    public void setPeakThreadCount(int peakThreadCount) {
        this.peakThreadCount = peakThreadCount;
    }

    public int getDaemonThreadCount() {
        return daemonThreadCount;
    }

    public void setDaemonThreadCount(int daemonThreadCount) {
        this.daemonThreadCount = daemonThreadCount;
    }

    public long getTotalStartedThreadCount() {
        return totalStartedThreadCount;
    }

    public void setTotalStartedThreadCount(long totalStartedThreadCount) {
        this.totalStartedThreadCount = totalStartedThreadCount;
    }

    public int getDeadlockedThreadCount() {
        return deadlockedThreadCount;
    }

    public void setDeadlockedThreadCount(int deadlockedThreadCount) {
        this.deadlockedThreadCount = deadlockedThreadCount;
    }

    public int getLoadedClassCount() {
        return loadedClassCount;
    }

    public void setLoadedClassCount(int loadedClassCount) {
        this.loadedClassCount = loadedClassCount;
    }

    public long getTotalLoadedClassCount() {
        return totalLoadedClassCount;
    }

    public void setTotalLoadedClassCount(long totalLoadedClassCount) {
        this.totalLoadedClassCount = totalLoadedClassCount;
    }

    public long getUnloadedClassCount() {
        return unloadedClassCount;
    }

    public void setUnloadedClassCount(long unloadedClassCount) {
        this.unloadedClassCount = unloadedClassCount;
    }

    public double getProcessCpuLoad() {
        return processCpuLoad;
    }

    public void setProcessCpuLoad(double processCpuLoad) {
        this.processCpuLoad = processCpuLoad;
    }

    public double getSystemCpuLoad() {
        return systemCpuLoad;
    }

    public void setSystemCpuLoad(double systemCpuLoad) {
        this.systemCpuLoad = systemCpuLoad;
    }

    public String getVmName() {
        return vmName;
    }

    public void setVmName(String vmName) {
        this.vmName = vmName;
    }

    public String getVmVersion() {
        return vmVersion;
    }

    public void setVmVersion(String vmVersion) {
        this.vmVersion = vmVersion;
    }

    public long getUptime() {
        return uptime;
    }

    public void setUptime(long uptime) {
        this.uptime = uptime;
    }

    // Utility methods
    public double getHeapUsagePercentage() {
        return heapMax > 0 ? (heapUsed * 100.0 / heapMax) : 0.0;
    }

    public long getTotalGcCount() {
        return youngGcCount + oldGcCount;
    }

    public long getTotalGcTime() {
        return youngGcTime + oldGcTime;
    }

    public double getAverageYoungGcTime() {
        return youngGcCount > 0 ? (youngGcTime * 1.0 / youngGcCount) : 0.0;
    }

    public double getAverageOldGcTime() {
        return oldGcCount > 0 ? (oldGcTime * 1.0 / oldGcCount) : 0.0;
    }

    @Override
    public String toString() {
        return String.format(
                "JVMMetrics{timestamp=%s, heapUsed=%dMB, heapMax=%dMB, usage=%.2f%%, " +
                        "gcCount=%d, gcTime=%dms, threads=%d}",
                timestamp,
                heapUsed / (1024 * 1024),
                heapMax / (1024 * 1024),
                getHeapUsagePercentage(),
                getTotalGcCount(),
                getTotalGcTime(),
                threadCount
        );
    }
}