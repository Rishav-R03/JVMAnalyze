package com.jvmprofiler.analyzer;

import com.jvmprofiler.analyzer.model.GCLog;
import com.jvmprofiler.analyzer.parsers.G1GCParser;
import com.jvmprofiler.analyzer.parsers.ZGCParser;
import com.jvmprofiler.analyzer.parsers.ParallelGCParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class GCLogParser {
    private static final Logger logger = LogManager.getLogger(GCLogParser.class);

    private G1GCParser g1Parser = new G1GCParser();
    private ZGCParser zgcParser = new ZGCParser();
    private ParallelGCParser parallelParser = new ParallelGCParser();

    public GCLog parseLogFile(String filePath) throws IOException {
        logger.info("Parsing GC log file: {}", filePath);

        Path path = Path.of(filePath);
        if (!Files.exists(path)) {
            throw new IOException("GC log file not found: " + filePath);
        }

        List<String> lines = Files.readAllLines(path);
        if (lines.isEmpty()) {
            throw new IOException("GC log file is empty: " + filePath);
        }

        // Detect GC type from log content
        String gcType = detectGCType(lines);
        logger.info("Detected GC type: {}", gcType);

        GCLog gcLog = new GCLog();
        gcLog.setLogFile(filePath);
        gcLog.setGcType(gcType);

        // Parse based on detected GC type
        switch (gcType.toUpperCase()) {
            case "G1GC":
                g1Parser.parse(lines, gcLog);
                break;
            case "ZGC":
                zgcParser.parse(lines, gcLog);
                break;
            case "PARALLELGC":
                parallelParser.parse(lines, gcLog);
                break;
            default:
                logger.warn("Unknown GC type: {}, trying G1GC parser", gcType);
                g1Parser.parse(lines, gcLog);
        }

        gcLog.calculateStatistics();
        logger.info("Parsed {} GC events from log", gcLog.getEvents().size());

        return gcLog;
    }

    private String detectGCType(List<String> lines) {
        for (String line : lines) {
            String lowerLine = line.toLowerCase();

            if (lowerLine.contains("g1") || lowerLine.contains("garbage-first")) {
                return "G1GC";
            } else if (lowerLine.contains("zgc") || lowerLine.contains("z garbage")) {
                return "ZGC";
            } else if (lowerLine.contains("parallelgc") || lowerLine.contains("ps")) {
                return "ParallelGC";
            } else if (lowerLine.contains("commandlineflags")) {
                // Parse JVM flags to detect GC
                if (line.contains("-XX:+UseG1GC"))
                    return "G1GC";
                if (line.contains("-XX:+UseZGC"))
                    return "ZGC";
                if (line.contains("-XX:+UseParallelGC"))
                    return "ParallelGC";
            }
        }

        // Default to G1GC (most common)
        return "G1GC";
    }
}