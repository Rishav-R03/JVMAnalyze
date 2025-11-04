package com.jvmprofiler.monitor.prometheus;

import com.jvmprofiler.monitor.model.JVMMetrics;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;
import io.prometheus.client.hotspot.DefaultExports;
import io.prometheus.client.exporter.HTTPServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

public class PrometheusMetricsExporter {
    private static final Logger logger = LogManager.getLogger(PrometheusMetricsExporter.class);

    private HTTPServer server;
    private final int port;
    private final AtomicReference<JVMMetrics> latestMetrics = new AtomicReference<>();

    // Prometheus Metrics
    private final Gauge heapMemoryUsed;
    private final Gauge heapMemoryCommitted;
    private final Gauge heapMemoryMax;
    private final Gauge nonHeapMemoryUsed;
    private final Gauge nonHeapMemoryCommitted;
    private final Gauge nonHeapMemoryMax;
    private final Gauge threadCount;
    private final Gauge peakThreadCount;
    private final Counter gcCount;
    private final Counter gcTime;
    private final Histogram gcPauseDuration;
    private final Gauge memoryEfficiency;

    public PrometheusMetricsExporter(int port) {
        this.port = port;

        // Initialize default JVM metrics (standard JVM metrics)
        DefaultExports.initialize();

        // Custom JVM Profiler metrics
        this.heapMemoryUsed = Gauge.build()
                .name("jvm_profiler_heap_memory_used_bytes")
                .help("Current used heap memory in bytes")
                .labelNames("pid")
                .register();

        this.heapMemoryCommitted = Gauge.build()
                .name("jvm_profiler_heap_memory_committed_bytes")
                .help("Current committed heap memory in bytes")
                .labelNames("pid")
                .register();

        this.heapMemoryMax = Gauge.build()
                .name("jvm_profiler_heap_memory_max_bytes")
                .help("Maximum heap memory in bytes")
                .labelNames("pid")
                .register();

        this.nonHeapMemoryUsed = Gauge.build()
                .name("jvm_profiler_non_heap_memory_used_bytes")
                .help("Current used non-heap memory in bytes")
                .labelNames("pid")
                .register();

        this.nonHeapMemoryCommitted = Gauge.build()
                .name("jvm_profiler_non_heap_memory_committed_bytes")
                .help("Current committed non-heap memory in bytes")
                .labelNames("pid")
                .register();

        this.nonHeapMemoryMax = Gauge.build()
                .name("jvm_profiler_non_heap_memory_max_bytes")
                .help("Maximum non-heap memory in bytes")
                .labelNames("pid")
                .register();

        this.threadCount = Gauge.build()
                .name("jvm_profiler_thread_count")
                .help("Current live thread count")
                .labelNames("pid")
                .register();

        this.peakThreadCount = Gauge.build()
                .name("jvm_profiler_peak_thread_count")
                .help("Peak live thread count")
                .labelNames("pid")
                .register();

        this.gcCount = Counter.build()
                .name("jvm_profiler_gc_events_total")
                .help("Total number of GC events")
                .labelNames("pid", "gc_type")
                .register();

        this.gcTime = Counter.build()
                .name("jvm_profiler_gc_time_milliseconds_total")
                .help("Total time spent in GC in milliseconds")
                .labelNames("pid")
                .register();

        this.gcPauseDuration = Histogram.build()
                .name("jvm_profiler_gc_pause_duration_seconds")
                .help("GC pause duration in seconds")
                .labelNames("pid", "gc_type")
                .buckets(0.001, 0.005, 0.01, 0.05, 0.1, 0.5, 1.0, 5.0)
                .register();

        this.memoryEfficiency = Gauge.build()
                .name("jvm_profiler_memory_efficiency_ratio")
                .help("Memory efficiency ratio (0-1)")
                .labelNames("pid")
                .register();
    }

    public static void main(String[] args) {
        try {
            System.out.println("🚀 Starting Prometheus Metrics Server...");

            int port = 9090;
            if (args.length > 0) {
                try {
                    port = Integer.parseInt(args[0]);
                } catch (NumberFormatException e) {
                    System.err.println("Invalid port number: " + args[0] + ", using default: 9090");
                }
            }

            PrometheusMetricsExporter exporter = new PrometheusMetricsExporter(port);
            exporter.start();

            System.out.println("✅ Prometheus Metrics Server Started Successfully!");
            System.out.println("📊 Metrics available at: http://localhost:" + port + "/metrics");
            System.out.println("⏹️  Press Ctrl+C to stop the server");

            // Add some sample metrics for testing
            exporter.addSampleMetrics();

            // Keep the server running
            Thread.currentThread().join();

        } catch (Exception e) {
            System.err.println("❌ Error starting Prometheus server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    private void addSampleMetrics() {
        // Create sample metrics to demonstrate the exporter is working
        new Thread(() -> {
            try {
                int samplePid = 9999;
                String sampleGcType = "G1GC";

                while (server != null) {
                    // Update sample metrics every 5 seconds
                    heapMemoryUsed.labels(String.valueOf(samplePid)).set(256 * 1024 * 1024); // 256MB
                    heapMemoryCommitted.labels(String.valueOf(samplePid)).set(512 * 1024 * 1024); // 512MB

                    Thread.sleep(5000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    public void start() throws IOException {
        if (server != null) {
            logger.warn("Prometheus metrics server already running on port {}", port);
            return;
        }

        server = new HTTPServer(port);

        logger.info("Prometheus metrics server started on port {}", port);
        logger.info("Metrics available at: http://localhost:{}/metrics", port);
    }

    public void stop() {
        if (server != null) {
            server.stop();
            logger.info("Prometheus metrics server stopped");
        }
    }

    public void updateMetrics(JVMMetrics metrics, int targetPid, String gcType) {
        if (metrics == null) return;

        latestMetrics.set(metrics);
        String pidLabel = String.valueOf(targetPid);
        String gcTypeLabel = gcType != null ? gcType : "unknown";

        try {
            // Memory metrics
            heapMemoryUsed.labels(pidLabel).set(metrics.getHeapMemory().getUsed());
            heapMemoryCommitted.labels(pidLabel).set(metrics.getHeapMemory().getCommitted());
            heapMemoryMax.labels(pidLabel).set(metrics.getHeapMemory().getMax());

            nonHeapMemoryUsed.labels(pidLabel).set(metrics.getNonHeapMemory().getUsed());
            nonHeapMemoryCommitted.labels(pidLabel).set(metrics.getNonHeapMemory().getCommitted());
            nonHeapMemoryMax.labels(pidLabel).set(metrics.getNonHeapMemory().getMax());

            // Thread metrics
            threadCount.labels(pidLabel).set(metrics.getThreadCount());
            peakThreadCount.labels(pidLabel).set(metrics.getPeakThreadCount());

            // GC metrics - we'll update these incrementally
            // For counters, we track the absolute value and let Prometheus handle the rate

            // Calculate memory efficiency
            if (metrics.getHeapMemory().getUsed() > 0 && metrics.getNonHeapMemory().getUsed() > 0) {
                double efficiency = calculateMemoryEfficiency(metrics);
                memoryEfficiency.labels(pidLabel).set(efficiency);
            }

            logger.debug("Updated Prometheus metrics for PID: {}", targetPid);

        } catch (Exception e) {
            logger.error("Error updating Prometheus metrics: {}", e.getMessage(), e);
        }
    }

    public void updateGcMetrics(int targetPid, String gcType, long gcEvents, long gcTimeMs, long gcPauseMs) {
        String pidLabel = String.valueOf(targetPid);
        String gcTypeLabel = gcType != null ? gcType : "unknown";

        try {
            // Update GC counters
            gcCount.labels(pidLabel, gcTypeLabel).inc(gcEvents);
            gcTime.labels(pidLabel).inc(gcTimeMs);

            // Record GC pause duration
            if (gcPauseMs > 0) {
                double durationSeconds = gcPauseMs / 1000.0;
                gcPauseDuration.labels(pidLabel, gcTypeLabel).observe(durationSeconds);
            }

            logger.debug("Updated GC metrics for PID: {}, GC Type: {}", targetPid, gcType);

        } catch (Exception e) {
            logger.error("Error updating GC metrics: {}", e.getMessage(), e);
        }
    }

    private double calculateMemoryEfficiency(JVMMetrics metrics) {
        long totalUsed = metrics.getHeapMemory().getUsed() + metrics.getNonHeapMemory().getUsed();
        long totalCommitted = metrics.getHeapMemory().getCommitted() + metrics.getNonHeapMemory().getCommitted();

        if (totalCommitted == 0) return 0.0;

        return (double) totalUsed / totalCommitted;
    }

    public void recordGcPause(int targetPid, String gcType, long durationMs) {
        String pidLabel = String.valueOf(targetPid);
        String gcTypeLabel = gcType != null ? gcType : "unknown";

        double durationSeconds = durationMs / 1000.0;
        gcPauseDuration.labels(pidLabel, gcTypeLabel).observe(durationSeconds);

        logger.debug("Recorded GC pause: {}ms for PID: {}, GC Type: {}", durationMs, targetPid, gcType);
    }

    public boolean isRunning() {
        return server != null;
    }

    public int getPort() {
        return port;
    }
}