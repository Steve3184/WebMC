package java.util.concurrent.atomic;

public class LongAdder extends Number {
    private long value = 0L;
    public LongAdder() {}
    public void add(long x) { value += x; }
    public void increment() { value++; }
    public void decrement() { value--; }
    public long sum() { return value; }
    public void reset() { value = 0L; }
    public long sumThenReset() { long old = value; value = 0L; return old; }
    @Override public String toString() { return Long.toString(value); }
    @Override public long longValue() { return value; }
    @Override public int intValue() { return (int) value; }
    @Override public float floatValue() { return value; }
    @Override public double doubleValue() { return value; }
}
