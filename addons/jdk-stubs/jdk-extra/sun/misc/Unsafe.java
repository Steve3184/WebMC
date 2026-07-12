package sun.misc;

import java.lang.reflect.Field;

/**
 * TeaVM stub for sun.misc.Unsafe.
 * All methods are no-ops as browser environment doesn't support direct memory access.
 * TeaVM configuration preserves these methods to support Netty's PlatformDependent0.
 */
@SuppressWarnings("deprecation")
public final class Unsafe {
    public static final int ARRAY_BOOLEAN_BASE_OFFSET = 0;
    public static final int ARRAY_BYTE_BASE_OFFSET = 16;
    public static final int ARRAY_SHORT_BASE_OFFSET = 16;
    public static final int ARRAY_CHAR_BASE_OFFSET = 16;
    public static final int ARRAY_INT_BASE_OFFSET = 16;
    public static final int ARRAY_LONG_BASE_OFFSET = 24;
    public static final int ARRAY_FLOAT_BASE_OFFSET = 16;
    public static final int ARRAY_DOUBLE_BASE_OFFSET = 24;
    public static final int ARRAY_OBJECT_BASE_OFFSET = 16;

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

    private static final Unsafe THE_ONE = new Unsafe();
    private Unsafe() {}

    public static Unsafe getUnsafe() {
        return THE_ONE;
    }

    public long objectFieldOffset(Field f) { return 0L; }
    public long staticFieldOffset(Field f) { return 0L; }
    public Object staticFieldBase(Field f) { return null; }
    public boolean shouldBeInitialized(Class<?> c) { return false; }
    public void ensureClassInitialized(Class<?> c) {}

    // Object accessors with (Object;J) signature
    public Object getObject(Object o, long offset) { return null; }
    public void putObject(Object o, long offset, Object x) {}
    public Object getObjectVolatile(Object o, long offset) { return null; }
    public void putObjectVolatile(Object o, long offset, Object x) {}
    public void putOrderedObject(Object o, long offset, Object x) {}

    // Boolean accessors
    public boolean getBoolean(Object o, long offset) { return false; }
    public void putBoolean(Object o, long offset, boolean x) {}
    public boolean getBooleanVolatile(Object o, long offset) { return false; }
    public void putBooleanVolatile(Object o, long offset, boolean x) {}

    // Byte accessors
    public byte getByte(Object o, long offset) { return 0; }
    public void putByte(Object o, long offset, byte x) {}
    public byte getByteVolatile(Object o, long offset) { return 0; }
    public void putByteVolatile(Object o, long offset, byte x) {}

    // Byte accessors (J) - absolute address
    public byte getByte(long address) { return 0; }
    public void putByte(long address, byte x) {}

    // Short accessors
    public short getShort(Object o, long offset) { return 0; }
    public void putShort(Object o, long offset, short x) {}
    public short getShortVolatile(Object o, long offset) { return 0; }
    public void putShortVolatile(Object o, long offset, short x) {}

    // Short accessors (J) - absolute address
    public short getShort(long address) { return 0; }
    public void putShort(long address, short x) {}

    // Char accessors
    public char getChar(Object o, long offset) { return 0; }
    public void putChar(Object o, long offset, char x) {}
    public char getCharVolatile(Object o, long offset) { return 0; }
    public void putCharVolatile(Object o, long offset, char x) {}

    // Char accessors (J) - absolute address
    public char getChar(long address) { return 0; }
    public void putChar(long address, char x) {}

    // Int accessors
    public int getInt(Object o, long offset) { return 0; }
    public void putInt(Object o, long offset, int x) {}
    public void putOrderedInt(Object o, long offset, int x) {}
    public int getIntVolatile(Object o, long offset) { return 0; }
    public void putIntVolatile(Object o, long offset, int x) {}

    // Int accessors (J) - absolute address
    public int getInt(long address) { return 0; }
    public void putInt(long address, int x) {}

    // Long accessors
    public long getLong(Object o, long offset) { return 0L; }
    public void putLong(Object o, long offset, long x) {}
    public void putOrderedLong(Object o, long offset, long x) {}
    public long getLongVolatile(Object o, long offset) { return 0L; }
    public void putLongVolatile(Object o, long offset, long x) {}

    // Long accessors (J) - absolute address
    public long getLong(long address) { return 0L; }
    public void putLong(long address, long x) {}

    // Float accessors
    public float getFloat(Object o, long offset) { return 0F; }
    public void putFloat(Object o, long offset, float x) {}
    public float getFloatVolatile(Object o, long offset) { return 0F; }
    public void putFloatVolatile(Object o, long offset, float x) {}

    // Float accessors (J) - absolute address
    public float getFloat(long address) { return 0F; }
    public void putFloat(long address, float x) {}

    // Double accessors
    public double getDouble(Object o, long offset) { return 0D; }
    public void putDouble(Object o, long offset, double x) {}
    public double getDoubleVolatile(Object o, long offset) { return 0D; }
    public void putDoubleVolatile(Object o, long offset, double x) {}

    // Double accessors (J) - absolute address
    public double getDouble(long address) { return 0D; }
    public void putDouble(long address, double x) {}

    public long getAddress(long address) { return 0L; }
    public void putAddress(long address, long x) {}

    // CAS operations
    public boolean compareAndSwapObject(Object o, long offset, Object expected, Object x) { return false; }
    public boolean compareAndSwapInt(Object o, long offset, int expected, int x) { return false; }
    public boolean compareAndSwapLong(Object o, long offset, long expected, long x) { return false; }

    // Atomic operations
    public int getAndAddInt(Object o, long offset, int delta) { return 0; }
    public long getAndAddLong(Object o, long offset, long delta) { return 0L; }
    public int getAndSetInt(Object o, long offset, int x) { return 0; }
    public long getAndSetLong(Object o, long offset, long x) { return 0L; }
    public Object getAndSetObject(Object o, long offset, Object x) { return null; }

    // Array access
    public int arrayBaseOffset(Class<?> arrayClass) { return 16; }
    public int arrayIndexScale(Class<?> arrayClass) { return 1; }
    public int addressSize() { return 8; }
    public int pageSize() { return 4096; }

    // Memory operations
    public long allocateMemory(long bytes) { throw new UnsupportedOperationException(); }
    public long reallocateMemory(long address, long bytes) { throw new UnsupportedOperationException(); }
    public void freeMemory(long address) {}
    public void setMemory(long address, long bytes, byte value) {}
    public void copyMemory(long srcAddress, long destAddress, long bytes) {}
    public void copyMemory(Object srcBase, long srcOffset, Object destBase, long destOffset, long bytes) {}

    // Object creation
    public Object allocateInstance(Class<?> cls) throws InstantiationException {
        throw new InstantiationException("allocateInstance not supported in browser");
    }

    // Exception throwing
    public void throwException(Throwable e) {
        if (e instanceof RuntimeException) throw (RuntimeException) e;
        if (e instanceof Error) throw (Error) e;
        throw new RuntimeException(e);
    }

    // Monitor operations
    public void monitorEnter(Object o) {}
    public void monitorExit(Object o) {}
    public boolean tryMonitorEnter(Object o) { return true; }

    // Fence operations
    public void loadFence() {}
    public void storeFence() {}
    public void fullFence() {}

    // Thread parking
    public void park(boolean isAbsolute, long time) {}
    public void unpark(Object thread) {}
}
