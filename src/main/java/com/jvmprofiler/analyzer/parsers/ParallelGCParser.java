package com.jvmprofiler.analyzer.parsers;

import com.jvmprofiler.analyzer.model.GCEvent;
import com.jvmprofiler.analyzer.model.GCLog;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class ParallelGCParser {
    private static final Pattern PARALLEL_PATTERN = Pattern.compile(
            "\\[(?<timestamp>\\d+\\.\\d+)s\\]\\s+" +
                    "\\[(?<type>.*?GC)\\]\\s+" +
                    "(?<before>\\d+)K->(?<after>\\d+)K\\((?<committed>\\d+)K\\),\\s+" +
                    "(?<duration>\\d+\\.\\d+) secs"
    );

    public void parse(List<String> lines, GCLog gcLog) {
        for (String line : lines) {
            if ((line.contains("GC") || line.contains("Full GC")) && line.contains("secs")) {
                parseParallelEvent(line, gcLog);
            } else if (line.contains("CommandLineFlags")) {
                parseJVMFlags(line, gcLog);
            }
        }
    }

    private void parseParallelEvent(String line, GCLog gcLog) {
        Matcher matcher = PARALLEL_PATTERN.matcher(line);
        if (matcher.find()) {
            try {
                GCEvent event = new GCEvent();

                double timestampSec = Double.parseDouble(matcher.group("timestamp"));
                event.setTimestamp((long)(timestampSec * 1000));

                String gcType = matcher.group("type").trim();
                event.setGcType(gcType);
                event.setGcCause("Allocation Failure");

                // Parse memory (Parallel GC uses KB)
                long before = Long.parseLong(matcher.group("before")) * 1024;
                long after = Long.parseLong(matcher.group("after")) * 1024;
                long committed = Long.parseLong(matcher.group("committed")) * 1024;

                event.setHeapBefore(before);
                event.setHeapAfter(after);
                event.setHeapCommitted(committed);

                // Parse duration (convert seconds to milliseconds)
                double durationSec = Double.parseDouble(matcher.group("duration"));
                event.setDuration((long)(durationSec * 1000));

                // Determine if major GC
                event.setMajorGc(gcType.contains("Full"));

                gcLog.addEvent(event);

            } catch (Exception e) {
                System.err.println("Error parsing ParallelGC line: " + line);
            }
        }
    }

    private void parseJVMFlags(String line, GCLog gcLog) {
        if (line.contains("java version")) {
            int start = line.indexOf("java version");
            if (start != -1) {
                gcLog.setJvmVersion(line.substring(start));
            }
        }
    }
}