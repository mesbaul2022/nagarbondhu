package com.nagarbondhu.pattern.factory;

import com.nagarbondhu.dto.request.CreateComplaintRequest;
import com.nagarbondhu.entity.Complaint;
import com.nagarbondhu.entity.User;
import com.nagarbondhu.entity.enums.ComplaintPriority;
import com.nagarbondhu.entity.enums.ComplaintStatus;
import com.nagarbondhu.pattern.flyweight.CategoryMetadata;
import com.nagarbondhu.pattern.flyweight.CategoryMetadataFlyweightFactory;
import com.nagarbondhu.pattern.singleton.TrackingIdGenerator;
import org.springframework.stereotype.Component;

/**
 * <h2>GoF Design Pattern: Factory (Creational)</h2>
 *
 * <p><strong>GoF Role — Factory / Creator:</strong> Encapsulates the instantiation logic for
 * {@link Complaint} domain entities so callers do not manually assemble tracking IDs, category
 * defaults, or initial workflow state.</p>
 *
 * <p>Coordinates two other GoF patterns during construction:</p>
 * <ul>
 *   <li>{@link TrackingIdGenerator#getInstance()} (Singleton) to assign a unique tracking number</li>
 *   <li>{@link CategoryMetadataFlyweightFactory#getMetadata} (Flyweight) to resolve the default
 *       SLA priority when none is explicitly provided</li>
 * </ul>
 * <p>Every newly created complaint always starts in {@link ComplaintStatus#PENDING} status.</p>
 */
@Component
public class ComplaintFactory {

    /**
     * Constructs a new {@link Complaint} entity initialized with a generated tracking number,
     * flyweight-derived default priority (if omitted), and {@link ComplaintStatus#PENDING} status.
     *
     * @param request  validated complaint creation payload
     * @param reporter authenticated resident reporting the complaint
     * @return transient {@link Complaint} entity ready for persistence
     */
    public Complaint createComplaint(CreateComplaintRequest request, User reporter) {
        if (request == null) {
            throw new IllegalArgumentException("CreateComplaintRequest must not be null");
        }
        if (reporter == null) {
            throw new IllegalArgumentException("Reporter must not be null");
        }
        if (request.getCategory() == null) {
            throw new IllegalArgumentException("Complaint category must not be null");
        }

        CategoryMetadata metadata = CategoryMetadataFlyweightFactory.getMetadata(request.getCategory());
        String trackingNo = TrackingIdGenerator.getInstance().nextTrackingNumber();
        ComplaintPriority priority = request.getPriority() != null
                ? request.getPriority()
                : metadata.getDefaultPriority();

        String wardNo = request.getWardNo() != null && !request.getWardNo().isBlank()
                ? request.getWardNo().trim()
                : null;

        return Complaint.builder()
                .trackingNo(trackingNo)
                .title(request.getTitle().trim())
                .description(request.getDescription().trim())
                .category(request.getCategory())
                .location(request.getLocation().trim())
                .wardNo(wardNo)
                .status(ComplaintStatus.PENDING)
                .priority(priority)
                .reporter(reporter)
                .build();
    }
}
