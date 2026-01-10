package com.pratham.livo.exception;

import org.springframework.security.core.AuthenticationException;

public class SecurityRiskException extends AuthenticationException {
    public SecurityRiskException(String message) {
        super(message);
    }
}
