package com.pratham.livo.service;

import com.pratham.livo.dto.message.EmailMessage;

public interface EmailPublisher {
    void publish(EmailMessage emailMessage);
}
