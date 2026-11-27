package com.nagarbondhu.service;

import com.nagarbondhu.dto.request.CreateComplaintRequest;
import com.nagarbondhu.dto.request.UpdateComplaintRequest;
import com.nagarbondhu.dto.response.ComplaintHistoryResponse;
import com.nagarbondhu.dto.response.ComplaintImageResponse;
import com.nagarbondhu.dto.response.ComplaintResponse;
import com.nagarbondhu.dto.response.TrackingResponse;
import com.nagarbondhu.entity.Complaint;
import com.nagarbondhu.entity.ComplaintHistory;
import com.nagarbondhu.entity.ComplaintImage;
import com.nagarbondhu.entity.MaintenanceTeam;
import com.nagarbondhu.entity.User;
import com.nagarbondhu.entity.enums.ComplaintStatus;
import com.nagarbondhu.entity.enums.ImageType;
import com.nagarbondhu.exception.ResourceNotFoundException;
import com.nagarbondhu.pattern.factory.ComplaintFactory;
import com.nagarbondhu.pattern.flyweight.CategoryMetadata;
import com.nagarbondhu.pattern.flyweight.CategoryMetadataFlyweightFactory;
import com.nagarbondhu.pattern.proxy.ComplaintOperations;
import com.nagarbondhu.repository.ComplaintHistoryRepository;
import com.nagarbondhu.repository.ComplaintImageRepository;
import com.nagarbondhu.repository.ComplaintRepository;
import com.nagarbondhu.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * <h2>GoF Design Pattern: Proxy (Structural)</h2>
 *
 * <p><strong>GoF Role — RealSubject:</strong> Executes the core complaint persistence,
 * file storage, history auditing, and DTO mapping operations behind
 * {@link com.nagarbondhu.pattern.proxy.ComplaintAccessProxy}.</p>
 */
@Service("complaintServiceImpl")
@RequiredArgsConstructor
@Slf4j
public class ComplaintServiceImpl implements ComplaintOperations {

    private static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024L * 1024L;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp"
    );

    private final ComplaintRepository complaintRepository;
    private final ComplaintHistoryRepository complaintHistoryRepository;
    private final ComplaintImageRepository complaintImageRepository;
    private final UserRepository userRepository;
    private final ComplaintFactory complaintFactory;

    @Value("${app.upload-dir:./uploads}")
    private String uploadDir = "./uploads";

    @Override
    @Transactional
    public ComplaintResponse createComplaint(CreateComplaintRequest request, String userEmail) {
        User reporter = findUserByEmail(userEmail);
        Complaint complaint = complaintFactory.createComplaint(request, reporter);
        Complaint savedComplaint = complaintRepository.save(complaint);

        ComplaintHistory initialHistory = ComplaintHistory.builder()
                .complaint(savedComplaint)
                .updatedBy(reporter)
                .oldStatus(null)
                .newStatus(ComplaintStatus.PENDING)
                .remarks("Complaint submitted by resident")
                .build();
        ComplaintHistory savedHistory = complaintHistoryRepository.save(initialHistory);
        ComplaintHistory effectiveHistory = savedHistory != null ? savedHistory : initialHistory;
        if (savedComplaint.getHistory() != null) {
            savedComplaint.getHistory().add(effectiveHistory);
        }

        log.info("Created complaint {} for reporter {}", savedComplaint.getTrackingNo(), reporter.getEmail());
        return toResponse(savedComplaint);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplaintResponse> getMyComplaints(String userEmail) {
        User reporter = findUserByEmail(userEmail);
        return complaintRepository.findByReporterIdOrderByCreatedAtDesc(reporter.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ComplaintResponse getComplaintById(Long id, String userEmail) {
        Complaint complaint = findComplaintById(id);
        return toResponse(complaint);
    }

    @Override
    @Transactional
    public ComplaintResponse updateComplaint(Long id, UpdateComplaintRequest request, String userEmail) {
        Complaint complaint = findComplaintById(id);

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            complaint.setTitle(request.getTitle().trim());
        }
        if (request.getDescription() != null && !request.getDescription().isBlank()) {
            complaint.setDescription(request.getDescription().trim());
        }
        if (request.getCategory() != null && request.getCategory() != complaint.getCategory()) {
            complaint.setCategory(request.getCategory());
            if (request.getPriority() == null) {
                CategoryMetadata metadata = CategoryMetadataFlyweightFactory.getMetadata(request.getCategory());
                complaint.setPriority(metadata.getDefaultPriority());
            }
        }
        if (request.getPriority() != null) {
            complaint.setPriority(request.getPriority());
        }
        if (request.getLocation() != null && !request.getLocation().isBlank()) {
            complaint.setLocation(request.getLocation().trim());
        }
        if (request.getWardNo() != null) {
            complaint.setWardNo(request.getWardNo().isBlank() ? null : request.getWardNo().trim());
        }

        Complaint updated = complaintRepository.save(complaint);
        log.info("Updated complaint {} (id={})", updated.getTrackingNo(), updated.getId());
        return toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteComplaint(Long id, String userEmail) {
        Complaint complaint = findComplaintById(id);
        deleteStoredFilesQuietly(complaint.getImages());
        complaintRepository.delete(complaint);
        log.info("Deleted complaint {} (id={})", complaint.getTrackingNo(), complaint.getId());
    }

    @Override
    @Transactional
    public ComplaintResponse uploadEvidenceImage(Long id, MultipartFile file, String userEmail) {
        Complaint complaint = findComplaintById(id);
        String storedFilename = storeValidatedImage(file);

        ComplaintImage image = ComplaintImage.builder()
                .complaint(complaint)
                .imageUrl("/uploads/" + storedFilename)
                .imageType(ImageType.EVIDENCE)
                .build();
        ComplaintImage savedImage = complaintImageRepository.save(image);
        ComplaintImage effectiveImage = savedImage != null ? savedImage : image;
        if (complaint.getImages() != null) {
            complaint.getImages().add(effectiveImage);
        }

        log.info("Uploaded evidence image {} for complaint {}", storedFilename, complaint.getTrackingNo());
        return toResponse(complaint);
    }

    @Override
    @Transactional(readOnly = true)
    public TrackingResponse trackByTrackingNo(String trackingNo) {
        if (trackingNo == null || trackingNo.isBlank()) {
            throw new IllegalArgumentException("Tracking number must not be blank");
        }
        String normalizedTrackingNo = trackingNo.trim();
        Complaint complaint = complaintRepository.findByTrackingNo(normalizedTrackingNo)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint", "trackingNo", normalizedTrackingNo));

        return toTrackingResponse(complaint);
    }

    public ComplaintResponse toResponse(Complaint complaint) {
        CategoryMetadata metadata = complaint.getCategory() != null
                ? CategoryMetadataFlyweightFactory.getMetadata(complaint.getCategory())
                : null;
        User reporter = complaint.getReporter();
        MaintenanceTeam team = complaint.getAssignedTeam();

        return ComplaintResponse.builder()
                .id(complaint.getId())
                .trackingNo(complaint.getTrackingNo())
                .title(complaint.getTitle())
                .description(complaint.getDescription())
                .category(complaint.getCategory())
                .categoryDisplayName(metadata != null ? metadata.getDisplayName() : null)
                .slaHours(metadata != null ? metadata.getSlaHours() : null)
                .badgeColor(metadata != null ? metadata.getBadgeColor() : null)
                .location(complaint.getLocation())
                .wardNo(complaint.getWardNo())
                .status(complaint.getStatus())
                .priority(complaint.getPriority())
                .reporterId(reporter != null ? reporter.getId() : null)
                .reporterName(reporter != null ? reporter.getName() : null)
                .reporterEmail(reporter != null ? reporter.getEmail() : null)
                .assignedTeamId(team != null ? team.getId() : null)
                .assignedTeamName(team != null ? team.getTeamName() : null)
                .assignedTeamZone(team != null ? team.getZone() : null)
                .assignedTeamPhone(team != null ? team.getContactPhone() : null)
                .images(mapImages(resolveImages(complaint)))
                .history(mapHistory(resolveHistory(complaint)))
                .createdAt(complaint.getCreatedAt())
                .updatedAt(complaint.getUpdatedAt())
                .resolvedAt(complaint.getResolvedAt())
                .build();
    }

    private TrackingResponse toTrackingResponse(Complaint complaint) {
        CategoryMetadata metadata = complaint.getCategory() != null
                ? CategoryMetadataFlyweightFactory.getMetadata(complaint.getCategory())
                : null;
        MaintenanceTeam team = complaint.getAssignedTeam();

        return TrackingResponse.builder()
                .trackingNo(complaint.getTrackingNo())
                .title(complaint.getTitle())
                .description(complaint.getDescription())
                .category(complaint.getCategory())
                .categoryDisplayName(metadata != null ? metadata.getDisplayName() : null)
                .slaHours(metadata != null ? metadata.getSlaHours() : null)
                .badgeColor(metadata != null ? metadata.getBadgeColor() : null)
                .location(complaint.getLocation())
                .wardNo(complaint.getWardNo())
                .status(complaint.getStatus())
                .priority(complaint.getPriority())
                .assignedTeamName(team != null ? team.getTeamName() : null)
                .assignedTeamZone(team != null ? team.getZone() : null)
                .assignedTeamContactPhone(team != null ? team.getContactPhone() : null)
                .images(mapImages(resolveImages(complaint)))
                .history(mapHistory(resolveHistory(complaint)))
                .createdAt(complaint.getCreatedAt())
                .updatedAt(complaint.getUpdatedAt())
                .resolvedAt(complaint.getResolvedAt())
                .build();
    }

    private List<ComplaintImage> resolveImages(Complaint complaint) {
        if (complaint.getImages() != null && !complaint.getImages().isEmpty()) {
            return complaint.getImages();
        }
        if (complaint.getId() != null) {
            List<ComplaintImage> loaded = complaintImageRepository.findByComplaintId(complaint.getId());
            if (loaded != null) {
                return loaded;
            }
        }
        return Collections.emptyList();
    }

    private List<ComplaintHistory> resolveHistory(Complaint complaint) {
        if (complaint.getHistory() != null && !complaint.getHistory().isEmpty()) {
            return complaint.getHistory();
        }
        if (complaint.getId() != null) {
            List<ComplaintHistory> loaded = complaintHistoryRepository.findByComplaintIdOrderByChangedAtAsc(complaint.getId());
            if (loaded != null) {
                return loaded;
            }
        }
        return Collections.emptyList();
    }

    private List<ComplaintImageResponse> mapImages(List<ComplaintImage> images) {
        if (images == null) {
            return Collections.emptyList();
        }
        return images.stream()
                .filter(Objects::nonNull)
                .map(img -> ComplaintImageResponse.builder()
                        .id(img.getId())
                        .imageUrl(img.getImageUrl())
                        .imageType(img.getImageType())
                        .uploadedAt(img.getUploadedAt())
                        .build())
                .toList();
    }

    private List<ComplaintHistoryResponse> mapHistory(List<ComplaintHistory> historyEntries) {
        if (historyEntries == null) {
            return Collections.emptyList();
        }
        return historyEntries.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(
                        ComplaintHistory::getChangedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(entry -> ComplaintHistoryResponse.builder()
                        .id(entry.getId())
                        .oldStatus(entry.getOldStatus())
                        .newStatus(entry.getNewStatus())
                        .remarks(entry.getRemarks())
                        .updatedByName(entry.getUpdatedBy() != null ? entry.getUpdatedBy().getName() : null)
                        .changedAt(entry.getChangedAt())
                        .build())
                .toList();
    }

    private String storeValidatedImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Image file must not be empty");
        }
        if (file.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new IllegalArgumentException("Image file size must not exceed 5MB");
        }

        String originalFilename = StringUtils.cleanPath(Objects.requireNonNullElse(file.getOriginalFilename(), ""));
        if (originalFilename.contains("..")) {
            throw new IllegalArgumentException("Invalid file path sequence in filename");
        }

        String extension = extractExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Only .jpg, .jpeg, .png, and .webp image files are allowed");
        }

        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank()) {
            String normalizedType = contentType.toLowerCase(Locale.ROOT);
            if (!"application/octet-stream".equals(normalizedType) && !ALLOWED_CONTENT_TYPES.contains(normalizedType)) {
                throw new IllegalArgumentException("Unsupported image content type: " + contentType);
            }
        }

        String sanitizedFilename = UUID.randomUUID() + "." + extension;
        Path uploadRoot = resolveUploadRoot();
        try {
            Files.createDirectories(uploadRoot);
            Path destination = uploadRoot.resolve(sanitizedFilename).normalize();
            if (!destination.startsWith(uploadRoot)) {
                throw new IllegalArgumentException("Invalid upload destination path");
            }
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
            }
            return sanitizedFilename;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to store uploaded evidence image", ex);
        }
    }

    private String extractExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT).trim();
    }

    private Path resolveUploadRoot() {
        String configuredDir = (uploadDir == null || uploadDir.isBlank()) ? "./uploads" : uploadDir;
        return Paths.get(configuredDir).toAbsolutePath().normalize();
    }

    private void deleteStoredFilesQuietly(List<ComplaintImage> images) {
        if (images == null || images.isEmpty()) {
            return;
        }
        Path uploadRoot = resolveUploadRoot();
        for (ComplaintImage image : images) {
            if (image == null || image.getImageUrl() == null || !image.getImageUrl().startsWith("/uploads/")) {
                continue;
            }
            String filename = image.getImageUrl().substring("/uploads/".length());
            if (filename.isBlank() || filename.contains("..")) {
                continue;
            }
            Path filePath = uploadRoot.resolve(filename).normalize();
            if (filePath.startsWith(uploadRoot)) {
                try {
                    Files.deleteIfExists(filePath);
                } catch (IOException ex) {
                    log.warn("Could not delete uploaded image file {}: {}", filePath, ex.getMessage());
                }
            }
        }
    }

    private Complaint findComplaintById(Long id) {
        return complaintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint", "id", id));
    }

    private User findUserByEmail(String userEmail) {
        if (userEmail == null || userEmail.isBlank()) {
            throw new IllegalArgumentException("Authenticated user email is required");
        }
        String normalizedEmail = userEmail.trim().toLowerCase(Locale.ROOT);
        return userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", normalizedEmail));
    }
}
