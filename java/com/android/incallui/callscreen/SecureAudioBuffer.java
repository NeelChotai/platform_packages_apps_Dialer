package com.android.incallui.callscreen;

import com.android.dialer.common.LogUtil;

import java.io.Closeable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Secure buffer for audio data with guaranteed memory zeroing.
 *
 * <p>Security properties:
 * <ul>
 *   <li>Uses direct ByteBuffer (native memory, not Java heap)</li>
 *   <li>Memory zeroed on close() via Unsafe.setMemory() when available</li>
 *   <li>Falls back to ByteBuffer.put() loop if Unsafe unavailable</li>
 *   <li>Maximum size enforced at construction</li>
 *   <li>Thread-safe via ReentrantLock</li>
 *   <li>Cannot be cloned or serialized</li>
 * </ul>
 */
public final class SecureAudioBuffer implements Closeable {

    private static final String TAG = "SecureAudioBuffer";

    /** Maximum allowed buffer size: 10MB */
    public static final int MAX_ALLOWED_SIZE = 10 * 1024 * 1024;

    // Unsafe access via reflection (to avoid direct dependency on sun.misc)
    private static final Object UNSAFE;
    private static final Method SET_MEMORY_METHOD;
    private static final Field ADDRESS_FIELD;
    private static final boolean UNSAFE_AVAILABLE;

    static {
        Object tempUnsafe = null;
        Method tempSetMemory = null;
        Field tempAddressField = null;
        boolean available = false;

        try {
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            Field f = unsafeClass.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            tempUnsafe = f.get(null);

            tempSetMemory = unsafeClass.getMethod("setMemory", long.class, long.class, byte.class);

            tempAddressField = java.nio.Buffer.class.getDeclaredField("address");
            tempAddressField.setAccessible(true);

            available = true;
            LogUtil.i(TAG, "Unsafe memory zeroing available");
        } catch (Exception e) {
            LogUtil.w(TAG, "Unsafe not available, using fallback zeroing: %s", e.getMessage());
        }

        UNSAFE = tempUnsafe;
        SET_MEMORY_METHOD = tempSetMemory;
        ADDRESS_FIELD = tempAddressField;
        UNSAFE_AVAILABLE = available;
    }

    private final ByteBuffer buffer;
    private final int capacity;
    private final ReentrantLock lock = new ReentrantLock();
    private volatile boolean closed = false;

    /**
     * Creates a secure audio buffer with the specified capacity.
     *
     * @param capacity The buffer capacity in bytes (must be 1 to MAX_ALLOWED_SIZE)
     * @throws IllegalArgumentException if capacity is invalid
     */
    public SecureAudioBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive: " + capacity);
        }
        if (capacity > MAX_ALLOWED_SIZE) {
            throw new IllegalArgumentException("Capacity exceeds maximum: " + capacity +
                    " > " + MAX_ALLOWED_SIZE);
        }

        this.capacity = capacity;
        this.buffer = ByteBuffer.allocateDirect(capacity);
    }

    /**
     * Writes data to the buffer.
     *
     * @param data The source byte array
     * @param offset The offset in the source array
     * @param length The number of bytes to write
     * @return The number of bytes actually written (may be less than length if buffer fills)
     * @throws IllegalStateException if buffer is closed
     */
    public int write(byte[] data, int offset, int length) {
        checkNotClosed();

        lock.lock();
        try {
            int remaining = buffer.remaining();
            int toWrite = Math.min(length, remaining);

            if (toWrite > 0) {
                buffer.put(data, offset, toWrite);
            }

            return toWrite;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns a copy of the buffer contents.
     *
     * @return A new byte array containing the buffer data
     * @throws IllegalStateException if buffer is closed
     */
    public byte[] toByteArray() {
        checkNotClosed();

        lock.lock();
        try {
            int size = buffer.position();
            byte[] result = new byte[size];

            // Read from beginning without modifying position
            buffer.rewind();
            buffer.get(result);
            buffer.position(size);

            return result;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns the current size (number of bytes written).
     *
     * @throws IllegalStateException if buffer is closed
     */
    public int size() {
        checkNotClosed();

        lock.lock();
        try {
            return buffer.position();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns the buffer capacity.
     */
    public int capacity() {
        return capacity;
    }

    /**
     * Returns true if the buffer has been closed.
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * Zeros the buffer memory and resets position, allowing reuse.
     *
     * @throws IllegalStateException if buffer is closed
     */
    public void clear() {
        checkNotClosed();

        lock.lock();
        try {
            zeroMemory();
            buffer.clear();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Zeros the buffer memory and marks as closed.
     * Safe to call multiple times.
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }

        lock.lock();
        try {
            if (!closed) {
                zeroMemory();
                closed = true;
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Zeros all memory in the buffer using Unsafe if available.
     */
    private void zeroMemory() {
        if (UNSAFE_AVAILABLE) {
            try {
                long address = ADDRESS_FIELD.getLong(buffer);
                SET_MEMORY_METHOD.invoke(UNSAFE, address, (long) capacity, (byte) 0);
                return;
            } catch (Exception e) {
                LogUtil.w(TAG, "Unsafe zeroing failed, using fallback: %s", e.getMessage());
            }
        }
        fallbackZero();
    }

    /**
     * Fallback method to zero memory using ByteBuffer operations.
     */
    private void fallbackZero() {
        buffer.clear();
        byte[] zeros = new byte[Math.min(8192, capacity)];
        while (buffer.hasRemaining()) {
            int toWrite = Math.min(zeros.length, buffer.remaining());
            buffer.put(zeros, 0, toWrite);
        }
        buffer.clear();
    }

    private void checkNotClosed() {
        if (closed) {
            throw new IllegalStateException("Buffer is closed");
        }
    }

    /**
     * Prevents cloning for security.
     */
    @Override
    protected Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException("SecureAudioBuffer cannot be cloned");
    }

    /**
     * Ensures memory is zeroed on garbage collection.
     */
    @Override
    @SuppressWarnings("deprecation")
    protected void finalize() throws Throwable {
        try {
            close();
        } finally {
            super.finalize();
        }
    }
}
