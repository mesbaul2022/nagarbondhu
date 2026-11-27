package com.nagarbondhu.pattern.proxy;

import com.nagarbondhu.dto.request.CreateComplaintRequest;
import com.nagarbondhu.dto.request.UpdateComplaintRequest;
import com.nagarbondhu.dto.response.ComplaintResponse;
import com.nagarbondhu.dto.response.TrackingResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * <h2>GoF Design Pattern: Proxy (Structural)</h2>
 *
 * <p><strong>GoF Role — Subject:</strong> Defines the common contract implemented by both the
 * RealSubject ({@link com.nagarbondhu.service.ComplaintServiceImpl}) and the Protection Proxy
 * ({@link ComplaintAccessProxy}) so clients interact with protected complaint operations through
 * a uniform interface.</p>
 */
public interface ComplaintOperations {

    ComplaintResponse createComplaint(CreateComplaintRequest request, String userEmail);

    List<ComplaintResponse> getMyComplaints(String userEmail);

    ComplaintResponse getComplaintById(Long id, String userEmail);

    ComplaintResponse updateComplaint(Long id, UpdateComplaintRequest request, String userEmail);

    void deleteComplaint(Long id, String userEmail);

    ComplaintResponse uploadEvidenceImage(Long id, MultipartFile file, String userEmail);

    TrackingResponse trackByTrackingNo(String trackingNo);
}
