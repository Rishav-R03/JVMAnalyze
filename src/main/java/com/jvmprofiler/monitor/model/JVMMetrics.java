package com.jvmprofiler.monitor.model;

import java.lang.management.MemoryUsage;

public class JVMMetrics {
    private MemoryUsage heapMemory;
    private MemoryUsage nonHeapMemory;
    private long gcCount;
    private long gcTime;
    private int threadCount;
    private int peakThreadCount;
    private long totalStartedThreads;
    private long timestamp;

    // Constructors
    public JVMMetrics() {
        // No need to initialize MemoryUsage objects - they'll be set from JMX
    }

    // Getters and Setters
    public MemoryUsage getHeapMemory() { return heapMemory; }
    public void setHeapMemory(MemoryUsage heapMemory) { this.heapMemory = heapMemory; }

    public MemoryUsage getNonHeapMemory() { return nonHeapMemory; }
    public void setNonHeapMemory(MemoryUsage nonHeapMemory) { this.nonHeapMemory = nonHeapMemory; }

    public long getGcCount() { return gcCount; }
    public void setGcCount(long gcCount) { this.gcCount = gcCount; }

    public long getGcTime() { return gcTime; }
    public void setGcTime(long gcTime) { this.gcTime = gcTime; }

    public int getThreadCount() { return threadCount; }
    public void setThreadCount(int threadCount) { this.threadCount = threadCount; }

    public int getPeakThreadCount() { return peakThreadCount; }
    public void setPeakThreadCount(int peakThreadCount) { this.peakThreadCount = peakThreadCount; }

    public long getTotalStartedThreads() { return totalStartedThreads; }
    public void setTotalStartedThreads(long totalStartedThreads) { this.totalStartedThreads = totalStartedThreads; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}