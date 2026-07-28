package com.pratham.livo.media.cron;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class MediaCleanupCron {

    private final S3Client s3Client;

    @Value("${supabase.s3.bucket}")
    private String bucketName;

    // Run every 4 hours.
    // Cron format: Seconds Minutes Hours DayOfMonth Month DayOfWeek
    @Scheduled(cron = "0 0 */4 * * *")
    @SchedulerLock(
            name = "temporaryMediaCleanupTask",
            lockAtLeastFor = "PT5M",
            lockAtMostFor = "PT30M"
    )
    public void cleanupAbandonedTemporaryFiles() {
        log.info("CRON JOB START: Cleaning up abandoned temporary media files.");

        try {
            Instant threshold = Instant.now().minus(24, ChronoUnit.HOURS);
            int deletedCount = 0;
            String continuationToken = null;

            do {
                ListObjectsV2Request.Builder requestBuilder = ListObjectsV2Request.builder()
                        .bucket(bucketName)
                        .prefix("temp/") // Only look in the temp folder!
                        .continuationToken(continuationToken);

                ListObjectsV2Response response = s3Client.listObjectsV2(requestBuilder.build());

                // Find all objects older than 24 hours
                List<ObjectIdentifier> objectsToDelete = response.contents().stream()
                        .filter(s3Object -> s3Object.lastModified().isBefore(threshold))
                        .map(s3Object -> ObjectIdentifier.builder().key(s3Object.key()).build())
                        .collect(Collectors.toList());

                if (!objectsToDelete.isEmpty()) {
                    s3Client.deleteObjects(DeleteObjectsRequest.builder()
                            .bucket(bucketName)
                            .delete(Delete.builder().objects(objectsToDelete).build())
                            .build());

                    deletedCount += objectsToDelete.size();
                }

                continuationToken = response.nextContinuationToken();

            } while (continuationToken != null);

            log.info("CRON JOB SUCCESS: Deleted {} abandoned temporary files.", deletedCount);

        } catch (Exception e) {
            log.error("CRON JOB FAILED: Error cleaning up temporary media files.", e);
        }
    }
}