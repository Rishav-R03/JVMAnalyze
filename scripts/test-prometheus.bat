@echo off
echo Testing Prometheus Integration...
echo.

echo Step 1: Stopping any running Java processes...
taskkill /f /im java.exe 2>nul
timeout /t 2 /nobreak > nul

echo Step 2: Building project...
call mvn clean compile package -q

echo.
echo ✅ Build successful!
echo.

echo Step 3: Starting Prometheus metrics server...
echo 📊 Metrics will be available at: http://localhost:9090/metrics
echo ⏹️  Press Ctrl+C to stop the server
echo.

java -cp target\jvm-profiler-tool-1.0.0.jar com.jvmprofiler.monitor.prometheus.PrometheusMetricsExporter