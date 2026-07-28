package com.pratham.livo.media.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PresignFileRequest(
        @NotBlank(message = "File name cannot be blank")
        String fileName,

        @NotBlank
        @Pattern(regexp = "^image/(jpeg|png|webp)$", message = "Only JPEG, PNG, and WEBP are allowed")
        String contentType,

        @Max(value = 5242880, message = "Individual file size must not exceed 5 MB")
        long contentLength
) {}


