package com.nagarbondhu.pattern.flyweight;

import com.nagarbondhu.entity.enums.ComplaintCategory;
import com.nagarbondhu.entity.enums.ComplaintPriority;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * <h2>GoF Design Pattern: Flyweight (Structural)</h2>
 *
 * <p><strong>GoF Role — ConcreteFlyweight:</strong> Represents the immutable, shareable intrinsic
 * metadata associated with a {@link ComplaintCategory}, including its human-readable display name,
 * default SLA priority, target SLA resolution hours, and UI badge color.</p>
 *
 * <p>Instances are created and cached once per category by
 * {@link CategoryMetadataFlyweightFactory} and shared across all complaints of that category
 * to eliminate redundant object allocations.</p>
 */
@Getter
@EqualsAndHashCode
@ToString
public final class CategoryMetadata {

    private final ComplaintCategory category;
    private final String displayName;
    private final ComplaintPriority defaultPriority;
    private final int slaHours;
    private final String badgeColor;

    public CategoryMetadata(
            ComplaintCategory category,
            String displayName,
            ComplaintPriority defaultPriority,
            int slaHours,
            String badgeColor) {
        this.category = category;
        this.displayName = displayName;
        this.defaultPriority = defaultPriority;
        this.slaHours = slaHours;
        this.badgeColor = badgeColor;
    }
}
