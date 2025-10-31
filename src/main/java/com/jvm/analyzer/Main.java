package com.jvm.analyzer;

import com.jvm.analyzer.jmx.JMXConnectionManager;
import com.jvm.analyzer.jmx.MetricCollector;
import com.jvm.analyzer.model.JVMMetrics;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Main entry point for JVM Performance Analyzer
 */
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final String VERSION = "1.0.0";

    public static void main(String[] args) {
        Options options = createOptions();
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();

        try {
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("help")) {
                printHelp(formatter, options);
                return;
            }

            if (cmd.hasOption("version")) {
                System.out.println("JVM Performance Analyzer v" + VERSION);
                return;
            }

            // Get connection parameters
            String host = cmd.getOptionValue("host", "localhost");
            int port = Integer.parseInt(cmd.getOptionValue("port", "0"));
            long pid = Long.parseLong(cmd.getOptionValue("pid", "0"));

            boolean continuous = cmd.hasOption("continuous");
            int interval = Integer.parseInt(cmd.getOptionValue("interval", "5"));

            // Connect to JVM
            JMXConnectionManager connectionManager = new JMXConnectionManager();

            if (pid > 0) {
                logger.info("Connecting to local JVM with PID: {}", pid);
                connectionManager.connectLocal(pid);
            } else if (port > 0) {
                logger.info("Connecting to remote JVM at {}:{}", host, port);
                connectionManager.connectRemote(host, port);
            } else {
                // Connect to current JVM
                logger.info("Connecting to current JVM");
                connectionManager.connectLocal(ProcessHandle.current().pid());
            }

            MetricCollector collector = new MetricCollector(connectionManager);

            if (continuous) {
                logger.info("Starting continuous monitoring (interval: {} seconds)", interval);
                monitorContinuously(collector, interval);
            } else {
                // Single snapshot
                collectAndDisplay(collector);
            }

            connectionManager.disconnect();

        } catch (ParseException e) {
            System.err.println("Error parsing command line: " + e.getMessage());
            printHelp(formatter, options);
            System.exit(1);
        } catch (Exception e) {
            logger.error("Error during execution", e);
            System.exit(1);
        }
    }

    private static Options createOptions() {
        Options options = new Options();

        options.addOption(Option.builder("h")
                .longOpt("help")
                .desc("Display help information")
                .build());

        options.addOption(Option.builder("v")
                .longOpt("version")
                .desc("Display version information")
                .build());

        options.addOption(Option.builder("p")
                .longOpt("pid")
                .hasArg()
                .argName("PID")
                .desc("Process ID of the target JVM")
                .build());

        options.addOption(Option.builder("H")
                .longOpt("host")
                .hasArg()
                .argName("HOST")
                .desc("Remote JVM hostname (default: localhost)")
                .build());

        options.addOption(Option.builder("P")
                .longOpt("port")
                .hasArg()
                .argName("PORT")
                .desc("Remote JVM JMX port")
                .build());

        options.addOption(Option.builder("c")
                .longOpt("continuous")
                .desc("Enable continuous monitoring")
                .build());

        options.addOption(Option.builder("i")
                .longOpt("interval")
                .hasArg()
                .argName("SECONDS")
                .desc("Monitoring interval in seconds (default: 5)")
                .build());

        options.addOption(Option.builder("o")
                .longOpt("output")
                .hasArg()
                .argName("FILE")
                .desc("Output file for metrics (JSON format)")
                .build());

        return options;
    }

    private static void printHelp(HelpFormatter formatter, Options options) {
        System.out.println("JVM Performance Analyzer v" + VERSION);
        System.out.println("\nA command-line tool to analyze JVM performance metrics\n");

        formatter.printHelp("jvm-analyzer [OPTIONS]",
                "\nOptions:", options,
                "\nExamples:\n" +
                        "  jvm-analyzer -p 12345                    # Monitor local JVM with PID 12345\n" +
                        "  jvm-analyzer -H localhost -P 9010        # Monitor remote JVM\n" +
                        "  jvm-analyzer -p 12345 -c -i 10          # Continuous monitoring every 10s\n" +
                        "  jvm-analyzer -p 12345 -o metrics.json   # Save metrics to file\n",
                true);
    }

    private static void collectAndDisplay(MetricCollector collector) throws IOException {
        JVMMetrics metrics = collector.collectMetrics();
        displayMetrics(metrics);
    }

    private static void monitorContinuously(MetricCollector collector, int intervalSeconds) {
        System.out.println("\n=== Starting Continuous Monitoring ===");
        System.out.println("Press Ctrl+C to stop\n");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n\nStopping monitoring...");
        }));

        while (true) {
            try {
                JVMMetrics metrics = collector.collectMetrics();
                displayMetrics(metrics);
                Thread.sleep(intervalSeconds * 1000L);
            } catch (InterruptedException e) {
                logger.info("Monitoring interrupted");
                break;
            } catch (Exception e) {
                logger.error("Error collecting metrics", e);
            }
        }
    }

    private static void displayMetrics(JVMMetrics metrics) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("JVM Performance Metrics - " + metrics.getTimestamp());
        System.out.println("=".repeat(80));

        System.out.println("\n--- Memory (Heap) ---");
        System.out.printf("Used:      %,d MB\n", metrics.getHeapUsed() / (1024 * 1024));
        System.out.printf("Committed: %,d MB\n", metrics.getHeapCommitted() / (1024 * 1024));
        System.out.printf("Max:       %,d MB\n", metrics.getHeapMax() / (1024 * 1024));
        System.out.printf("Usage:     %.2f%%\n",
                (metrics.getHeapUsed() * 100.0 / metrics.getHeapMax()));

        System.out.println("\n--- Memory (Non-Heap) ---");
        System.out.printf("Used:      %,d MB\n", metrics.getNonHeapUsed() / (1024 * 1024));
        System.out.printf("Committed: %,d MB\n", metrics.getNonHeapCommitted() / (1024 * 1024));

        System.out.println("\n--- Garbage Collection ---");
        System.out.printf("Young GC Count:  %d\n", metrics.getYoungGcCount());
        System.out.printf("Young GC Time:   %,d ms\n", metrics.getYoungGcTime());
        System.out.printf("Old GC Count:    %d\n", metrics.getOldGcCount());
        System.out.printf("Old GC Time:     %,d ms\n", metrics.getOldGcTime());
        System.out.printf("Total GC Time:   %,d ms\n",
                metrics.getYoungGcTime() + metrics.getOldGcTime());

        System.out.println("\n--- Threads ---");
        System.out.printf("Thread Count:    %d\n", metrics.getThreadCount());
        System.out.printf("Peak Threads:    %d\n", metrics.getPeakThreadCount());
        System.out.printf("Daemon Threads:  %d\n", metrics.getDaemonThreadCount());

        System.out.println("\n--- Classes ---");
        System.out.printf("Loaded:          %,d\n", metrics.getLoadedClassCount());
        System.out.printf("Total Loaded:    %,d\n", metrics.getTotalLoadedClassCount());
        System.out.printf("Unloaded:        %,d\n", metrics.getUnloadedClassCount());

        System.out.println("\n--- CPU ---");
        System.out.printf("Process CPU:     %.2f%%\n", metrics.getProcessCpuLoad() * 100);
        System.out.printf("System CPU:      %.2f%%\n", metrics.getSystemCpuLoad() * 100);

        System.out.println("\n" + "=".repeat(80) + "\n");
    }
}