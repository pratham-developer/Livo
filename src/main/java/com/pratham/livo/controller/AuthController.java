package com.pratham.livo.controller;

import com.pratham.livo.dto.auth.*;
import com.pratham.livo.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup/initiate")
    public ResponseEntity<InitiateSignupResponseDto> initiateSignup(
            @RequestBody InitiateSignupRequestDto requestDto,
            HttpServletRequest servletRequest
    ){
        log.info("Attempting to initiate signup for user with email: {}",requestDto.getEmail());
        return ResponseEntity.ok(authService.initiateSignup(requestDto,servletRequest.getRemoteAddr()));
    }

    @PostMapping("/signup/complete")
    public ResponseEntity<CompleteSignupResponseDto> completeSignup(
            @RequestBody CompleteSignupRequestDto requestDto,
            HttpServletRequest servletRequest
    ){
        log.info("Attempting to complete signup for user with registrationId: {}",requestDto.getRegistrationId());
        return ResponseEntity.ok(authService.completeSignup(requestDto,servletRequest.getRemoteAddr()));
    }

    @PostMapping("/signup/resend-otp")
    public ResponseEntity<ResendOtpResponseDto> resendOtp(
            @RequestBody ResendOtpRequestDto requestDto,
            HttpServletRequest servletRequest
    ){
        log.info("Attempting to resend otp for signing up user with registrationId: {}",requestDto.getRegistrationId());
        return ResponseEntity.ok(authService.resendSignupOtp(requestDto,servletRequest.getRemoteAddr()));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@RequestBody LoginRequestDto loginRequestDto){
        log.info("Attempting to login user with email: {}",loginRequestDto.getEmail());
        return ResponseEntity.ok(authService.login(loginRequestDto));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("x-refresh-token") String refreshToken){
        authService.logout(refreshToken);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponseDto> refresh(@RequestHeader("x-refresh-token") String refreshToken){
        return ResponseEntity.ok(authService.refresh(refreshToken));
    }
}
