package com.pratham.livo.media.controller;

import com.pratham.livo.dto.auth.AuthenticatedUser;
import com.pratham.livo.exception.SessionNotFoundException;
import com.pratham.livo.media.dto.PresignBatchRequest;
import com.pratham.livo.media.dto.PresignResponse;
import com.pratham.livo.media.service.MediaPresignService;
import com.pratham.livo.security.SecurityHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/media")
@RequiredArgsConstructor
@Slf4j
public class MediaController {

    private final MediaPresignService mediaPresignService;
    private final SecurityHelper securityHelper;

    @PostMapping("/presign")
    public ResponseEntity<List<PresignResponse>> generatePresignedUrls(
            @Valid @RequestBody PresignBatchRequest request) {

        AuthenticatedUser currentUser = securityHelper.getCurrentAuthenticatedUser()
                .orElseThrow(() -> new SessionNotFoundException("Cannot identify the authenticated user"));

        log.info("Generating presigned URLs for user ID: {} for {} file(s)",
                currentUser.getId(), request.files().size());

        List<PresignResponse> responses = mediaPresignService.generateUploadUrls(
                currentUser.getId(),
                request
        );

        return ResponseEntity.ok(responses);
    }
}