package com.jvmprofiler.cli;

import com.jvmprofiler.monitor.RealTimeDashboard;
import org.apache.commons.cli.CommandLine;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
}