package com.nagarbondhu.pattern.flyweight;

import com.nagarbondhu.entity.enums.ComplaintCategory;
import com.nagarbondhu.entity.enums.ComplaintPriority;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <h2>GoF Design Pattern: Flyweight (Structural)</h2>
 *
 * <p><strong>GoF Role — FlyweightFactory:</strong> Creates and manages the shared pool of
 * immutable {@link CategoryMetadata} flyweight instances keyed by {@link ComplaintCategory}.</p>
 *
 * <p>Backed by a thread-safe {@link ConcurrentHashMap}, {@link #getMetadata(ComplaintCategory)}
 * guarantees that each category's metadata object is constructed at most once and reused
 * identically across complaint creation, DTO mapping, and dashboard rendering.</p>
 */
public final class CategoryMetadataFlyweightFactory {

    private static final ConcurrentHashMap<ComplaintCategory, CategoryMetadata> CACHE = new ConcurrentHashMap<>();

    private CategoryMetadataFlyweightFactory() {
    }

    /**
     * Returns the cached immutable {@link CategoryMetadata} flyweight for the given category,
     * creating and storing it atomically on first access.
     *
     * @param category municipal complaint category (must not be {@code null})
     * @return shared immutable {@link CategoryMetadata} instance
     */
    public static CategoryMetadata getMetadata(ComplaintCategory category) {
        if (category == null) {
            throw new IllegalArgumentException("ComplaintCategory must not be null");
        }
        return CACHE.computeIfAbsent(category, CategoryMetadataFlyweightFactory::createMetadata);
    }

    /**
     * Returns an unmodifiable view of all currently cached category metadata flyweights.
     *
     * @return unmodifiable map of cached flyweights
     */
    public static Map<ComplaintCategory, CategoryMetadata> getCachedMetadata() {
        return Collections.unmodifiableMap(CACHE);
    }

    /**
     * Returns the number of flyweight instances currently held in the cache.
     *
     * @return flyweight cache size
     */
    public static int getCacheSize() {
        return CACHE.size();
    }

    private static CategoryMetadata createMetadata(ComplaintCategory category) {
        return switch (category) {
            case WATERLOGGING -> new CategoryMetadata(
                    ComplaintCategory.WATERLOGGING,
                    "Waterlogging",
                    ComplaintPriority.HIGH,
                    12,
                    "#0d6efd"
            );
            case BLOCKED_DRAINAGE -> new CategoryMetadata(
                    ComplaintCategory.BLOCKED_DRAINAGE,
                    "Blocked Drainage",
                    ComplaintPriority.HIGH,
                    24,
                    "#0dcaf0"
            );
            case DAMAGED_ROAD -> new CategoryMetadata(
                    ComplaintCategory.DAMAGED_ROAD,
                    "Damaged Road",
                    ComplaintPriority.MEDIUM,
                    72,
                    "#fd7e14"
            );
            case BROKEN_STREETLIGHT -> new CategoryMetadata(
                    ComplaintCategory.BROKEN_STREETLIGHT,
                    "Broken Streetlight",
                    ComplaintPriority.MEDIUM,
                    48,
                    "#ffc107"
            );
            case WASTE_ACCUMULATION -> new CategoryMetadata(
                    ComplaintCategory.WASTE_ACCUMULATION,
                    "Waste Accumulation",
                    ComplaintPriority.MEDIUM,
                    24,
                    "#198754"
            );
            case WATER_SUPPLY -> new CategoryMetadata(
                    ComplaintCategory.WATER_SUPPLY,
                    "Water Supply Issue",
                    ComplaintPriority.CRITICAL,
                    8,
                    "#dc3545"
            );
            case OTHER -> new CategoryMetadata(
                    ComplaintCategory.OTHER,
                    "Other Municipal Issue",
                    ComplaintPriority.LOW,
                    96,
                    "#6c757d"
            );
        };
    }
}
