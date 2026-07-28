package com.pratham.livo.media.port;

import com.pratham.livo.media.dto.PresignFileRequest;
import com.pratham.livo.media.dto.PresignResponse;

import java.util.List;

public interface StorageGateway {

    // Generates a temporary PUT URL for direct client upload.
    PresignResponse generatePresignedUrl(String userId, PresignFileRequest request);


    // Verifies temporary files exist and meet requirements before promotion.
    void validateTemporaryFiles(String userId, List<String> tempPaths);


    // Promotes files from temporary paths to a permanent prefix (e.g., hotels/123/).
    // Returns the list of permanent paths.
    List<String> promoteToPermanent(List<String> tempPaths, String destinationPrefix);

    // Permanently deletes objects (used for cleanup and rolling back failures).
    void deleteFiles(List<String> paths);
}
