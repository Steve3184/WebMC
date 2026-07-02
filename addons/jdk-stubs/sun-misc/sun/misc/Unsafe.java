package sun.misc;

import java.lang.reflect.Field;

public final class Unsafe {
    public static final int ARRAY_BOOLEAN_BASE_OFFSET = 0;
    public static final int ARRAY_BYTE_BASE_OFFSET = 0;
    public static final int ARRAY_SHORT_BASE_OFFSET = 0;
    public static final int ARRAY_CHAR_BASE_OFFSET = 0;
    public static final int ARRAY_INT_BASE_OFFSET = 0;
    public static final int ARRAY_LONG_BASE_OFFSET = 0;
    public static final int ARRAY_FLOAT_BASE_OFFSET = 0;
    public static final int ARRAY_DOUBLE_BASE_OFFSET = 0;
    public static final int ARRAY_OBJECT_BASE_OFFSET = 0;

    public static final int ARRAY_BOOLEAN_INDEX_SCALE = 1;
    public static final int ARRAY_BYTE_INDEX_SCALE = 1;
    public static final int ARRAY_SHORT_INDEX_SCALE = 2;
    public static final int ARRAY_CHAR_INDEX_SCALE = 2;
    public static final int ARRAY_INT_INDEX_SCALE = 4;
    public static final int ARRAY_LONG_INDEX_SCALE = 8;
    public static final int ARRAY_FLOAT_INDEX_SCALE = 4;
    public static final int ARRAY_DOUBLE_INDEX_SCALE = 8;
    public static final int ARRAY_OBJECT_INDEX_SCALE = 4;

    public static final int ADDRESS_SIZE = 8;

    private Unsafe() {}

    public static Unsafe getUnsafe() {
        throw new UnsupportedOperationException("Unsafe is not available in the browser runtime");
    }

    public long objectFieldOffset(Field f) { return 0L; }
    public long staticFieldOffset(Field f) { return 0L; }
    public Object staticFieldBase(Field f) { return null; }

    public Object getObject(Object o, long offset) { return null; }
    public void putObject(Object o, long offset, Object x) {}
    public Object getObjectVolatile(Object o, long offset) { return null; }
    public void putObjectVolatile(Object o, long offset, Object x) {}
    public void putOrderedObject(Object o, long offset, Object x) {}

    public boolean getBoolean(Object o, long offset) { return false; }
    public void putBoolean(Object o, long offset, boolean x) {}
    public byte getByte(Object o, long offset) { return 0; }
    public void putByte(Object o, long offset, byte x) {}
    public short getShort(Object o, long offset) { return 0; }
    public void putShort(Object o, long offset, short x) {}
    public char getChar(Object o, long offset) { return 0; }
    public void putChar(Object o, long offset, char x) {}
    public int getInt(Object o, long offset) { return 0; }
    public void putInt(Object o, long offset, int x) {}
    public void putOrderedInt(Object o, long offset, int x) {}
    public int getIntVolatile(Object o, long offset) { return 0; }
    public void putIntVolatile(Object o, long offset, int x) {}
    public long getLong(Object o, long offset) { return 0L; }
    public void putLong(Object o, long offset, long x) {}
    public void putOrderedLong(Object o, long offset, long x) {}
    public long getLongVolatile(Object o, long offset) { return 0L; }
    public void putLongVolatile(Object o, long offset, long x) {}
    public float getFloat(Object o, long offset) { return 0F; }
    public void putFloat(Object o, long offset, float x) {}
    public double getDouble(Object o, long offset) { return 0D; }
    public void putDouble(Object o, long offset, double x) {}

    public byte getByte(long address) { return 0; }
    public void putByte(long address, byte x) {}
    public short getShort(long address) { return 0; }
    public void putShort(long address, short x) {}
    public char getChar(long address) { return 0; }
    public void putChar(long address, char x) {}
    public int getInt(long address) { return 0; }
    public void putInt(long address, int x) {}
    public long getLong(long address) { return 0L; }
    public void putLong(long address, long x) {}
    public float getFloat(long address) { return 0F; }
    public void putFloat(long address, float x) {}
    public double getDouble(long address) { return 0D; }
    public void putDouble(long address, double x) {}
    public long getAddress(long address) { return 0L; }
    public void putAddress(long address, long x) {}

    public boolean compareAndSwapObject(Object o, long offset, Object expected, Object x) { return false; }
    public boolean compareAndSwapInt(Object o, long offset, int expected, int x) { return false; }
    public boolean compareAndSwapLong(Object o, long offset, long expected, long x) { return false; }

    public int getAndAddInt(Object o, long offset, int delta) { return 0; }
    public long getAndAddLong(Object o, long offset, long delta) { return 0L; }
    public int getAndSetInt(Object o, long offset, int x) { return 0; }
    public long getAndSetLong(Object o, long offset, long x) { return 0L; }
    public Object getAndSetObject(Object o, long offset, Object x) { return null; }

    public int arrayBaseOffset(Class<?> arrayClass) { return 0; }
    public int arrayIndexScale(Class<?> arrayClass) { return 1; }
    public int addressSize() { return 8; }
    public int pageSize() { return 4096; }

    public long allocateMemory(long bytes) { throw new UnsupportedOperationException(); }
    public long reallocateMemory(long address, long bytes) { throw new UnsupportedOperationException(); }
    public void freeMemory(long address) {}
    public void setMemory(long address, long bytes, byte value) {}
    public void copyMemory(long srcAddress, long destAddress, long bytes) {}
    public void copyMemory(Object srcBase, long srcOffset, Object destBase, long destOffset, long bytes) {}

    public Object allocateInstance(Class<?> cls) throws InstantiationException {
        throw new InstantiationException("not supported");
    }

    public void throwException(Throwable e) {
        if (e instanceof RuntimeException) throw (RuntimeException) e;
        if (e instanceof Error) throw (Error) e;
        throw new RuntimeException(e);
    }

    public void monitorEnter(Object o) {}
    public void monitorExit(Object o) {}
    public boolean tryMonitorEnter(Object o) { return true; }

    public void loadFence() {}
    public void storeFence() {}
    public void fullFence() {}

    public void park(boolean isAbsolute, long time) {}
    public void unpark(Object thread) {}

    public boolean shouldBeInitialized(Class<?> c) { return false; }
    public void ensureClassInitialized(Class<?> c) {}
}
