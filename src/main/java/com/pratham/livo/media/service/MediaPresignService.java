package com.pratham.livo.media.service;

import com.pratham.livo.media.dto.PresignBatchRequest;
import com.pratham.livo.media.dto.PresignFileRequest;
import com.pratham.livo.media.dto.PresignResponse;
import com.pratham.livo.media.port.StorageGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MediaPresignService {

    private static final long MAX_COMBINED_PAYLOAD = 20 * 1024 * 1024; // 20 MB

    private final StorageGateway storageGateway;

    public List<PresignResponse> generateUploadUrls(Long userId, PresignBatchRequest request) {
        String userIdStr = String.valueOf(userId);

        long totalSize = request.files().stream()
                .mapToLong(PresignFileRequest::contentLength)
                .sum();

        if (totalSize > MAX_COMBINED_PAYLOAD) {
            throw new IllegalArgumentException("Combined upload payload exceeds 20 MB limit");
        }

        return request.files().stream()
                .map(file -> storageGateway.generatePresignedUrl(userIdStr, file))
                .toList();
    }
}