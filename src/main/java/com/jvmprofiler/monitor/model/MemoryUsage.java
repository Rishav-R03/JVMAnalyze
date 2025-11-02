package com.jvmprofiler.monitor.model;

public class MemoryUsage {
    private long used;
    private long committed;
    private long max;

    // Getters and Setters
    public long getUsed() { return used; }
    public void setUsed(long used) { this.used = used; }

    public long getCommitted() { return committed; }
    public void setCommitted(long committed) { this.committed = committed; }

    public long getMax() { return max; }
    public void setMax(long max) { this.max = max; }
}