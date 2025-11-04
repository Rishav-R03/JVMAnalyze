package com.jvmprofiler.cli;

import com.jvmprofiler.analyzer.GCLogAnalyzer;
import com.jvmprofiler.analyzer.GCLogParser;
import com.jvmprofiler.analyzer.MemoryLeakDetector;
import com.jvmprofiler.analyzer.model.GCLog;
import com.jvmprofiler.analyzer.model.PauseAnalysis;
import com.jvmprofiler.monitor.RealTimeDashboard;
import org.apache.commons.cli.CommandLine;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.jvmprofiler.monitor.prometheus.PrometheusMetricsExporter;

import java.io.IOException;

public class CLIHandler {
    private static final Logger logger = LogManager.getLogger(CLIHandler.class);
    private final CommandLineParser parser;

    public CLIHandler() {
        this.parser = new CommandLineParser();
    }

    public void handleCommand(String[] args) {
        try {
            CommandLine cmd = parser.parse(args);

            if (cmd.hasOption("help")) {
                parser.printHelp();
                return;
            }

            if (cmd.hasOption("monitor")) {
                handleMonitorCommand(cmd);
            } else if (cmd.hasOption("analyze-gc")) {
                handleAnalyzeGcCommand(cmd);
            } else if (cmd.hasOption("detect-leaks")) {
                handleDetectLeaksCommand(cmd);
            } else {
                System.out.println("Unknown command. Use --help for usage information.");
            }

        } catch (Exception e) {
            logger.error("Command handling error: {}", e.getMessage(), e);
            System.err.println("Error: " + e.getMessage());
        }
    }

    private void handleMonitorCommand(CommandLine cmd) {
        String pidStr = cmd.getOptionValue("monitor");
        int interval = Integer.parseInt(cmd.getOptionValue("interval", "2"));
        String durationStr = cmd.getOptionValue("duration");

        try {
            int pid = Integer.parseInt(pidStr);
            Integer duration = durationStr != null ? Integer.parseInt(durationStr) : null;

            logger.info("Starting monitoring for PID: {}, interval: {}s, duration: {}",
                    pid, interval, duration != null ? duration + "s" : "unlimited");

            RealTimeDashboard dashboard = new RealTimeDashboard();
            dashboard.startMonitoring(pid, interval, duration);

        } catch (NumberFormatException e) {
            System.err.println("Invalid PID format: " + pidStr);
        }
    }
    private void handlePrometheusCommand(CommandLine cmd) {
        String portStr = cmd.getOptionValue("prometheus-port", "9091");
        String pidStr = cmd.getOptionValue("monitor");

        try {
            int port = Integer.parseInt(portStr);
            int pid = pidStr != null ? Integer.parseInt(pidStr) : 0;

            logger.info("Starting Prometheus metrics server on port {}", port);

            PrometheusMetricsExporter exporter = new PrometheusMetricsExporter(port);
            exporter.start();

            if (pid > 0) {
                logger.info("Monitoring PID {} and exporting to Prometheus", pid);
                // You could start monitoring here and export metrics
            }

            System.out.println("✅ Prometheus metrics server started successfully!");
            System.out.println("📊 Metrics available at: http://localhost:" + port + "/metrics");
            System.out.println("Press Ctrl+C to stop the server...");

            // Keep the server running
            Thread.currentThread().join();

        } catch (NumberFormatException e) {
            System.err.println("Invalid port number: " + portStr);
        } catch (IOException e) {
            System.err.println("Failed to start Prometheus server: " + e.getMessage());
        } catch (InterruptedException e) {
            System.out.println("\nPrometheus server stopped.");
        }
    }

    private void handleAnalyzeGcCommand(CommandLine cmd) {
        String logFile = cmd.getOptionValue("analyze-gc");
        String outputFormat = cmd.getOptionValue("output", "text");

        try {
            logger.info("Analyzing GC log file: {}", logFile);

            GCLogParser parser = new GCLogParser();
            GCLog gcLog = parser.parseLogFile(logFile);

            GCLogAnalyzer analyzer = new GCLogAnalyzer();
            PauseAnalysis analysis = analyzer.analyze(gcLog);

            // Generate report based on output format
            generateGcReport(analysis, outputFormat);

        } catch (IOException e) {
            System.err.println("Error reading GC log file: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error analyzing GC log: " + e.getMessage());
            logger.error("GC analysis error", e);
        }
    }

    private void handleDetectLeaksCommand(CommandLine cmd) {
        String logFile = cmd.getOptionValue("detect-leaks");

        try {
            logger.info("Detecting memory leaks in GC log file: {}", logFile);

            GCLogParser parser = new GCLogParser();
            GCLog gcLog = parser.parseLogFile(logFile);

            MemoryLeakDetector leakDetector = new MemoryLeakDetector();
            MemoryLeakDetector.LeakAnalysisResult result = leakDetector.detectMemoryLeak(gcLog);

            generateLeakReport(result, gcLog);

        } catch (IOException e) {
            System.err.println("Error reading GC log file: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error detecting memory leaks: " + e.getMessage());
            logger.error("Leak detection error", e);
        }
    }

    private void generateGcReport(PauseAnalysis analysis, String format) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("                      GC LOG ANALYSIS REPORT");
        System.out.println("=".repeat(80));

        GCLog gcLog = analysis.getGcLog();

        // Basic Information
        System.out.printf("GC Type: %s\n", gcLog.getGcType());
        System.out.printf("JVM Version: %s\n", gcLog.getJvmVersion() != null ? gcLog.getJvmVersion() : "Unknown");
        System.out.printf("Log File: %s\n", gcLog.getLogFile());
        System.out.printf("Analysis Period: %,d events over %.1f minutes\n",
                analysis.getTotalEvents(),
                (gcLog.getEndTime() - gcLog.getStartTime()) / (1000.0 * 60.0));

        System.out.println("\n" + "-".repeat(80));
        System.out.println("                         STATISTICS");
        System.out.println("-".repeat(80));

        // Statistics
        System.out.printf("Total GC Events: %,d\n", analysis.getTotalEvents());
        System.out.printf("  - Minor GC: %,d\n", analysis.getMinorGcCount());
        System.out.printf("  - Major GC: %,d\n", analysis.getMajorGcCount());
        System.out.printf("Total GC Time: %.3f seconds\n", analysis.getTotalGcTime() / 1000.0);
        System.out.printf("GC Time Percentage: %.2f%%\n", analysis.getGcTimePercentage());
        System.out.printf("Application Throughput: %.2f%%\n", 100 - analysis.getGcTimePercentage());

        System.out.println("\nPause Times:");
        System.out.printf("  Longest Pause: %.3f seconds\n", analysis.getLongestPause() / 1000.0);
        System.out.printf("  Average Pause: %.3f seconds\n", analysis.getAveragePause() / 1000.0);
        System.out.printf("  P50: %.3f seconds\n", analysis.getP50() / 1000.0);
        System.out.printf("  P90: %.3f seconds\n", analysis.getP90() / 1000.0);
        System.out.printf("  P95: %.3f seconds\n", analysis.getP95() / 1000.0);
        System.out.printf("  P99: %.3f seconds\n", analysis.getP99() / 1000.0);

        System.out.printf("Memory Efficiency: %.1f%%\n", analysis.getAverageMemoryEfficiency());

        // Issues
        if (analysis.getIssueCount() > 0) {
            System.out.println("\n" + "-".repeat(80));
            System.out.println("                         ISSUES DETECTED");
            System.out.println("-".repeat(80));

            for (String issue : analysis.getIssues()) {
                String[] parts = issue.split(":", 3);
                String severity = parts[2];
                String message = parts[1];

                String icon = "⚠️";
                if ("CRITICAL".equals(severity)) {
                    icon = "🚨";
                }

                System.out.printf("%s [%s] %s\n", icon, severity, message);
            }
        } else {
            System.out.println("\n✅ No significant issues detected");
        }

        // Recommendations
        if (!analysis.getRecommendations().isEmpty()) {
            System.out.println("\n" + "-".repeat(80));
            System.out.println("                         RECOMMENDATIONS");
            System.out.println("-".repeat(80));

            for (int i = 0; i < analysis.getRecommendations().size(); i++) {
                System.out.printf("%d. %s\n", i + 1, analysis.getRecommendations().get(i));
            }
        }

        System.out.println("\n" + "=".repeat(80));
        System.out.println("Analysis completed at: " + new java.util.Date());
        System.out.println("=".repeat(80));
    }

    private void generateLeakReport(MemoryLeakDetector.LeakAnalysisResult result, GCLog gcLog) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("                   MEMORY LEAK DETECTION REPORT");
        System.out.println("=".repeat(80));

        System.out.printf("GC Log: %s\n", gcLog.getLogFile());
        System.out.printf("Total Events Analyzed: %,d\n", gcLog.getEvents().size());
        System.out.printf("Analysis Date: %s\n", new java.util.Date());

        System.out.println("\n" + "-".repeat(80));
        System.out.println("                      LEAK ANALYSIS RESULT");
        System.out.println("-".repeat(80));

        if (result.isLeakDetected()) {
            System.out.println("🚨 MEMORY LEAK DETECTED!");
            System.out.printf("Confidence: %.1f%%\n", result.getConfidence() * 100);
            System.out.printf("Pattern Type: %s\n", result.getPatternType());
            System.out.printf("Growth Rate: %.2f MB/minute\n", result.getGrowthRate() / (1024 * 1024));
            System.out.printf("Description: %s\n", result.getDescription());

            if (!result.getSuspiciousEvents().isEmpty()) {
                System.out.println("\nSuspicious Events:");
                result.getSuspiciousEvents().stream()
                        .limit(5) // Show first 5 suspicious events
                        .forEach(event -> System.out.printf("  - %s: Heap after GC: %.1f MB\n",
                                event.getEventDate(),
                                event.getHeapAfter() / (1024.0 * 1024.0)));
            }
        } else {
            System.out.println("✅ No memory leak detected");
            System.out.printf("Confidence: %.1f%%\n", result.getConfidence() * 100);
            System.out.printf("Description: %s\n", result.getDescription());
        }

        System.out.println("\n" + "=".repeat(80));
    }
}