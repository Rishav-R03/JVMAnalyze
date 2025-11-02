package com.jvmprofiler.monitor;

import com.jvmprofiler.monitor.model.JVMMetrics;
import java.lang.management.MemoryUsage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RealTimeDashboard {
    private static final Logger logger = LogManager.getLogger(RealTimeDashboard.class);
    private volatile boolean monitoring = false;

    public void startMonitoring(int pid, int intervalSeconds, Integer durationSeconds) {
        JVMProfilerJMXConnector jmxConnector = new JVMProfilerJMXConnector();

        if (!jmxConnector.connect(pid)) {
            System.err.println("Failed to connect to JVM process: " + pid);
            System.err.println("Make sure the target JVM has JMX enabled with:");
            System.err.println("  -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=9090");
            System.err.println("  -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false");
            return;
        }

        monitoring = true;
        long startTime = System.currentTimeMillis();
        long durationMillis = durationSeconds != null ? durationSeconds * 1000L : Long.MAX_VALUE;

        try {
            // Clear screen and setup
            clearScreen();
            System.out.println("🚀 JVM Profiler - Real-time Monitoring");
            System.out.println("Monitoring PID: " + pid + " | Interval: " + intervalSeconds + "s");
            System.out.println("Press Ctrl+C to stop monitoring\n");

            while (monitoring && (System.currentTimeMillis() - startTime) < durationMillis) {
                JVMMetrics metrics = jmxConnector.collectMetrics();
                updateDashboard(metrics);

                Thread.sleep(intervalSeconds * 1000L);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.info("Monitoring interrupted");
        } catch (Exception e) {
            logger.error("Monitoring error: {}", e.getMessage(), e);
            System.err.println("Monitoring error: " + e.getMessage());
        } finally {
            monitoring = false;
            jmxConnector.disconnect();
            System.out.println("\nMonitoring stopped.");
        }
    }

    private void updateDashboard(JVMMetrics metrics) {
        // Move cursor to top of dashboard area
        System.out.print("\033[4A\033[0J");

        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║                     JVM REAL-TIME METRICS                 ║");
        System.out.println("╠════════════════════════════════════════════════════════════╣");

        // Memory Section
        MemoryUsage heap = metrics.getHeapMemory();
        double heapUsagePercent = heap.getMax() > 0 ? (double) heap.getUsed() / heap.getMax() * 100 : 0;

        System.out.printf("║ Heap Memory:    %6.2f MB / %6.2f MB (%5.1f%%) %s ║\n",
                bytesToMB(heap.getUsed()),
                bytesToMB(heap.getMax()),
                heapUsagePercent,
                createProgressBar(heapUsagePercent));

        MemoryUsage nonHeap = metrics.getNonHeapMemory();
        double nonHeapUsagePercent = nonHeap.getCommitted() > 0 ? (double) nonHeap.getUsed() / nonHeap.getCommitted() * 100 : 0;

        System.out.printf("║ Non-Heap Memory:%6.2f MB / %6.2f MB (%5.1f%%)         ║\n",
                bytesToMB(nonHeap.getUsed()),
                bytesToMB(nonHeap.getCommitted()),
                nonHeapUsagePercent);

        // GC Section
        System.out.printf("║ GC Count: %-8d GC Time: %-8d ms                  ║\n",
                metrics.getGcCount(),
                metrics.getGcTime());

        // Threads Section
        System.out.printf("║ Threads: %-4d (Peak: %-4d, Total Started: %-6d) ║\n",
                metrics.getThreadCount(),
                metrics.getPeakThreadCount(),
                metrics.getTotalStartedThreads());

        // Timestamp
        System.out.printf("║ Last Update: %-30s           ║\n",
                new java.util.Date(metrics.getTimestamp()));

        System.out.println("╚════════════════════════════════════════════════════════════╝");
    }

    private String createProgressBar(double percentage) {
        int bars = (int) (percentage / 5);
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < 20; i++) {
            if (i < bars) {
                sb.append("█");
            } else {
                sb.append(" ");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    private double bytesToMB(long bytes) {
        return bytes / (1024.0 * 1024.0);
    }

    private void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    public void stopMonitoring() {
        monitoring = false;
    }
}