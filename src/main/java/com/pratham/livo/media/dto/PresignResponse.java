package com.pratham.livo.media.dto;

public record PresignResponse(
        String tempPath,
        String presignedUrl
) {}