package com.pratham.livo.controller;

import com.pratham.livo.dto.auth.*;
import com.pratham.livo.dto.auth.otp.*;
import com.pratham.livo.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup/initiate")
    public ResponseEntity<InitiateOtpResponseDto> initiateSignup(
            @Valid @RequestBody SignupRequestDto requestDto
    ){
        log.info("Attempting to initiate signup for user with email: {}",requestDto.getEmail());
        return ResponseEntity.ok(authService.initiateSignup(requestDto));
    }

    @PostMapping("/signup/complete")
    public ResponseEntity<OtpVerifyResponseDto> completeSignup(
            @Valid @RequestBody SignupOtpVerifyRequestDto requestDto
    ){
        log.info("Attempting to complete signup for user with registrationId: {}",requestDto.getRegistrationId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.completeSignup(requestDto));
    }

    @PostMapping("/signup/resend-otp")
    public ResponseEntity<ResendOtpResponseDto> resendSignupOtp(
            @Valid @RequestBody ResendOtpRequestDto requestDto
    ){
        log.info("Attempting to resend otp for signing up user with registrationId: {}",requestDto.getRegistrationId());
        return ResponseEntity.ok(authService.resendSignupOtp(requestDto));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto loginRequestDto){
        log.info("Attempting to login user with email: {}",loginRequestDto.getEmail());
        return ResponseEntity.ok(authService.login(loginRequestDto));
    }

    @PostMapping("/forgot-pwd/initiate")
    public ResponseEntity<InitiateOtpResponseDto> initiateForgotPwd(
            @Valid @RequestBody ForgotPwdRequestDto requestDto
    ){
        log.info("Attempting to initiate forgot pwd for user with email: {}",requestDto.getEmail());
        return ResponseEntity.ok(authService.initiateForgotPwd(requestDto));
    }

    @PostMapping("/forgot-pwd/complete")
    public ResponseEntity<OtpVerifyResponseDto> completeForgotPwd(
            @Valid @RequestBody ForgotOtpVerifyRequestDto requestDto
    ){
        log.info("Attempting to complete forgot pwd for user with registrationId: {}",requestDto.getRegistrationId());
        return ResponseEntity.ok(authService.completeForgotPwd(requestDto));
    }

    @PostMapping("/forgot-pwd/resend-otp")
    public ResponseEntity<ResendOtpResponseDto> resendForgotPwdOtp(
            @Valid @RequestBody ResendOtpRequestDto requestDto
    ){
        log.info("Attempting to resend otp for forgot pwd with registrationId: {}",requestDto.getRegistrationId());
        return ResponseEntity.ok(authService.resendForgotPwdOtp(requestDto));
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