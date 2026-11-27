package com.nagarbondhu.pattern.singleton;

import java.time.Year;
import java.util.concurrent.atomic.AtomicLong;

/**
 * <h2>GoF Design Pattern: Singleton (Creational)</h2>
 *
 * <p><strong>GoF Role — Singleton:</strong> Ensures that only one instance of the complaint
 * tracking number generator exists within the JVM and provides a global point of access via
 * {@link #getInstance()}.</p>
 *
 * <p>Uses thread-safe double-checked locking with a {@code volatile} instance reference and an
 * {@link AtomicLong} sequence counter so concurrent complaint submissions receive strictly
 * unique, monotonic tracking identifiers in {@code NB-<year>-<5-digit-sequence>} format
 * (e.g., {@code NB-2026-00001}). Call {@link #syncCounter(long)} at application startup to
 * synchronize the counter with the highest sequence persisted in the database.</p>
 */
public final class TrackingIdGenerator {

    private static volatile TrackingIdGenerator instance;
    private final AtomicLong counter = new AtomicLong(0);

    private TrackingIdGenerator() {
    }

    /**
     * Returns the single global {@link TrackingIdGenerator} instance using thread-safe
     * double-checked locking.
     *
     * @return the singleton {@link TrackingIdGenerator} instance
     */
    public static TrackingIdGenerator getInstance() {
        TrackingIdGenerator localRef = instance;
        if (localRef == null) {
            synchronized (TrackingIdGenerator.class) {
                localRef = instance;
                if (localRef == null) {
                    localRef = new TrackingIdGenerator();
                    instance = localRef;
                }
            }
        }
        return localRef;
    }

    /**
     * Generates the next sequential complaint tracking number in {@code NB-<year>-%05d} format.
     *
     * @return formatted tracking number such as {@code NB-2026-00001}
     */
    public String nextTrackingNumber() {
        int year = Year.now().getValue();
        long sequence = counter.incrementAndGet();
        return String.format("NB-%d-%05d", year, sequence);
    }

    /**
     * Synchronizes the internal counter with the maximum sequence number already stored
     * in PostgreSQL so application restarts never produce duplicate tracking numbers.
     *
     * @param maxExisting highest existing numeric sequence from persisted complaints
     */
    public void syncCounter(long maxExisting) {
        if (maxExisting <= 0) {
            return;
        }
        counter.updateAndGet(current -> Math.max(current, maxExisting));
    }

    /**
     * Returns the current counter value without incrementing it.
     *
     * @return current sequence count
     */
    public long getCurrentCount() {
        return counter.get();
    }
}
