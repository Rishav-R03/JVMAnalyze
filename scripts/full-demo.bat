@echo off
echo 🚀 JVM Profiler - Complete Demo
echo ==============================
echo.

echo Step 1: Cleanup - Stopping any running services...
taskkill /f /im java.exe 2>nul
timeout /t 2 /nobreak > nul

echo Step 2: Building project...
call mvn clean compile package -q
echo ✅ Build completed!

echo.
echo Step 3: Starting Test Application on port 9091...
start "TestApp" cmd /c "java -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=9091 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -cp target/classes TestApp"
timeout /t 3 /nobreak > nul

echo Step 4: Starting Prometheus Metrics Server on port 9090...
start "Prometheus" cmd /c "java -cp target\jvm-profiler-tool-1.0.0.jar com.jvmprofiler.monitor.prometheus.PrometheusMetricsExporter"
timeout /t 2 /nobreak > nul

echo.
echo 📊 Services Started:
echo - Test App:    JMX on port 9091
echo - Prometheus:  Metrics on http://localhost:9090/metrics
echo.
echo Step 5: Finding TestApp PID...
jps -l | findstr TestApp

echo.
echo Step 6: Now run monitoring (replace 1234 with actual PID):
echo   java -jar target\jvm-profiler-tool-1.0.0.jar --monitor 1234 --prometheus
echo.
pause