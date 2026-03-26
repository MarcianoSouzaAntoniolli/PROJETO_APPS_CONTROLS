package java.lang.management;

public class MemoryUsage {
    private final long max;

    public MemoryUsage(long init, long used, long committed, long max) {
        this.max = max;
    }

    public long getMax() {
        return Runtime.getRuntime().maxMemory();
    }

    public long getUsed() {
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }

    public long getCommitted() {
        return Runtime.getRuntime().totalMemory();
    }

    public long getInit() {
        return -1;
    }
}
