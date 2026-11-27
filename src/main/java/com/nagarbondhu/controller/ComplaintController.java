package com.nagarbondhu.controller;

import com.nagarbondhu.dto.request.CreateComplaintRequest;
import com.nagarbondhu.dto.request.UpdateComplaintRequest;
import com.nagarbondhu.dto.response.ComplaintResponse;
import com.nagarbondhu.dto.response.TrackingResponse;
import com.nagarbondhu.pattern.proxy.ComplaintOperations;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/complaints")
public class ComplaintController {

    private final ComplaintOperations complaintOperations;

    public ComplaintController(@Qualifier("complaintAccessProxy") ComplaintOperations complaintOperations) {
        this.complaintOperations = complaintOperations;
    }

    @PostMapping
    @PreAuthorize("hasRole('BUYER')")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ComplaintResponse> createComplaint(
            @Valid @RequestBody CreateComplaintRequest request,
            Authentication authentication) {
        ComplaintResponse response = complaintOperations.createComplaint(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<List<ComplaintResponse>> getMyComplaints(Authentication authentication) {
        return ResponseEntity.ok(complaintOperations.getMyComplaints(authentication.getName()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('BUYER', 'SELLER', 'ADMIN')")
    public ResponseEntity<ComplaintResponse> getComplaintById(
            @PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(complaintOperations.getComplaintById(id, authentication.getName()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<ComplaintResponse> updateComplaint(
            @PathVariable Long id,
            @Valid @RequestBody UpdateComplaintRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(complaintOperations.updateComplaint(id, request, authentication.getName()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('BUYER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deleteComplaint(
            @PathVariable Long id,
            Authentication authentication) {
        complaintOperations.deleteComplaint(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('BUYER')")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ComplaintResponse> uploadEvidenceImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        ComplaintResponse response = complaintOperations.uploadEvidenceImage(id, file, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/track/{trackingNo}")
    public ResponseEntity<TrackingResponse> trackComplaint(@PathVariable String trackingNo) {
        return ResponseEntity.ok(complaintOperations.trackByTrackingNo(trackingNo));
    }
}
