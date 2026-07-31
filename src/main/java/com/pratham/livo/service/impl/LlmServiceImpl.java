package com.pratham.livo.service.impl;

import com.pratham.livo.service.LlmService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

@Service
@Slf4j
public class LlmServiceImpl implements LlmService {

    private final ChatClient chatClient;

    public LlmServiceImpl(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public String generateSummary(String systemPrompt, String reviewData) {
        log.info("Sending {} chars of stratified review data to the LLM", reviewData.length());
        try {
            String response = chatClient.prompt()
                    .system(systemPrompt)
                    .user(reviewData)
                    .call()
                    .content();

            if (response == null || response.isBlank()) {
                throw new IllegalStateException("LLM returned empty content");
            }
            log.info("Received AI summary ({} chars)", response.length());
            return response.trim();

        } catch (ResourceAccessException e) {
            // connect/read timeout or network failure
            log.warn("LLM call timed out or unreachable: {}", e.getMessage());
            throw new RuntimeException("LLM timeout/unreachable", e);
        } catch (Exception e) {
            log.error("LLM API call failed (down, rate-limited, or bad response)", e);
            throw new RuntimeException("LLM API call failed", e);
        }
    }
}