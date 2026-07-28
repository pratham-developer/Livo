package com.pratham.livo.media.port;

import com.pratham.livo.media.dto.PresignFileRequest;
import com.pratham.livo.media.dto.PresignResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
@Slf4j
public class SupabaseStorageAdapter implements StorageGateway {
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${supabase.s3.bucket}")
    private String bucketName;

    @Override
    public PresignResponse generatePresignedUrl(String userId, PresignFileRequest request) {
        String extension = getFileExtension(request.fileName());
        String objectKey = String.format("temp/%s/%s%s", userId, UUID.randomUUID(), extension);

        // Bind strict constraints to the URL so Supabase rejects mismatched uploads
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentType(request.contentType())
                .contentLength(request.contentLength())
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(15))
                .putObjectRequest(putObjectRequest)
                .build();

        PresignedPutObjectRequest presignedPut = s3Presigner.presignPutObject(presignRequest);
        URL url = presignedPut.url();

        return new PresignResponse(objectKey, url.toString());
    }

    @Override
    public void validateTemporaryFiles(String userId, List<String> tempPaths) {
        String expectedPrefix = "temp/" + userId + "/";

        for (String path : tempPaths) {
            if (!path.startsWith(expectedPrefix)) {
                log.warn("Security alert: User {} attempted to claim path {}", userId, path);
                throw new SecurityException("Unauthorized or invalid media path: " + path);
            }

            try {
                // HeadObject is lightweight; it just fetches metadata to prove the file exists
                s3Client.headObject(b -> b.bucket(bucketName).key(path));
            } catch (NoSuchKeyException e) {
                throw new IllegalArgumentException("Temporary file missing or expired: " + path);
            }
        }
    }

    @Override
    public List<String> promoteToPermanent(List<String> tempPaths, String destinationPrefix) {
        List<String> successfulCopies = new ArrayList<>();

        for (String tempPath : tempPaths) {
            String extension = getFileExtension(tempPath);
            String permanentKey = destinationPrefix + UUID.randomUUID() + extension;

            try {
                s3Client.copyObject(CopyObjectRequest.builder()
                        .sourceBucket(bucketName)
                        .sourceKey(tempPath)
                        .destinationBucket(bucketName)
                        .destinationKey(permanentKey)
                        .build());

                successfulCopies.add(permanentKey);
            } catch (Exception e) {
                log.error("Failed to copy {} to {}. Rolling back copied files.", tempPath, permanentKey, e);
                // If any file fails to copy, delete the ones we already successfully copied in this batch
                deleteFiles(successfulCopies);
                throw new RuntimeException("Promotion failed. Mid-flight S3 rollback executed.", e);
            }
        }
        return successfulCopies;
    }

    @Override
    public void deleteFiles(List<String> paths) {
        if (paths == null || paths.isEmpty()) return;

        List<ObjectIdentifier> identifiers = paths.stream()
                .map(path -> ObjectIdentifier.builder().key(path).build())
                .collect(Collectors.toList());

        s3Client.deleteObjects(DeleteObjectsRequest.builder()
                .bucket(bucketName)
                .delete(Delete.builder().objects(identifiers).build())
                .build());
    }

    private String getFileExtension(String fileName) {
        int lastIndexOf = fileName.lastIndexOf(".");
        return lastIndexOf == -1 ? "" : fileName.substring(lastIndexOf);
    }
}
