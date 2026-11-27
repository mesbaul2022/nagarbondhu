package com.nagarbondhu.dto.request;

import com.nagarbondhu.entity.enums.ComplaintCategory;
import com.nagarbondhu.entity.enums.ComplaintPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateComplaintRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    @NotBlank(message = "Description is required")
    @Size(max = 4000, message = "Description must not exceed 4000 characters")
    private String description;

    @NotNull(message = "Category is required")
    private ComplaintCategory category;

    @NotBlank(message = "Location is required")
    @Size(max = 255, message = "Location must not exceed 255 characters")
    private String location;

    @Size(max = 30, message = "Ward number must not exceed 30 characters")
    private String wardNo;

    private ComplaintPriority priority;
}
