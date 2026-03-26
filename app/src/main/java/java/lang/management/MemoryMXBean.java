package java.lang.management;

public interface MemoryMXBean {
    MemoryUsage getHeapMemoryUsage();
    MemoryUsage getNonHeapMemoryUsage();
    int getObjectPendingFinalizationCount();
    boolean isVerbose();
    void setVerbose(boolean value);
    void gc();
}
