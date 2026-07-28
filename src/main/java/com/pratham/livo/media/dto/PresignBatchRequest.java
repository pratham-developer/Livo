package com.pratham.livo.media.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PresignBatchRequest(
        @NotEmpty
        @Size(max = 10, message = "Maximum 10 files allowed per request")
        List<PresignFileRequest> files
) {}
