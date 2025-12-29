package com.android.incallui.callscreen;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tests for {@link SecureAudioBuffer}.
 *
 * <p>Verifies capacity limits, thread-safety, memory zeroing, and close behavior.
 */
class SecureAudioBufferTest {

    @Test
    @DisplayName("Constructor throws for negative capacity")
    void constructor_throwsForNegativeCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new SecureAudioBuffer(-1));
    }

    @Test
    @DisplayName("Constructor throws for zero capacity")
    void constructor_throwsForZeroCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new SecureAudioBuffer(0));
    }

    @Test
    @DisplayName("Constructor throws for excessive capacity")
    void constructor_throwsForExcessiveCapacity() {
        assertThrows(IllegalArgumentException.class,
                () -> new SecureAudioBuffer(SecureAudioBuffer.MAX_ALLOWED_SIZE + 1));
    }

    @Test
    @DisplayName("Constructor allows max capacity")
    void constructor_allowsMaxCapacity() {
        try (SecureAudioBuffer buffer = new SecureAudioBuffer(SecureAudioBuffer.MAX_ALLOWED_SIZE)) {
            assertEquals(SecureAudioBuffer.MAX_ALLOWED_SIZE, buffer.capacity());
        }
    }

    @Test
    @DisplayName("Write respects capacity limits")
    void write_respectsCapacity() {
        try (SecureAudioBuffer buffer = new SecureAudioBuffer(10)) {
            byte[] data = new byte[15];
            Arrays.fill(data, (byte) 1);

            int written = buffer.write(data, 0, data.length);

            assertEquals(10, written);
            assertEquals(10, buffer.size());
        }
    }

    @Test
    @DisplayName("Write with offset works correctly")
    void write_withOffset() {
        try (SecureAudioBuffer buffer = new SecureAudioBuffer(100)) {
            byte[] data = new byte[]{0, 0, 1, 2, 3, 4, 5, 0, 0};

            int written = buffer.write(data, 2, 5);

            assertEquals(5, written);
            assertEquals(5, buffer.size());

            byte[] result = buffer.toByteArray();
            assertArrayEquals(new byte[]{1, 2, 3, 4, 5}, result);
        }
    }

    @Test
    @DisplayName("toByteArray returns copy, not reference")
    void toByteArray_returnsCopy() {
        try (SecureAudioBuffer buffer = new SecureAudioBuffer(10)) {
            buffer.write(new byte[]{1, 2, 3, 4, 5}, 0, 5);

            byte[] copy1 = buffer.toByteArray();
            byte[] copy2 = buffer.toByteArray();

            assertNotSame(copy1, copy2);
            assertArrayEquals(copy1, copy2);
        }
    }

    @Test
    @DisplayName("Close prevents further operations")
    void close_preventsFurtherOperations() {
        SecureAudioBuffer buffer = new SecureAudioBuffer(100);
        buffer.close();

        assertThrows(IllegalStateException.class, () -> buffer.write(new byte[10], 0, 10));
        assertThrows(IllegalStateException.class, buffer::toByteArray);
        assertThrows(IllegalStateException.class, buffer::size);
    }

    @Test
    @DisplayName("Close can be called multiple times")
    void close_canBeCalledMultipleTimes() {
        SecureAudioBuffer buffer = new SecureAudioBuffer(100);
        buffer.close();
        buffer.close(); // Should not throw
    }

    @Test
    @DisplayName("Clear zeros memory but allows reuse")
    void clear_zerosAndAllowsReuse() {
        try (SecureAudioBuffer buffer = new SecureAudioBuffer(100)) {
            buffer.write(new byte[]{1, 2, 3}, 0, 3);
            buffer.clear();

            assertEquals(0, buffer.size());

            // Can write again
            buffer.write(new byte[]{4, 5, 6}, 0, 3);
            assertEquals(3, buffer.size());

            byte[] result = buffer.toByteArray();
            assertArrayEquals(new byte[]{4, 5, 6}, result);
        }
    }

    @Test
    @DisplayName("Capacity returns correct value")
    void capacity_returnsCorrectValue() {
        try (SecureAudioBuffer buffer = new SecureAudioBuffer(1234)) {
            assertEquals(1234, buffer.capacity());
        }
    }

    @Test
    @DisplayName("Empty buffer returns empty array")
    void emptyBuffer_returnsEmptyArray() {
        try (SecureAudioBuffer buffer = new SecureAudioBuffer(100)) {
            byte[] result = buffer.toByteArray();
            assertEquals(0, result.length);
        }
    }

    @Test
    @DisplayName("Concurrent writes are thread-safe")
    void concurrentWrites_areThreadSafe() throws Exception {
        try (SecureAudioBuffer buffer = new SecureAudioBuffer(10000)) {
            ExecutorService executor = Executors.newFixedThreadPool(4);
            CountDownLatch latch = new CountDownLatch(100);
            AtomicInteger totalWritten = new AtomicInteger(0);

            for (int i = 0; i < 100; i++) {
                executor.submit(() -> {
                    byte[] data = new byte[100];
                    Arrays.fill(data, (byte) 1);
                    int written = buffer.write(data, 0, data.length);
                    totalWritten.addAndGet(written);
                    latch.countDown();
                });
            }

            assertTrue(latch.await(5, TimeUnit.SECONDS));
            executor.shutdown();

            assertEquals(10000, totalWritten.get());
            assertEquals(10000, buffer.size());
        }
    }

    @Test
    @DisplayName("isClosed returns correct state")
    void isClosed_returnsCorrectState() {
        SecureAudioBuffer buffer = new SecureAudioBuffer(100);
        assertFalse(buffer.isClosed());

        buffer.close();
        assertTrue(buffer.isClosed());
    }
}
