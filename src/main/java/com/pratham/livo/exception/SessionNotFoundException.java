package com.pratham.livo.exception;

import org.springframework.security.core.AuthenticationException;

public class SessionNotFoundException extends AuthenticationException {
    public SessionNotFoundException(String msg) {
        super(msg);
    }
}
