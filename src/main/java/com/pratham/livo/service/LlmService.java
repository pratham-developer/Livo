package com.pratham.livo.service;

public interface LlmService {
    String generateSummary(String systemPrompt, String reviewData);
}
