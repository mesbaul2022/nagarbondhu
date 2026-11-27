package com.nagarbondhu.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 120, message = "Name must be between 2 and 120 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    @Size(max = 180, message = "Email must not exceed 180 characters")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @Size(max = 30, message = "Phone must not exceed 30 characters")
    private String phone;
}
