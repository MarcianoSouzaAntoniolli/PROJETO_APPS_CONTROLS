package java.lang.management;

public class ManagementFactory {

    private static final MemoryMXBean MEMORY_MX_BEAN = new MemoryMXBean() {
        @Override
        public MemoryUsage getHeapMemoryUsage() {
            long max = Runtime.getRuntime().maxMemory();
            long total = Runtime.getRuntime().totalMemory();
            long free = Runtime.getRuntime().freeMemory();
            return new MemoryUsage(-1, total - free, total, max);
        }

        @Override
        public MemoryUsage getNonHeapMemoryUsage() {
            return new MemoryUsage(-1, 0, 0, -1);
        }

        @Override
        public int getObjectPendingFinalizationCount() {
            return 0;
        }

        @Override
        public boolean isVerbose() {
            return false;
        }

        @Override
        public void setVerbose(boolean value) {}

        @Override
        public void gc() {
            Runtime.getRuntime().gc();
        }
    };

    public static MemoryMXBean getMemoryMXBean() {
        return MEMORY_MX_BEAN;
    }
}
