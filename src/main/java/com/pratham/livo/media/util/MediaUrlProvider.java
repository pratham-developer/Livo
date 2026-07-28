package com.pratham.livo.media.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class MediaUrlProvider {

    @Value("${supabase.storage.public-url}")
    private String publicUrlBase;

    public String generatePublicUrl(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        // Safety check: if it's already a full URL, don't append
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }
        return publicUrlBase + path;
    }

    public List<String> generatePublicUrls(List<String> paths) {
        if (paths == null) {
            return List.of();
        }
        return paths.stream()
                .map(this::generatePublicUrl)

                .collect(Collectors.toList());
    }
}