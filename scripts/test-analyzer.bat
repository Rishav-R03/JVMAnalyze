@echo off
echo JVM Profiler Tool - GC Analysis Demo
echo.

if not exist examples\gc-logs (
    echo Creating sample GC logs...
    mkdir examples\gc-logs
)

echo.
echo Available GC Logs:
dir examples\gc-logs\*.log /b

echo.
echo Running GC Analysis Demos...
echo.

echo 1. Analyzing G1GC Sample...
java -jar target/jvm-profiler-tool-1.0.0.jar --analyze-gc examples\gc-logs\g1gc-sample.log

echo.
echo 2. Analyzing ZGC Sample...
java -jar target\jvm-profiler-tool-1.0.0.jar --analyze-gc examples\gc-logs\zgc-sample.log

echo.
echo 3. Detecting Memory Leaks...
java -jar target\jvm-profiler-tool-1.0.0.jar --detect-leaks examples\gc-logs\memory-leak-sample.log

echo.
echo 4. Analyzing ParallelGC with Issues...
java -jar target\jvm-profiler-tool-1.0.0.jar --analyze-gc examples\gc-logs\parallelgc-sample.log

echo.
echo Demo completed!
pause