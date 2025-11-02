package com.jvmprofiler.analyzer;

import com.jvmprofiler.analyzer.model.GCEvent;
import com.jvmprofiler.analyzer.model.GCLog;
import com.jvmprofiler.analyzer.model.PauseAnalysis;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

public class GCLogAnalyzer {
    private static final Logger logger = LogManager.getLogger(GCLogAnalyzer.class);

    // Configuration thresholds (configurable)
    private long longPauseThresholdMs = 100;    // Pauses longer than 100ms are "long"
    private long criticalPauseThresholdMs = 1000; // Pauses longer than 1s are critical
    private double gcTimePercentageThreshold = 10.0; // GC time > 10% of total time is bad
    private double memoryEfficiencyThreshold = 50.0; // Heap freed < 50% is inefficient

    public GCLogAnalyzer() {}

    public GCLogAnalyzer(long longPauseThresholdMs, long criticalPauseThresholdMs,
                         double gcTimePercentageThreshold, double memoryEfficiencyThreshold) {
        this.longPauseThresholdMs = longPauseThresholdMs;
        this.criticalPauseThresholdMs = criticalPauseThresholdMs;
        this.gcTimePercentageThreshold = gcTimePercentageThreshold;
        this.memoryEfficiencyThreshold = memoryEfficiencyThreshold;
    }

    /**
     * Comprehensive analysis of GC log data
     */
    public PauseAnalysis analyze(GCLog gcLog) {
        logger.info("Starting GC log analysis for {} events", gcLog.getEvents().size());

        PauseAnalysis analysis = new PauseAnalysis();
        analysis.setGcLog(gcLog);

        // Basic statistics
        calculateBasicStatistics(gcLog, analysis);

        // Performance issues detection
        detectLongPauses(gcLog, analysis);
        detectFrequentGc(gcLog, analysis);
        detectMemoryIssues(gcLog, analysis);
        detectSystemGcIssues(gcLog, analysis);

        // Generate recommendations
        generateRecommendations(analysis);

        logger.info("GC analysis completed. Found {} issues.", analysis.getIssues().size());

        return analysis;
    }

    private void calculateBasicStatistics(GCLog gcLog, PauseAnalysis analysis) {
        List<GCEvent> events = gcLog.getEvents();

        if (events.isEmpty()) {
            analysis.addIssue("NO_EVENTS", "No GC events found in log", "WARNING");
            return;
        }

        // Basic counts
        long totalEvents = events.size();
        long majorGcEvents = events.stream().filter(GCEvent::isMajorGc).count();
        long minorGcEvents = totalEvents - majorGcEvents;

        analysis.setTotalEvents(totalEvents);
        analysis.setMajorGcCount(majorGcEvents);
        analysis.setMinorGcCount(minorGcEvents);

        // Duration statistics
        List<Long> durations = events.stream()
                .map(GCEvent::getDuration)
                .sorted()
                .toList();

        analysis.setTotalGcTime(gcLog.getTotalGcTime());
        analysis.setLongestPause(gcLog.getLongestPause());
        analysis.setAveragePause(gcLog.getAveragePause());

        // Percentiles
        analysis.setP50(calculatePercentile(durations, 50));
        analysis.setP90(calculatePercentile(durations, 90));
        analysis.setP95(calculatePercentile(durations, 95));
        analysis.setP99(calculatePercentile(durations, 99));

        // GC time percentage
        analysis.setGcTimePercentage(gcLog.getGcTimePercentage());

        // Memory efficiency
        calculateMemoryEfficiency(gcLog, analysis);
    }

    private long calculatePercentile(List<Long> values, double percentile) {
        if (values.isEmpty()) return 0;

        int index = (int) Math.ceil(percentile / 100.0 * values.size()) - 1;
        index = Math.max(0, Math.min(index, values.size() - 1));
        return values.get(index);
    }

    private void calculateMemoryEfficiency(GCLog gcLog, PauseAnalysis analysis) {
        List<GCEvent> events = gcLog.getEvents();

        double totalEfficiency = events.stream()
                .mapToDouble(event -> {
                    if (event.getHeapBefore() == 0) return 0;
                    return (double) event.getHeapFreed() / event.getHeapBefore() * 100;
                })
                .average()
                .orElse(0);

        analysis.setAverageMemoryEfficiency(totalEfficiency);

        // Find most and least efficient GCs
        events.stream()
                .max(Comparator.comparingDouble(event ->
                        event.getHeapBefore() > 0 ?
                                (double) event.getHeapFreed() / event.getHeapBefore() : 0))
                .ifPresent(analysis::setMostEfficientGc);

        events.stream()
                .min(Comparator.comparingDouble(event ->
                        event.getHeapBefore() > 0 ?
                                (double) event.getHeapFreed() / event.getHeapBefore() : 0))
                .ifPresent(analysis::setLeastEfficientGc);
    }

    private void detectLongPauses(GCLog gcLog, PauseAnalysis analysis) {
        List<GCEvent> longPauses = gcLog.getEvents().stream()
                .filter(event -> event.getDuration() > longPauseThresholdMs)
                .toList();

        List<GCEvent> criticalPauses = gcLog.getEvents().stream()
                .filter(event -> event.getDuration() > criticalPauseThresholdMs)
                .toList();

        analysis.setLongPauses(longPauses);
        analysis.setCriticalPauses(criticalPauses);

        if (!criticalPauses.isEmpty()) {
            analysis.addIssue("CRITICAL_PAUSES",
                    String.format("Found %d critical pauses (>%dms)", criticalPauses.size(), criticalPauseThresholdMs),
                    "CRITICAL");
        }

        if (!longPauses.isEmpty()) {
            analysis.addIssue("LONG_PAUSES",
                    String.format("Found %d long pauses (>%dms)", longPauses.size(), longPauseThresholdMs),
                    "WARNING");
        }

        // Check if average pause is too high
        if (analysis.getAveragePause() > longPauseThresholdMs) {
            analysis.addIssue("HIGH_AVERAGE_PAUSE",
                    String.format("Average pause time %.2fms is high", analysis.getAveragePause()),
                    "WARNING");
        }
    }

    private void detectFrequentGc(GCLog gcLog, PauseAnalysis analysis) {
        double gcTimePercentage = analysis.getGcTimePercentage();

        if (gcTimePercentage > gcTimePercentageThreshold) {
            analysis.addIssue("HIGH_GC_TIME",
                    String.format("GC time is %.1f%% of total time (threshold: %.1f%%)",
                            gcTimePercentage, gcTimePercentageThreshold),
                    "WARNING");
        }

        // Check for GC storms (many GCs in short period)
        detectGcStorms(gcLog, analysis);
    }

    private void detectGcStorms(GCLog gcLog, PauseAnalysis analysis) {
        List<GCEvent> events = gcLog.getEvents();
        if (events.size() < 10) return; // Need enough events

        // Group events by time windows and look for high frequency
        Map<Long, Integer> eventsPerMinute = new HashMap<>();

        for (GCEvent event : events) {
            long minute = event.getTimestamp() / (60 * 1000); // Group by minute
            eventsPerMinute.put(minute, eventsPerMinute.getOrDefault(minute, 0) + 1);
        }

        Optional<Map.Entry<Long, Integer>> maxEntry = eventsPerMinute.entrySet().stream()
                .max(Map.Entry.comparingByValue());

        if (maxEntry.isPresent() && maxEntry.get().getValue() > 10) { // More than 10 GCs per minute
            analysis.addIssue("GC_STORM",
                    String.format("Detected GC storm: %d GCs in one minute", maxEntry.get().getValue()),
                    "WARNING");
        }
    }

    private void detectMemoryIssues(GCLog gcLog, PauseAnalysis analysis) {
        // Check memory efficiency
        if (analysis.getAverageMemoryEfficiency() < memoryEfficiencyThreshold) {
            analysis.addIssue("LOW_MEMORY_EFFICIENCY",
                    String.format("Low memory efficiency: %.1f%% (threshold: %.1f%%)",
                            analysis.getAverageMemoryEfficiency(), memoryEfficiencyThreshold),
                    "WARNING");
        }

        // Check for memory leaks by analyzing heap trends
        if (gcLog.getEvents().size() > 20) { // Need enough data
            analyzeHeapTrend(gcLog, analysis);
        }

        // Check for too many major GCs
        double majorGcRatio = (double) analysis.getMajorGcCount() / analysis.getTotalEvents();
        if (majorGcRatio > 0.1) { // More than 10% major GCs
            analysis.addIssue("HIGH_MAJOR_GC_RATIO",
                    String.format("High ratio of major GCs: %.1f%%", majorGcRatio * 100),
                    "WARNING");
        }
    }

    private void analyzeHeapTrend(GCLog gcLog, PauseAnalysis analysis) {
        List<GCEvent> events = gcLog.getEvents();

        // Use the last 20 events for trend analysis
        int sampleSize = Math.min(20, events.size());
        List<GCEvent> recentEvents = events.subList(events.size() - sampleSize, events.size());

        // Calculate average heap usage after GC (should be stable if no leak)
        double avgHeapAfter = recentEvents.stream()
                .mapToLong(GCEvent::getHeapAfter)
                .average()
                .orElse(0);

        // Check if heap after GC is consistently growing
        boolean growingTrend = isHeapGrowing(recentEvents);

        if (growingTrend) {
            analysis.addIssue("POSSIBLE_MEMORY_LEAK",
                    "Detected growing heap trend after GCs - possible memory leak",
                    "CRITICAL");
        }
    }

    private boolean isHeapGrowing(List<GCEvent> events) {
        if (events.size() < 5) return false;

        // Simple linear regression to detect trend
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        int n = events.size();

        for (int i = 0; i < n; i++) {
            double x = i;
            double y = events.get(i).getHeapAfter();
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }

        double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);

        // If slope is positive and significant, heap is growing
        return slope > (1024 * 1024); // Growing more than 1MB per GC on average
    }

    private void detectSystemGcIssues(GCLog gcLog, PauseAnalysis analysis) {
        long systemGcCount = gcLog.getEvents().stream()
                .filter(GCEvent::isSystemGc)
                .count();

        if (systemGcCount > 0) {
            analysis.addIssue("SYSTEM_GC_CALLS",
                    String.format("Found %d System.gc() calls - can cause unnecessary pauses", systemGcCount),
                    "WARNING");
        }
    }

    private void generateRecommendations(PauseAnalysis analysis) {
        List<String> recommendations = new ArrayList<>();

        // Generate recommendations based on detected issues
        if (analysis.getCriticalPauses().size() > 0) {
            recommendations.add("Consider tuning GC parameters to reduce pause times");
            recommendations.add("Evaluate switching to low-pause GC (ZGC, Shenandoah) for critical applications");
        }

        if (analysis.getGcTimePercentage() > gcTimePercentageThreshold) {
            recommendations.add("Increase heap size to reduce GC frequency");
            recommendations.add("Optimize object allocation patterns");
        }

        if (analysis.getAverageMemoryEfficiency() < memoryEfficiencyThreshold) {
            recommendations.add("Review object retention and memory usage patterns");
            recommendations.add("Consider adjusting generation sizes");
        }

        if (analysis.getIssues().stream().anyMatch(issue -> issue.contains("MEMORY_LEAK"))) {
            recommendations.add("Perform memory profiling to identify leaking objects");
            recommendations.add("Review object lifecycle management");
        }

        if (analysis.getMajorGcCount() > analysis.getTotalEvents() * 0.1) {
            recommendations.add("Increase young generation size to reduce promotion rate");
            recommendations.add("Tune -XX:MaxTenuringThreshold if appropriate");
        }

        // GC-specific recommendations
        String gcType = analysis.getGcLog().getGcType();
        if ("G1GC".equals(gcType)) {
            recommendations.add("Consider tuning G1GC: -XX:MaxGCPauseMillis, -XX:G1HeapRegionSize");
        } else if ("ZGC".equals(gcType)) {
            recommendations.add("ZGC is well-tuned by default, but ensure adequate memory for best performance");
        } else if ("ParallelGC".equals(gcType)) {
            recommendations.add("For better pause times, consider switching to G1GC or ZGC");
        }

        analysis.setRecommendations(recommendations);
    }

    // Utility methods for specific analyses
    public List<GCEvent> findEventsByType(GCLog gcLog, String type) {
        return gcLog.getEvents().stream()
                .filter(event -> event.getGcType().toLowerCase().contains(type.toLowerCase()))
                .toList();
    }

    public Map<String, Long> getGcTypeDistribution(GCLog gcLog) {
        return gcLog.getEvents().stream()
                .collect(Collectors.groupingBy(
                        GCEvent::getGcType,
                        Collectors.counting()
                ));
    }

    public double getThroughput(GCLog gcLog) {
        long totalTime = gcLog.getEndTime() - gcLog.getStartTime();
        if (totalTime == 0) return 100.0;

        double gcTime = gcLog.getTotalGcTime();
        return 100.0 - (gcTime / totalTime * 100);
    }
}