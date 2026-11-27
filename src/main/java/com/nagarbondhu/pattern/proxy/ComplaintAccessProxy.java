package com.nagarbondhu.pattern.proxy;

import com.nagarbondhu.dto.request.CreateComplaintRequest;
import com.nagarbondhu.dto.request.UpdateComplaintRequest;
import com.nagarbondhu.dto.response.ComplaintResponse;
import com.nagarbondhu.dto.response.TrackingResponse;
import com.nagarbondhu.entity.Complaint;
import com.nagarbondhu.entity.User;
import com.nagarbondhu.entity.enums.ComplaintStatus;
import com.nagarbondhu.entity.enums.RoleName;
import com.nagarbondhu.exception.InvalidOperationException;
import com.nagarbondhu.exception.ResourceNotFoundException;
import com.nagarbondhu.exception.UnauthorizedAccessException;
import com.nagarbondhu.repository.ComplaintRepository;
import com.nagarbondhu.repository.UserRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * <h2>GoF Design Pattern: Proxy (Structural)</h2>
 *
 * <p><strong>GoF Role — Protection Proxy:</strong> Intercepts sensitive complaint access and
 * mutation requests before delegating to the RealSubject
 * ({@link com.nagarbondhu.service.ComplaintServiceImpl}).</p>
 *
 * <p>Enforces resource-level authorization and lifecycle invariants:</p>
 * <ul>
 *   <li><strong>View ({@link #getComplaintById}):</strong> Allowed only for the complaint reporter,
 *       a member of the assigned maintenance team, or an {@code ADMIN} (throws
 *       {@link UnauthorizedAccessException} / HTTP 403 otherwise).</li>
 *   <li><strong>Update &amp; Delete ({@link #updateComplaint}, {@link #deleteComplaint}):</strong>
 *       Allowed only for the complaint reporter or an {@code ADMIN} (HTTP 403 otherwise), and
 *       only while the complaint status is {@link ComplaintStatus#PENDING} (throws
 *       {@link InvalidOperationException} / HTTP 409 otherwise).</li>
 *   <li><strong>Evidence Upload ({@link #uploadEvidenceImage}):</strong> Allowed only for the
 *       complaint reporter or an {@code ADMIN} (HTTP 403 otherwise).</li>
 * </ul>
 */
@Component("complaintAccessProxy")
public class ComplaintAccessProxy implements ComplaintOperations {

    private final ComplaintOperations target;
    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;

    public ComplaintAccessProxy(
            @Qualifier("complaintServiceImpl") ComplaintOperations target,
            ComplaintRepository complaintRepository,
            UserRepository userRepository) {
        this.target = target;
        this.complaintRepository = complaintRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public ComplaintResponse createComplaint(CreateComplaintRequest request, String userEmail) {
        return target.createComplaint(request, userEmail);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplaintResponse> getMyComplaints(String userEmail) {
        return target.getMyComplaints(userEmail);
    }

    @Override
    @Transactional(readOnly = true)
    public ComplaintResponse getComplaintById(Long id, String userEmail) {
        Complaint complaint = findComplaint(id);
        User actor = findActor(userEmail);

        boolean allowed = isReporter(complaint, actor)
                || isAdmin(actor)
                || isAssignedTeamMember(complaint, actor);

        if (!allowed) {
            throw new UnauthorizedAccessException("You are not authorized to view this complaint");
        }
        return target.getComplaintById(id, userEmail);
    }

    @Override
    @Transactional
    public ComplaintResponse updateComplaint(Long id, UpdateComplaintRequest request, String userEmail) {
        Complaint complaint = findComplaint(id);
        User actor = findActor(userEmail);

        if (!isReporter(complaint, actor) && !isAdmin(actor)) {
            throw new UnauthorizedAccessException("You are not authorized to modify this complaint");
        }
        if (complaint.getStatus() != ComplaintStatus.PENDING) {
            throw new InvalidOperationException(
                    "Only PENDING complaints can be edited (current status: " + complaint.getStatus() + ")");
        }

        return target.updateComplaint(id, request, userEmail);
    }

    @Override
    @Transactional
    public void deleteComplaint(Long id, String userEmail) {
        Complaint complaint = findComplaint(id);
        User actor = findActor(userEmail);

        if (!isReporter(complaint, actor) && !isAdmin(actor)) {
            throw new UnauthorizedAccessException("You are not authorized to delete this complaint");
        }
        if (complaint.getStatus() != ComplaintStatus.PENDING) {
            throw new InvalidOperationException(
                    "Only PENDING complaints can be deleted (current status: " + complaint.getStatus() + ")");
        }

        target.deleteComplaint(id, userEmail);
    }

    @Override
    @Transactional
    public ComplaintResponse uploadEvidenceImage(Long id, MultipartFile file, String userEmail) {
        Complaint complaint = findComplaint(id);
        User actor = findActor(userEmail);

        if (!isReporter(complaint, actor) && !isAdmin(actor)) {
            throw new UnauthorizedAccessException("You are not authorized to upload images for this complaint");
        }

        return target.uploadEvidenceImage(id, file, userEmail);
    }

    @Override
    @Transactional(readOnly = true)
    public TrackingResponse trackByTrackingNo(String trackingNo) {
        return target.trackByTrackingNo(trackingNo);
    }

    private Complaint findComplaint(Long id) {
        return complaintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint", "id", id));
    }

    private User findActor(String userEmail) {
        if (userEmail == null || userEmail.isBlank()) {
            throw new UnauthorizedAccessException("Authenticated user email is required");
        }
        String normalizedEmail = userEmail.trim().toLowerCase();
        return userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", normalizedEmail));
    }

    private boolean isReporter(Complaint complaint, User actor) {
        return isSameUser(complaint.getReporter(), actor);
    }

    private boolean isAdmin(User user) {
        return user.getRoles() != null && user.getRoles().stream()
                .anyMatch(role -> role.getName() == RoleName.ADMIN);
    }

    private boolean isAssignedTeamMember(Complaint complaint, User actor) {
        if (complaint.getAssignedTeam() == null || complaint.getAssignedTeam().getMembers() == null) {
            return false;
        }
        return complaint.getAssignedTeam().getMembers().stream()
                .anyMatch(member -> isSameUser(member, actor));
    }

    private boolean isSameUser(User first, User second) {
        if (first == null || second == null) {
            return false;
        }
        if (first.getId() != null && second.getId() != null) {
            return first.getId().equals(second.getId());
        }
        return first.getEmail() != null && first.getEmail().equalsIgnoreCase(second.getEmail());
    }
}
