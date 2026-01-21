package com.pratham.livo.service.impl;

import com.pratham.livo.dto.auth.*;
import com.pratham.livo.dto.auth.otp.*;
import com.pratham.livo.entity.User;
import com.pratham.livo.enums.OtpType;
import com.pratham.livo.enums.Role;
import com.pratham.livo.exception.BadRequestException;
import com.pratham.livo.exception.SessionNotFoundException;
import com.pratham.livo.repository.UserRepository;
import com.pratham.livo.security.JwtService;
import com.pratham.livo.security.SecurityHelper;
import com.pratham.livo.service.AuthService;
import com.pratham.livo.service.OtpService;
import com.pratham.livo.service.SessionService;
import com.pratham.livo.utils.EmailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final SessionService sessionService;
    private final SecurityHelper securityHelper;
    private final EmailSender emailSender;
    private final OtpService otpService;

    private static final String PAYLOAD_NAME = "name";
    private static final String PAYLOAD_PWD_HASH = "passwordHash";

    @Override
    @Transactional
    public LoginResponseDto login(LoginRequestDto loginRequestDto) {
        log.info("Logging in user with email: {}",loginRequestDto.getEmail());
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequestDto.getEmail(),loginRequestDto.getPassword()
                )
        );

        User user = (User) authentication.getPrincipal();
        String familyId = UUID.randomUUID().toString();
        String refreshToken = jwtService.generateRefreshToken(user.getId(), familyId);

        //create session and get the jti for it
        String jti = sessionService.createSession(user.getId(),refreshToken, familyId);

        //create access token for this user with this jti
        String accessToken = jwtService.generateAccessToken(user,jti);
        log.info("Successfully logged in user with email: {}",loginRequestDto.getEmail());
        return LoginResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Override
    public void logout(String refreshToken) {
        if(refreshToken == null || refreshToken.isEmpty()){
            throw new BadRequestException("Refresh token cannot be null or empty");
        }
        AuthenticatedUser authenticatedUser = securityHelper.getCurrentAuthenticatedUser().orElseThrow(
                ()->new AuthenticationServiceException("Cannot verify the authenticated user.")
        );
        RefreshTokenClaims claims = jwtService.parseRefreshToken(refreshToken);
        Long userId = claims.getUserId();
        String familyId = claims.getFamilyId();
        if(!authenticatedUser.getId().equals(userId)){
            throw new AuthenticationServiceException("Cannot verify the authenticated user.");
        }
        log.info("Logging out user with id: {}",userId);
        sessionService.deleteSession(userId,familyId);
        log.info("Successfully logged out user with id: {}",userId);
    }

    @Override
    public LoginResponseDto refresh(String refreshToken) {
        if(refreshToken == null || refreshToken.isEmpty()){
            throw new BadRequestException("Refresh token cannot be null or empty");
        }
        RefreshTokenClaims claims = jwtService.parseRefreshToken(refreshToken);
        Long userId = claims.getUserId();
        String familyId = claims.getFamilyId();
        log.info("Refreshing user with id: {}",userId);
        LoginResponseDto loginResponseDto = sessionService.refreshSession(userId,refreshToken,familyId);
        if(loginResponseDto==null){
            throw new SessionNotFoundException("Please login again.");
        }
        log.info("Successfully refreshed user with id: {}",userId);
        return loginResponseDto;
    }

    @Override
    public InitiateOtpResponseDto initiateSignup(SignupRequestDto requestDto, String ipAddress) {
        log.info("Initiating signup with email: {}",requestDto.getEmail());
        if (userRepository.existsByEmail(requestDto.getEmail())) {
            throw new BadRequestException("An account already exists with this email.");
        }
        //create otp session
        OtpHelperDto otpHelperDto = otpService.createOtpSession(
                requestDto.getEmail(),ipAddress,
                OtpType.SIGNUP,Map.of(PAYLOAD_NAME,requestDto.getName(),
                        PAYLOAD_PWD_HASH,passwordEncoder.encode(requestDto.getPassword()))
                );

        //send email to user for otp
        emailSender.sendEmail(
                requestDto.getEmail(),
                "Verify your email",
                "verify_email",
                Map.of("otp", otpHelperDto.getOtp())
        );

        //return response
        log.info("Successfully Initiated signup with email: {}",requestDto.getEmail());
        return InitiateOtpResponseDto.builder()
                .registrationId(otpHelperDto.getRegistrationId())
                .nextResendAt(otpHelperDto.getNextResendAt())
                .build();
    }

    @Override
    @Transactional
    public OtpVerifyResponseDto completeSignup(SignupOtpVerifyRequestDto requestDto, String ipAddress) {
        log.info("Completing signup with registrationId: {}", requestDto.getRegistrationId());
        //verify and get the otp session
        String registrationId = requestDto.getRegistrationId();
        OtpSession otpSession = otpService.verifyOtp(
                registrationId,
                ipAddress, OtpType.SIGNUP,
                requestDto.getOtp()
        );

        //get payload
        Map<String,String> payload = otpSession.getPayload();

        //build user
        User user = User.builder()
                .name(payload.get(PAYLOAD_NAME))
                .email(otpSession.getEmail())
                .passwordHash(payload.get(PAYLOAD_PWD_HASH))
                .roles(Set.of(Role.GUEST)).build();

        //save user in db
        User savedUser = userRepository.saveAndFlush(user);

        //clean otp session from redis
        otpService.deleteOtpSession(registrationId,otpSession);

        log.info("Signup successful for userId: {}", savedUser.getId());
        //send email to user for onboarding
        emailSender.sendEmail(
                savedUser.getEmail(),
                "Welcome to Livo!",
                "welcome",
                Map.of("userName",savedUser.getName(),"userEmail",savedUser.getEmail())
        );
        return OtpVerifyResponseDto
                .builder().message("Account has been successfully created.")
                .build();
    }

    @Override
    public ResendOtpResponseDto resendSignupOtp(ResendOtpRequestDto requestDto, String ipAddress) {
        log.info("Resending otp for signup with registrationId: {}", requestDto.getRegistrationId());
        //update the otp session with new otp
        OtpHelperDto otpHelperDto = otpService.resendOtp(
               requestDto.getRegistrationId(),
               ipAddress,OtpType.SIGNUP
        );


        //send email to user for new otp
        emailSender.sendEmail(
                otpHelperDto.getEmail(),
                "Verify your email",
                "verify_email",
                Map.of("otp",otpHelperDto.getOtp())
        );
        log.info("Successfully resent otp for signup with registrationId: {}", requestDto.getRegistrationId());
        return ResendOtpResponseDto.builder()
                .nextResendAt(otpHelperDto.getNextResendAt()).build();
    }

    @Override
    public InitiateOtpResponseDto initiateForgotPwd(ForgotPwdRequestDto requestDto, String ipAddress) {
        log.info("Initiating forgot password with email: {}",requestDto.getEmail());
        if (!userRepository.existsByEmail(requestDto.getEmail())) {
            throw new BadRequestException("User not found with this email.");
        }
        //create otp session
        OtpHelperDto otpHelperDto = otpService.createOtpSession(
                requestDto.getEmail(),ipAddress,
                OtpType.FORGOT,Map.of());

        //send email to user for otp
        emailSender.sendEmail(
                requestDto.getEmail(),
                "Reset your password",
                "reset_pwd",
                Map.of("otp", otpHelperDto.getOtp())
        );

        //return response
        log.info("Successfully Initiated forgot pwd with email: {}",requestDto.getEmail());
        return InitiateOtpResponseDto.builder()
                .registrationId(otpHelperDto.getRegistrationId())
                .nextResendAt(otpHelperDto.getNextResendAt())
                .build();
    }

    @Override
    @Transactional
    public OtpVerifyResponseDto completeForgotPwd(ForgotOtpVerifyRequestDto requestDto, String ipAddress) {
        log.info("Completing forgot pwd with registrationId: {}", requestDto.getRegistrationId());
        String newPassword = requestDto.getNewPassword();
        if(newPassword == null || newPassword.isEmpty()){
            throw new BadRequestException("password cannot be null or empty.");
        }
        //verify and get the otp session
        String registrationId = requestDto.getRegistrationId();
        OtpSession otpSession = otpService.verifyOtp(
                registrationId,
                ipAddress, OtpType.FORGOT,
                requestDto.getOtp()
        );

        //update user
        User user = userRepository.findByEmail(otpSession.getEmail())
                .orElseThrow(() -> new SessionNotFoundException("User not found."));

        user.setPasswordHash(passwordEncoder.encode(newPassword));

        //save user in db
        User savedUser = userRepository.saveAndFlush(user);

        //clean otp session from redis
        otpService.deleteOtpSession(registrationId,otpSession);

        log.info("Forgot pwd successful for userId: {}", savedUser.getId());
        //send email to user for reset pwd
        emailSender.sendEmail(
                savedUser.getEmail(),
                "Password Reset Successful",
                "reset_pwd_success",
                Map.of("userName",savedUser.getName(),"userEmail",savedUser.getEmail())
        );
        return OtpVerifyResponseDto
                .builder().message("Password reset successfully.")
                .build();
    }

    @Override
    public ResendOtpResponseDto resendForgotPwdOtp(ResendOtpRequestDto requestDto, String ipAddress) {
        log.info("Resending otp for forgot pwd with registrationId: {}", requestDto.getRegistrationId());
        //update the otp session
        OtpHelperDto otpHelperDto = otpService.resendOtp(
                requestDto.getRegistrationId(),
                ipAddress,OtpType.FORGOT
        );


        //send email to user for new otp
        emailSender.sendEmail(
                otpHelperDto.getEmail(),
                "Reset your password",
                "reset_pwd",
                Map.of("otp", otpHelperDto.getOtp())
        );
        log.info("Successfully resent otp for forgot pwd with registrationId: {}", requestDto.getRegistrationId());
        return ResendOtpResponseDto.builder()
                .nextResendAt(otpHelperDto.getNextResendAt()).build();
    }


}
