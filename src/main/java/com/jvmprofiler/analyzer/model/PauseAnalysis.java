package com.jvmprofiler.analyzer.model;

import java.util.ArrayList;
import java.util.List;

public class PauseAnalysis {
    private GCLog gcLog;

    // Basic statistics
    private long totalEvents;
    private long majorGcCount;
    private long minorGcCount;
    private long totalGcTime;
    private long longestPause;
    private double averagePause;
    private double gcTimePercentage;
    private double averageMemoryEfficiency;

    // Percentile data
    private long p50;
    private long p90;
    private long p95;
    private long p99;

    // Issue detection
    private List<GCEvent> longPauses = new ArrayList<>();
    private List<GCEvent> criticalPauses = new ArrayList<>();
    private List<String> issues = new ArrayList<>(); // Format: "CODE:Description:SEVERITY"

    // Efficiency analysis
    private GCEvent mostEfficientGc;
    private GCEvent leastEfficientGc;

    // Recommendations
    private List<String> recommendations = new ArrayList<>();

    // Getters and Setters
    public GCLog getGcLog() { return gcLog; }
    public void setGcLog(GCLog gcLog) { this.gcLog = gcLog; }

    public long getTotalEvents() { return totalEvents; }
    public void setTotalEvents(long totalEvents) { this.totalEvents = totalEvents; }

    public long getMajorGcCount() { return majorGcCount; }
    public void setMajorGcCount(long majorGcCount) { this.majorGcCount = majorGcCount; }

    public long getMinorGcCount() { return minorGcCount; }
    public void setMinorGcCount(long minorGcCount) { this.minorGcCount = minorGcCount; }

    public long getTotalGcTime() { return totalGcTime; }
    public void setTotalGcTime(long totalGcTime) { this.totalGcTime = totalGcTime; }

    public long getLongestPause() { return longestPause; }
    public void setLongestPause(long longestPause) { this.longestPause = longestPause; }

    public double getAveragePause() { return averagePause; }
    public void setAveragePause(double averagePause) { this.averagePause = averagePause; }

    public double getGcTimePercentage() { return gcTimePercentage; }
    public void setGcTimePercentage(double gcTimePercentage) { this.gcTimePercentage = gcTimePercentage; }

    public double getAverageMemoryEfficiency() { return averageMemoryEfficiency; }
    public void setAverageMemoryEfficiency(double averageMemoryEfficiency) { this.averageMemoryEfficiency = averageMemoryEfficiency; }

    public long getP50() { return p50; }
    public void setP50(long p50) { this.p50 = p50; }

    public long getP90() { return p90; }
    public void setP90(long p90) { this.p90 = p90; }

    public long getP95() { return p95; }
    public void setP95(long p95) { this.p95 = p95; }

    public long getP99() { return p99; }
    public void setP99(long p99) { this.p99 = p99; }

    public List<GCEvent> getLongPauses() { return longPauses; }
    public void setLongPauses(List<GCEvent> longPauses) { this.longPauses = longPauses; }

    public List<GCEvent> getCriticalPauses() { return criticalPauses; }
    public void setCriticalPauses(List<GCEvent> criticalPauses) { this.criticalPauses = criticalPauses; }

    public List<String> getIssues() { return issues; }
    public void setIssues(List<String> issues) { this.issues = issues; }

    public GCEvent getMostEfficientGc() { return mostEfficientGc; }
    public void setMostEfficientGc(GCEvent mostEfficientGc) { this.mostEfficientGc = mostEfficientGc; }

    public GCEvent getLeastEfficientGc() { return leastEfficientGc; }
    public void setLeastEfficientGc(GCEvent leastEfficientGc) { this.leastEfficientGc = leastEfficientGc; }

    public List<String> getRecommendations() { return recommendations; }
    public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }

    // Utility methods
    public void addIssue(String code, String description, String severity) {
        issues.add(code + ":" + description + ":" + severity);
    }

    public int getIssueCount() {
        return issues.size();
    }

    public int getCriticalIssueCount() {
        return (int) issues.stream()
                .filter(issue -> issue.endsWith(":CRITICAL"))
                .count();
    }

    public int getWarningIssueCount() {
        return (int) issues.stream()
                .filter(issue -> issue.endsWith(":WARNING"))
                .count();
    }
}