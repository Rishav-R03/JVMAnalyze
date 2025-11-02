package com.jvmprofiler.analyzer.parsers;

import com.jvmprofiler.analyzer.model.GCEvent;
import com.jvmprofiler.analyzer.model.GCLog;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class ZGCParser {
    private static final Pattern ZGC_PATTERN = Pattern.compile(
            "\\[(?<timestamp>\\d+\\.\\d+)s\\]\\[info\\]\\[gc.*?\\]\\s+" +
                    "(?<phase>.*?)\\s+" +
                    "(?<duration>\\d+\\.\\d+)ms"
    );

    private static final Pattern MEMORY_PATTERN = Pattern.compile(
            "(?<before>\\d+)M->(?<after>\\d+)M\\((?<committed>\\d+)M\\)"
    );

    public void parse(List<String> lines, GCLog gcLog) {
        for (String line : lines) {
            if (line.contains("[gc") && line.contains("Pause")) {
                parseZGCEvent(line, gcLog);
            } else if (line.contains("CommandLineFlags")) {
                parseJVMFlags(line, gcLog);
            }
        }
    }

    private void parseZGCEvent(String line, GCLog gcLog) {
        Matcher matcher = ZGC_PATTERN.matcher(line);
        if (matcher.find()) {
            try {
                GCEvent event = new GCEvent();

                // Parse basic event info
                double timestampSec = Double.parseDouble(matcher.group("timestamp"));
                event.setTimestamp((long)(timestampSec * 1000));

                String phase = matcher.group("phase").trim();
                event.setGcType("ZGC " + phase);
                event.setGcCause("Allocation");

                // ZGC pauses are typically very short
                double durationMs = Double.parseDouble(matcher.group("duration"));
                event.setDuration((long) durationMs);

                // ZGC doesn't have traditional major/minor distinction
                event.setMajorGc(phase.contains("Mark") || phase.contains("Relocate"));

                // Parse memory information if available
                parseZGCMemory(line, event);

                gcLog.addEvent(event);

            } catch (Exception e) {
                System.err.println("Error parsing ZGC line: " + line);
            }
        }
    }

    private void parseZGCMemory(String line, GCEvent event) {
        Matcher memMatcher = MEMORY_PATTERN.matcher(line);
        if (memMatcher.find()) {
            event.setHeapBefore(parseMemorySizeZGC(memMatcher.group("before")));
            event.setHeapAfter(parseMemorySizeZGC(memMatcher.group("after")));
            event.setHeapCommitted(parseMemorySizeZGC(memMatcher.group("committed")));
        }
    }

    private long parseMemorySizeZGC(String sizeStr) {
        // ZGC typically uses MB in logs
        return Long.parseLong(sizeStr) * 1024 * 1024;
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