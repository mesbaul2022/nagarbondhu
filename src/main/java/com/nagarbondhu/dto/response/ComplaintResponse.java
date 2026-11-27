package com.nagarbondhu.dto.response;

import com.nagarbondhu.entity.enums.ComplaintCategory;
import com.nagarbondhu.entity.enums.ComplaintPriority;
import com.nagarbondhu.entity.enums.ComplaintStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintResponse {

    private Long id;
    private String trackingNo;
    private String title;
    private String description;
    private ComplaintCategory category;
    private String categoryDisplayName;
    private Integer slaHours;
    private String badgeColor;
    private String location;
    private String wardNo;
    private ComplaintStatus status;
    private ComplaintPriority priority;
    private Long reporterId;
    private String reporterName;
    private String reporterEmail;
    private Long assignedTeamId;
    private String assignedTeamName;
    private String assignedTeamZone;
    private String assignedTeamPhone;
    private List<ComplaintImageResponse> images;
    private List<ComplaintHistoryResponse> history;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;
}
