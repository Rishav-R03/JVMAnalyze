package com.jvmprofiler.analyzer.parsers;

import com.jvmprofiler.analyzer.model.GCEvent;
import com.jvmprofiler.analyzer.model.GCLog;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class G1GCParser {
    private static final Pattern G1_PATTERN = Pattern.compile(
            "\\[(?<timestamp>\\d+\\.\\d+)s\\]\\[info\\]\\[gc.*?\\]\\s+" +
                    "(?<type>G1.*?)\\s+" +
                    "\\((?<cause>.*?)\\)\\s+" +
                    "(?<before>\\d+)(?:K|M)?->(?<after>\\d+)(?:K|M)?->(?<committed>\\d+)(?:K|M)?\\s+" +
                    "(?<duration>\\d+\\.\\d+)s"
    );

    private static final Pattern MEMORY_PATTERN = Pattern.compile(
            "\\[(?<youngBefore>\\d+)K->(?<youngAfter>\\d+)K\\((?<youngCommitted>\\d+)K\\)\\]\\s+" +
                    "\\[(?<oldBefore>\\d+)K->(?<oldAfter>\\d+)K\\((?<oldCommitted>\\d+)K\\)\\]"
    );

    public void parse(List<String> lines, GCLog gcLog) {
        for (String line : lines) {
            if (line.contains("[gc") && !line.contains("ergo")) {
                parseG1Event(line, gcLog);
            } else if (line.contains("CommandLineFlags")) {
                parseJVMFlags(line, gcLog);
            }
        }
    }

    private void parseG1Event(String line, GCLog gcLog) {
        Matcher matcher = G1_PATTERN.matcher(line);
        if (matcher.find()) {
            try {
                GCEvent event = new GCEvent();

                // Parse basic event info
                double timestampSec = Double.parseDouble(matcher.group("timestamp"));
                event.setTimestamp((long)(timestampSec * 1000));
                event.setGcType(matcher.group("type").trim());
                event.setGcCause(matcher.group("cause").trim());

                // Parse memory sizes (convert from K/M to bytes)
                long before = parseMemorySize(matcher.group("before"));
                long after = parseMemorySize(matcher.group("after"));
                long committed = parseMemorySize(matcher.group("committed"));

                event.setHeapBefore(before);
                event.setHeapAfter(after);
                event.setHeapCommitted(committed);

                // Parse duration (convert seconds to milliseconds)
                double durationSec = Double.parseDouble(matcher.group("duration"));
                event.setDuration((long)(durationSec * 1000));

                // Determine if this is a major GC
                event.setMajorGc(isMajorGc(event.getGcType()));

                // Try to parse young/old gen details
                parseMemoryDetails(line, event);

                gcLog.addEvent(event);

            } catch (Exception e) {
                // Log parsing error but continue
                System.err.println("Error parsing G1GC line: " + line);
            }
        }
    }

    private void parseMemoryDetails(String line, GCEvent event) {
        Matcher memMatcher = MEMORY_PATTERN.matcher(line);
        if (memMatcher.find()) {
            event.setYoungBefore(parseMemorySize(memMatcher.group("youngBefore")));
            event.setYoungAfter(parseMemorySize(memMatcher.group("youngAfter")));
            event.setOldBefore(parseMemorySize(memMatcher.group("oldBefore")));
            event.setOldAfter(parseMemorySize(memMatcher.group("oldAfter")));
        }
    }

    private long parseMemorySize(String sizeStr) {
        // Handle K/M suffixes and convert to bytes
        if (sizeStr.endsWith("K")) {
            return Long.parseLong(sizeStr.replace("K", "")) * 1024;
        } else if (sizeStr.endsWith("M")) {
            return Long.parseLong(sizeStr.replace("M", "")) * 1024 * 1024;
        } else {
            // Assume bytes if no suffix
            return Long.parseLong(sizeStr);
        }
    }

    private boolean isMajorGc(String gcType) {
        return gcType.contains("Full") || gcType.contains("Mixed") || gcType.contains("Remark");
    }

    private void parseJVMFlags(String line, GCLog gcLog) {
        if (line.contains("java version")) {
            // Extract JVM version
            int start = line.indexOf("java version");
            if (start != -1) {
                gcLog.setJvmVersion(line.substring(start));
            }
        }
    }
}