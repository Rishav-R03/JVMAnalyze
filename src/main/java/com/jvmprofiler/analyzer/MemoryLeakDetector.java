package com.jvmprofiler.analyzer;

import com.jvmprofiler.analyzer.model.GCEvent;
import com.jvmprofiler.analyzer.model.GCLog;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MemoryLeakDetector {
    private static final Logger logger = LogManager.getLogger(MemoryLeakDetector.class);

    // Configuration
    private double leakConfidenceThreshold = 0.7; // 70% confidence for leak detection
    private int minEventsForAnalysis = 10;        // Minimum events needed for analysis

    public MemoryLeakDetector() {}

    public MemoryLeakDetector(double leakConfidenceThreshold, int minEventsForAnalysis) {
        this.leakConfidenceThreshold = leakConfidenceThreshold;
        this.minEventsForAnalysis = minEventsForAnalysis;
    }

    public static class LeakAnalysisResult {
        private boolean leakDetected;
        private double confidence;
        private String patternType; // "LINEAR", "EXPONENTIAL", "STEPPING"
        private double growthRate;  // Bytes per minute
        private List<GCEvent> suspiciousEvents;
        private String description;

        // Getters and Setters
        public boolean isLeakDetected() { return leakDetected; }
        public void setLeakDetected(boolean leakDetected) { this.leakDetected = leakDetected; }

        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }

        public String getPatternType() { return patternType; }
        public void setPatternType(String patternType) { this.patternType = patternType; }

        public double getGrowthRate() { return growthRate; }
        public void setGrowthRate(double growthRate) { this.growthRate = growthRate; }

        public List<GCEvent> getSuspiciousEvents() { return suspiciousEvents; }
        public void setSuspiciousEvents(List<GCEvent> suspiciousEvents) { this.suspiciousEvents = suspiciousEvents; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        @Override
        public String toString() {
            if (!leakDetected) {
                return "No memory leak detected (confidence: " + String.format("%.1f%%", confidence * 100) + ")";
            }
            return String.format(
                    "Memory leak detected! Type: %s, Confidence: %.1f%%, Growth: %.2f MB/min",
                    patternType, confidence * 100, growthRate / (1024 * 1024)
            );
        }
    }

    /**
     * Main method to detect memory leaks from GC log data
     */
    public LeakAnalysisResult detectMemoryLeak(GCLog gcLog) {
        logger.info("Starting memory leak detection analysis");

        LeakAnalysisResult result = new LeakAnalysisResult();
        result.setSuspiciousEvents(new ArrayList<>());

        List<GCEvent> events = gcLog.getEvents();

        if (events.size() < minEventsForAnalysis) {
            result.setLeakDetected(false);
            result.setConfidence(0.0);
            result.setDescription("Insufficient data for leak detection (need at least " + minEventsForAnalysis + " events)");
            return result;
        }

        // Multiple detection strategies
        double linearConfidence = analyzeLinearGrowth(events, result);
        double exponentialConfidence = analyzeExponentialGrowth(events, result);
        double steppingConfidence = analyzeSteppingPattern(events, result);
        double efficiencyConfidence = analyzeMemoryEfficiencyTrend(events, result);

        // Combine confidence scores
        double overallConfidence = Math.max(
                Math.max(linearConfidence, exponentialConfidence),
                Math.max(steppingConfidence, efficiencyConfidence)
        );

        result.setConfidence(overallConfidence);
        result.setLeakDetected(overallConfidence >= leakConfidenceThreshold);

        if (result.isLeakDetected()) {
            result.setDescription(buildLeakDescription(result));
            logger.warn("Memory leak detected with {}% confidence", String.format("%.1f", overallConfidence * 100));
        } else {
            result.setDescription("No strong evidence of memory leak detected");
            logger.info("No memory leak detected (confidence: {}%)", String.format("%.1f", overallConfidence * 100));
        }

        return result;
    }

    /**
     * Detect linear growth pattern in heap usage after GC
     */
    private double analyzeLinearGrowth(List<GCEvent> events, LeakAnalysisResult result) {
        List<GCEvent> filteredEvents = filterMajorGcEvents(events);
        if (filteredEvents.size() < 5) return 0.0;

        // Simple linear regression on heap after GC
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        int n = filteredEvents.size();

        for (int i = 0; i < n; i++) {
            double x = (filteredEvents.get(i).getTimestamp() - filteredEvents.get(0).getTimestamp()) / (1000.0 * 60.0); // minutes
            double y = filteredEvents.get(i).getHeapAfter(); // bytes
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }

        double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        double intercept = (sumY - slope * sumX) / n;

        // Calculate R-squared (goodness of fit)
        double ssTot = 0, ssRes = 0;
        double meanY = sumY / n;

        for (int i = 0; i < n; i++) {
            double x = (filteredEvents.get(i).getTimestamp() - filteredEvents.get(0).getTimestamp()) / (1000.0 * 60.0);
            double y = filteredEvents.get(i).getHeapAfter();
            double predicted = slope * x + intercept;
            ssTot += Math.pow(y - meanY, 2);
            ssRes += Math.pow(y - predicted, 2);
        }

        double rSquared = 1 - (ssRes / ssTot);

        // Confidence based on R-squared and positive slope
        double confidence = 0.0;
        if (slope > 0 && rSquared > 0.6) {
            confidence = Math.min(rSquared, 0.9); // Cap at 90% for linear
            result.setGrowthRate(slope); // bytes per minute
            result.setPatternType("LINEAR");

            // Mark events that contribute to the trend
            if (confidence > 0.7) {
                result.getSuspiciousEvents().addAll(filteredEvents.subList(n / 2, n)); // Later events more suspicious
            }
        }

        logger.debug("Linear growth analysis: slope={}, r²={}, confidence={}",
                String.format("%.2f", slope), String.format("%.3f", rSquared), String.format("%.3f", confidence));

        return confidence;
    }

    /**
     * Detect exponential growth pattern (more severe leaks)
     */
    private double analyzeExponentialGrowth(List<GCEvent> events, LeakAnalysisResult result) {
        List<GCEvent> filteredEvents = filterMajorGcEvents(events);
        if (filteredEvents.size() < 8) return 0.0;

        // Check if growth rate is accelerating
        List<Double> growthRates = new ArrayList<>();
        for (int i = 1; i < filteredEvents.size(); i++) {
            long timeDiff = filteredEvents.get(i).getTimestamp() - filteredEvents.get(i-1).getTimestamp();
            long heapDiff = filteredEvents.get(i).getHeapAfter() - filteredEvents.get(i-1).getHeapAfter();
            if (timeDiff > 0) {
                double rate = (double) heapDiff / (timeDiff / 60000.0); // bytes per minute
                growthRates.add(rate);
            }
        }

        // Check if growth rates are increasing
        boolean accelerating = isAccelerating(growthRates);
        double confidence = 0.0;

        if (accelerating && growthRates.stream().anyMatch(rate -> rate > 0)) {
            confidence = 0.8; // High confidence for exponential pattern
            if (result.getPatternType() == null || result.getConfidence() < confidence) {
                result.setPatternType("EXPONENTIAL");
                result.setGrowthRate(growthRates.get(growthRates.size() - 1)); // Latest rate
            }
        }

        return confidence;
    }

    private boolean isAccelerating(List<Double> rates) {
        if (rates.size() < 3) return false;

        // Simple test: check if more than half of consecutive pairs show acceleration
        int acceleratingPairs = 0;
        for (int i = 1; i < rates.size(); i++) {
            if (rates.get(i) > rates.get(i-1)) {
                acceleratingPairs++;
            }
        }

        return acceleratingPairs > rates.size() / 2;
    }

    /**
     * Detect stepping pattern (memory grows in steps, common with caches)
     */
    private double analyzeSteppingPattern(List<GCEvent> events, LeakAnalysisResult result) {
        List<GCEvent> filteredEvents = filterMajorGcEvents(events);
        if (filteredEvents.size() < 6) return 0.0;

        // Look for plateaus followed by sudden jumps
        int stepsDetected = 0;
        List<GCEvent> stepEvents = new ArrayList<>();

        for (int i = 1; i < filteredEvents.size() - 1; i++) {
            GCEvent prev = filteredEvents.get(i-1);
            GCEvent curr = filteredEvents.get(i);
            GCEvent next = filteredEvents.get(i+1);

            // Check for plateau (similar heap values) followed by jump
            boolean plateau = Math.abs(curr.getHeapAfter() - prev.getHeapAfter()) < (prev.getHeapAfter() * 0.05); // Within 5%
            boolean jump = next.getHeapAfter() > curr.getHeapAfter() * 1.1; // Jump more than 10%

            if (plateau && jump) {
                stepsDetected++;
                stepEvents.add(next);
            }
        }

        double confidence = 0.0;
        if (stepsDetected >= 2) {
            confidence = Math.min(stepsDetected * 0.3, 0.8); // 30% per step, max 80%
            if (result.getPatternType() == null || result.getConfidence() < confidence) {
                result.setPatternType("STEPPING");
                result.getSuspiciousEvents().addAll(stepEvents);

                // Calculate average growth rate
                double totalGrowth = filteredEvents.get(filteredEvents.size()-1).getHeapAfter() -
                        filteredEvents.get(0).getHeapAfter();
                double totalTime = (filteredEvents.get(filteredEvents.size()-1).getTimestamp() -
                        filteredEvents.get(0).getTimestamp()) / (1000.0 * 60.0);
                result.setGrowthRate(totalGrowth / totalTime);
            }
        }

        return confidence;
    }

    /**
     * Analyze trend in memory efficiency (should be stable or improving)
     */
    private double analyzeMemoryEfficiencyTrend(List<GCEvent> events, LeakAnalysisResult result) {
        if (events.size() < 10) return 0.0;

        // Calculate efficiency for each GC
        List<Double> efficiencies = new ArrayList<>();
        for (GCEvent event : events) {
            if (event.getHeapBefore() > 0) {
                double efficiency = (double) event.getHeapFreed() / event.getHeapBefore();
                efficiencies.add(efficiency);
            }
        }

        // Check if efficiency is decreasing over time (sign of fragmentation or leak)
        double earlyEfficiency = efficiencies.subList(0, efficiencies.size()/2).stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0);

        double lateEfficiency = efficiencies.subList(efficiencies.size()/2, efficiencies.size()).stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0);

        double efficiencyDrop = earlyEfficiency - lateEfficiency;
        double confidence = 0.0;

        if (efficiencyDrop > 0.2) { // 20% drop in efficiency
            confidence = 0.7;
            if (result.getPatternType() == null) {
                result.setPatternType("EFFICIENCY_DECLINE");
            }

            // Find least efficient events
            events.stream()
                    .filter(event -> {
                        if (event.getHeapBefore() == 0) return false;
                        double eff = (double) event.getHeapFreed() / event.getHeapBefore();
                        return eff < lateEfficiency;
                    })
                    .forEach(result.getSuspiciousEvents()::add);
        }

        return confidence;
    }

    private List<GCEvent> filterMajorGcEvents(List<GCEvent> events) {
        return events.stream()
                .filter(GCEvent::isMajorGc)
                .filter(event -> event.getHeapAfter() > 0)
                .toList();
    }

    private String buildLeakDescription(LeakAnalysisResult result) {
        String pattern = result.getPatternType();
        double growthRateMB = result.getGrowthRate() / (1024 * 1024);
        double confidencePercent = result.getConfidence() * 100;

        switch (pattern) {
            case "LINEAR":
                return String.format(
                        "Linear memory leak detected (%.1f%% confidence). Heap growing at %.2f MB/minute. " +
                                "This suggests consistent object accumulation.",
                        confidencePercent, growthRateMB
                );
            case "EXPONENTIAL":
                return String.format(
                        "Exponential memory leak detected (%.1f%% confidence). Growth rate accelerating. " +
                                "This suggests unbounded data structure growth.",
                        confidencePercent
                );
            case "STEPPING":
                return String.format(
                        "Stepping memory leak detected (%.1f%% confidence). Memory grows in distinct steps. " +
                                "This suggests cache-like behavior or periodic allocations.",
                        confidencePercent
                );
            case "EFFICIENCY_DECLINE":
                return String.format(
                        "Memory efficiency declining (%.1f%% confidence). GC becoming less effective over time. " +
                                "This suggests fragmentation or changing allocation patterns.",
                        confidencePercent
                );
            default:
                return String.format(
                        "Memory leak detected (%.1f%% confidence). Pattern: %s",
                        confidencePercent, pattern
                );
        }
    }

    /**
     * Quick analysis for real-time monitoring integration
     */
    public boolean quickLeakCheck(List<GCEvent> recentEvents) {
        if (recentEvents.size() < 5) return false;

        List<GCEvent> majorEvents = filterMajorGcEvents(recentEvents);
        if (majorEvents.size() < 3) return false;

        // Simple check: if last 3 major GCs show increasing heap after GC
        boolean increasing = true;
        for (int i = 1; i < majorEvents.size(); i++) {
            if (majorEvents.get(i).getHeapAfter() <= majorEvents.get(i-1).getHeapAfter()) {
                increasing = false;
                break;
            }
        }

        return increasing;
    }
}