package com.jvmprofiler.analyzer.model;

import java.util.Date;

public class GCEvent {
    private String gcType;          // "Young GC", "Full GC", etc.
    private String gcCause;         // "Allocation Failure", "System.gc()", etc.
    private long timestamp;         // Event timestamp
    private long duration;          // Pause time in milliseconds
    private long heapBefore;        // Heap size before GC (bytes)
    private long heapAfter;         // Heap size after GC (bytes)
    private long heapCommitted;     // Committed heap size
    private long youngBefore;       // Young gen before GC
    private long youngAfter;        // Young gen after GC
    private long oldBefore;         // Old gen before GC
    private long oldAfter;          // Old gen after GC
    private boolean majorGc;        // Is this a major GC?
    private boolean systemGc;       // Was it triggered by System.gc()?

    // Constructors
    public GCEvent() {}

    public GCEvent(String gcType, long timestamp, long duration) {
        this.gcType = gcType;
        this.timestamp = timestamp;
        this.duration = duration;
    }

    // Getters and Setters
    public String getGcType() { return gcType; }
    public void setGcType(String gcType) { this.gcType = gcType; }

    public String getGcCause() { return gcCause; }
    public void setGcCause(String gcCause) { this.gcCause = gcCause; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public long getDuration() { return duration; }
    public void setDuration(long duration) { this.duration = duration; }

    public long getHeapBefore() { return heapBefore; }
    public void setHeapBefore(long heapBefore) { this.heapBefore = heapBefore; }

    public long getHeapAfter() { return heapAfter; }
    public void setHeapAfter(long heapAfter) { this.heapAfter = heapAfter; }

    public long getHeapCommitted() { return heapCommitted; }
    public void setHeapCommitted(long heapCommitted) { this.heapCommitted = heapCommitted; }

    public long getYoungBefore() { return youngBefore; }
    public void setYoungBefore(long youngBefore) { this.youngBefore = youngBefore; }

    public long getYoungAfter() { return youngAfter; }
    public void setYoungAfter(long youngAfter) { this.youngAfter = youngAfter; }

    public long getOldBefore() { return oldBefore; }
    public void setOldBefore(long oldBefore) { this.oldBefore = oldBefore; }

    public long getOldAfter() { return oldAfter; }
    public void setOldAfter(long oldAfter) { this.oldAfter = oldAfter; }

    public boolean isMajorGc() { return majorGc; }
    public void setMajorGc(boolean majorGc) { this.majorGc = majorGc; }

    public boolean isSystemGc() { return systemGc; }
    public void setSystemGc(boolean systemGc) { this.systemGc = systemGc; }

    // Utility methods
    public long getHeapFreed() {
        return heapBefore - heapAfter;
    }

    public double getDurationSeconds() {
        return duration / 1000.0;
    }

    public Date getEventDate() {
        return new Date(timestamp);
    }

    @Override
    public String toString() {
        return String.format("GCEvent{type=%s, duration=%.3fs, heap=%,d->%,d}",
                gcType, getDurationSeconds(), heapBefore, heapAfter);
    }
}