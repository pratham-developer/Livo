package com.pratham.livo.service;

import com.pratham.livo.dto.auth.LoginResponseDto;

public interface SessionService {
    String createSession(Long userId, String refreshToken);
    void deleteSession(Long userId, String refreshToken);
    LoginResponseDto refreshSession(Long userId, String refreshToken);
}
