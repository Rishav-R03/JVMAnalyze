@echo off
echo Starting Prometheus Metrics Server...
echo.
java -cp target\jvm-profiler-tool-1.0.0.jar com.jvmprofiler.monitor.prometheus.PrometheusMetricsExporter %*