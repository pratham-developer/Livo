package com.pratham.livo.service;

import brevo.ApiException;
import com.pratham.livo.dto.message.EmailMessage;

public interface EmailConsumer {
    void consumeEmail(EmailMessage emailMessage);
    void sendWithBrevo(EmailMessage emailMessage) throws ApiException;
}
