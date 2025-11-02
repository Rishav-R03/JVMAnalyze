package com.jvmprofiler;

import com.jvmprofiler.cli.CLIHandler;
import com.jvmprofiler.cli.CommandLineParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main {
    private static final Logger logger = LogManager.getLogger(Main.class);

    public static void main(String[] args) {
        try {
            logger.info("Starting JVM Profiler Tool");

            CommandLineParser parser = new CommandLineParser();
            CLIHandler handler = new CLIHandler();

            if (args.length == 0) {
                parser.printHelp();
                return;
            }

            handler.handleCommand(args);

        } catch (Exception e) {
            logger.error("Application error: {}", e.getMessage(), e);
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }
}