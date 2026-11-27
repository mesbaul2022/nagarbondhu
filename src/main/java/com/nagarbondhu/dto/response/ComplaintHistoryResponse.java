package com.nagarbondhu.dto.response;

import com.nagarbondhu.entity.enums.ComplaintStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintHistoryResponse {

    private Long id;
    private ComplaintStatus oldStatus;
    private ComplaintStatus newStatus;
    private String remarks;
    private String updatedByName;
    private LocalDateTime changedAt;
}
