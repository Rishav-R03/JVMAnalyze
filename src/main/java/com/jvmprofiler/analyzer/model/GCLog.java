package com.jvmprofiler.analyzer.model;

import java.util.ArrayList;
import java.util.List;

public class GCLog {
    private List<GCEvent> events = new ArrayList<>();
    private String gcType;          // "G1GC", "ZGC", "ParallelGC"
    private String jvmVersion;
    private long startTime;
    private long endTime;
    private String logFile;

    // Statistics
    private long totalGcEvents;
    private long totalGcTime;
    private long longestPause;
    private double averagePause;
    private long totalHeapFreed;

    public GCLog() {}

    // Getters and Setters
    public List<GCEvent> getEvents() { return events; }
    public void setEvents(List<GCEvent> events) { this.events = events; }

    public String getGcType() { return gcType; }
    public void setGcType(String gcType) { this.gcType = gcType; }

    public String getJvmVersion() { return jvmVersion; }
    public void setJvmVersion(String jvmVersion) { this.jvmVersion = jvmVersion; }

    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }

    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }

    public String getLogFile() { return logFile; }
    public void setLogFile(String logFile) { this.logFile = logFile; }

    // Statistics calculation methods
    public void calculateStatistics() {
        if (events.isEmpty()) return;

        totalGcEvents = events.size();
        totalGcTime = events.stream().mapToLong(GCEvent::getDuration).sum();
        longestPause = events.stream().mapToLong(GCEvent::getDuration).max().orElse(0);
        averagePause = events.stream().mapToLong(GCEvent::getDuration).average().orElse(0);
        totalHeapFreed = events.stream().mapToLong(GCEvent::getHeapFreed).sum();

        // Set start and end times from events
        startTime = events.stream().mapToLong(GCEvent::getTimestamp).min().orElse(0);
        endTime = events.stream().mapToLong(GCEvent::getTimestamp).max().orElse(0);
    }

    public long getTotalGcEvents() { return totalGcEvents; }
    public long getTotalGcTime() { return totalGcTime; }
    public long getLongestPause() { return longestPause; }
    public double getAveragePause() { return averagePause; }
    public long getTotalHeapFreed() { return totalHeapFreed; }

    // Utility methods
    public void addEvent(GCEvent event) {
        events.add(event);
    }

    public List<GCEvent> getMajorGcEvents() {
        return events.stream().filter(GCEvent::isMajorGc).toList();
    }

    public List<GCEvent> getLongPauses(long thresholdMs) {
        return events.stream()
                .filter(event -> event.getDuration() > thresholdMs)
                .toList();
    }

    public double getGcTimePercentage() {
        if (endTime <= startTime) return 0;
        long totalTime = endTime - startTime;
        return (double) totalGcTime / totalTime * 100;
    }
}