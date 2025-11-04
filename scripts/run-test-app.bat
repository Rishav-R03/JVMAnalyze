@echo off
echo Compiling TestApp...
javac -d target/classes src/test/java/TestApp.java

echo.
echo Starting TestApp with JMX enabled on port 9091...
echo PID will be displayed above. Note it down for monitoring.
echo.
java -Dcom.sun.management.jmxremote ^
     -Dcom.sun.management.jmxremote.port=9091 ^
     -Dcom.sun.management.jmxremote.authenticate=false ^
     -Dcom.sun.management.jmxremote.ssl=false ^
     -cp target/classes TestApp