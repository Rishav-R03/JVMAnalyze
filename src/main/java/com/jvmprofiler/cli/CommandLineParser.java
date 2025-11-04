package com.jvmprofiler.cli;

import org.apache.commons.cli.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CommandLineParser {
    private static final Logger logger = LogManager.getLogger(CommandLineParser.class);
    private final Options options;

    public CommandLineParser() {
        options = new Options();
        setupOptions();
    }

    private void setupOptions() {
        // Monitor command
        Option monitor = Option.builder("m")
                .longOpt("monitor")
                .hasArg()
                .argName("PID")
                .desc("Monitor a JVM process by PID")
                .build();

        Option interval = Option.builder("i")
                .longOpt("interval")
                .hasArg()
                .argName("seconds")
                .desc("Monitoring interval in seconds (default: 2)")
                .build();

        Option duration = Option.builder("d")
                .longOpt("duration")
                .hasArg()
                .argName("seconds")
                .desc("Monitoring duration (default: until stopped)")
                .build();

        // Analyze GC command
        Option analyzeGc = Option.builder("a")
                .longOpt("analyze-gc")
                .hasArg()
                .argName("file")
                .desc("Analyze GC log file and generate report")
                .build();

        Option output = Option.builder("o")
                .longOpt("output")
                .hasArg()
                .argName("format")
                .desc("Output format: text, json, html (default: text)")
                .build();

        Option detectLeaks = Option.builder("L")
                .longOpt("detect-leaks")
                .hasArg()
                .argName("file")
                .desc("Detect memory leaks in GC log file")
                .build();

        // Prometheus
        // Add these options to your setupOptions() method
        Option prometheus = Option.builder("P")
                .longOpt("prometheus")
                .hasArg(false)
                .desc("Enable Prometheus metrics export")
                .build();

        Option prometheusPort = Option.builder("pp")
                .longOpt("prometheus-port")
                .hasArg()
                .argName("port")
                .desc("Prometheus metrics server port (default: 9091)")
                .build();

        Option metricsServer = Option.builder("M")
                .longOpt("metrics-server")
                .hasArg(false)
                .desc("Start standalone Prometheus metrics server")
                .build();

// Add to options
        options.addOption(prometheus);
        options.addOption(prometheusPort);
        options.addOption(metricsServer);
        options.addOption(monitor);
        options.addOption(interval);
        options.addOption(duration);
        options.addOption(analyzeGc);
        options.addOption(output);
        options.addOption(detectLeaks);
        options.addOption("h", "help", false, "Show help");
    }

    public CommandLine parse(String[] args) throws ParseException {
        org.apache.commons.cli.CommandLineParser parser = new DefaultParser();
        return parser.parse(options, args);
    }

    public void printHelp() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("jvm-profiler", options);
        System.out.println("\nExamples:");
        System.out.println("  jvm-profiler --monitor 1234");
        System.out.println("  jvm-profiler --monitor 1234 --interval 1 --duration 60");
        System.out.println("  jvm-profiler --analyze-gc gc.log");
        System.out.println("  jvm-profiler --analyze-gc gc.log --output html");
        System.out.println("  jvm-profiler --detect-leaks gc.log");
        System.out.println("  jvm-profiler --help");
    }
}