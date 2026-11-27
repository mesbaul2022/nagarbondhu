package com.nagarbondhu.dto.response;

import com.nagarbondhu.entity.enums.ImageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintImageResponse {

    private Long id;
    private String imageUrl;
    private ImageType imageType;
    private LocalDateTime uploadedAt;
}
