package com.pratham.livo.controller;

import com.pratham.livo.dto.auth.*;
import com.pratham.livo.dto.auth.otp.*;
import com.pratham.livo.service.AuthService;
import com.pratham.livo.utils.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
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
    private final IpUtil ipUtil;

    @PostMapping("/signup/initiate")
    public ResponseEntity<InitiateOtpResponseDto> initiateSignup(
            @RequestBody SignupRequestDto requestDto,
            HttpServletRequest servletRequest
    ){
        log.info("Attempting to initiate signup for user with email: {}",requestDto.getEmail());
        String ip = ipUtil.getClientIp(servletRequest);
        return ResponseEntity.ok(authService.initiateSignup(requestDto,ip));
    }

    @PostMapping("/signup/complete")
    public ResponseEntity<OtpVerifyResponseDto> completeSignup(
            @RequestBody SignupOtpVerifyRequestDto requestDto,
            HttpServletRequest servletRequest
    ){
        log.info("Attempting to complete signup for user with registrationId: {}",requestDto.getRegistrationId());
        String ip = ipUtil.getClientIp(servletRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                        .body(authService.completeSignup(requestDto,ip));
    }

    @PostMapping("/signup/resend-otp")
    public ResponseEntity<ResendOtpResponseDto> resendSignupOtp(
            @RequestBody ResendOtpRequestDto requestDto,
            HttpServletRequest servletRequest
    ){
        log.info("Attempting to resend otp for signing up user with registrationId: {}",requestDto.getRegistrationId());
        String ip = ipUtil.getClientIp(servletRequest);
        return ResponseEntity.ok(authService.resendSignupOtp(requestDto,ip));
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

    @PostMapping("/forgot-pwd/initiate")
    public ResponseEntity<InitiateOtpResponseDto> initiateForgotPwd(
            @RequestBody ForgotPwdRequestDto requestDto,
            HttpServletRequest servletRequest
    ){
        log.info("Attempting to initiate forgot pwd for user with email: {}",requestDto.getEmail());
        String ip = ipUtil.getClientIp(servletRequest);
        return ResponseEntity.ok(authService.initiateForgotPwd(requestDto,ip));
    }

    @PostMapping("/forgot-pwd/complete")
    public ResponseEntity<OtpVerifyResponseDto> completeForgotPwd(
            @RequestBody ForgotOtpVerifyRequestDto requestDto,
            HttpServletRequest servletRequest
    ){
        log.info("Attempting to complete forgot pwd for user with registrationId: {}",requestDto.getRegistrationId());
        String ip = ipUtil.getClientIp(servletRequest);
        return ResponseEntity.ok(authService.completeForgotPwd(requestDto,ip));
    }

    @PostMapping("/forgot-pwd/resend-otp")
    public ResponseEntity<ResendOtpResponseDto> resendForgotPwdOtp(
            @RequestBody ResendOtpRequestDto requestDto,
            HttpServletRequest servletRequest
    ){
        log.info("Attempting to resend otp for forgot pwd with registrationId: {}",requestDto.getRegistrationId());
        String ip = ipUtil.getClientIp(servletRequest);
        return ResponseEntity.ok(authService.resendForgotPwdOtp(requestDto,ip));
    }
}
