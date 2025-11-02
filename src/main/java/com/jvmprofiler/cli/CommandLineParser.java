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

        options.addOption(monitor);
        options.addOption(interval);
        options.addOption(duration);
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
        System.out.println("  jvm-profiler --help");
    }
}