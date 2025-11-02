public class TestApp {
    public static void main(String[] args) throws Exception {
        System.out.println("Test JVM Application Started with JMX enabled...");
        System.out.println("PID: " + ProcessHandle.current().pid());
        System.out.println("This app will run and consume memory for testing...");

        // Create some memory activity
        java.util.List<byte[]> memoryChunks = new java.util.ArrayList<>();

        int counter = 0;
        while (true) {
            // Allocate some memory periodically
            if (counter % 10 == 0) {
                byte[] chunk = new byte[1024 * 1024]; // 1MB
                memoryChunks.add(chunk);
                System.out.println("Allocated " + memoryChunks.size() + " MB");
            }

            // Some CPU work
            double calculation = Math.sqrt(counter * Math.PI);

            Thread.sleep(1000);
            counter++;

            // Remove some chunks occasionally to simulate GC
            if (memoryChunks.size() > 50) {
                memoryChunks.remove(0);
                System.out.println("Removed old memory chunk");
            }
        }
    }
}