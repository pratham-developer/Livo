package com.pratham.livo.service.impl;

import com.pratham.livo.dto.auth.*;
import com.pratham.livo.entity.User;
import com.pratham.livo.enums.Role;
import com.pratham.livo.exception.BadRequestException;
import com.pratham.livo.exception.SecurityRiskException;
import com.pratham.livo.exception.SessionNotFoundException;
import com.pratham.livo.repository.UserRepository;
import com.pratham.livo.security.JwtService;
import com.pratham.livo.security.SecurityHelper;
import com.pratham.livo.service.AuthService;
import com.pratham.livo.service.SessionService;
import com.pratham.livo.utils.EmailSender;
import com.pratham.livo.utils.OtpGenerator;
import com.pratham.livo.utils.SignupSessionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
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
    private final ModelMapper modelMapper;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final SessionService sessionService;
    private final SecurityHelper securityHelper;
    private final OtpGenerator otpGenerator;
    private final SignupSessionUtil signupSessionUtil;
    private final EmailSender emailSender;

    private static final int MAX_LIMIT = 3;
    private static final long TIME_LIMIT = 60000; //one minute
    //max time to complete the entire flow
    private static final long ABSOLUTE_SESSION_LIMIT = 1200000; //20 minutes

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
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        //create session and get the jti for it
        String jti = sessionService.createSession(user.getId(),refreshToken);

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
        Long userId = jwtService.getUserIdFromRefreshToken(refreshToken);
        if(!authenticatedUser.getId().equals(userId)){
            throw new AuthenticationServiceException("Cannot verify the authenticated user.");
        }
        log.info("Logging out user with id: {}",userId);
        sessionService.deleteSession(userId,refreshToken);
        log.info("Successfully logged out user with id: {}",userId);
    }

    @Override
    public LoginResponseDto refresh(String refreshToken) {
        if(refreshToken == null || refreshToken.isEmpty()){
            throw new BadRequestException("Refresh token cannot be null or empty");
        }
        Long userId = jwtService.getUserIdFromRefreshToken(refreshToken);
        log.info("Refreshing user with id: {}",userId);
        LoginResponseDto loginResponseDto = sessionService.refreshSession(userId,refreshToken);
        if(loginResponseDto==null){
            throw new SecurityRiskException("Security Risk Detected. Please login again.");
        }
        log.info("Successfully refreshed user with id: {}",userId);
        return loginResponseDto;
    }

    @Override
    public InitiateSignupResponseDto initiateSignup(InitiateSignupRequestDto requestDto, String ipAddress) {
        log.info("Initiating signup with email: {}",requestDto.getEmail());
        //if user exists with this email, throw bad request
        if(userRepository.existsByEmail(requestDto.getEmail())){
            throw new BadRequestException("An account already exists with this email.");
        }

        //generate signup session
        String registrationId = UUID.randomUUID().toString(); //reg id for ensuring same device
        String otp = otpGenerator.generate(); //otp for verification

        //generate signup session
        SignupSession signupSession = SignupSession.builder()
                .name(requestDto.getName())
                .email(requestDto.getEmail())
                .passwordHash(passwordEncoder.encode(requestDto.getPassword()))
                .otpHash(passwordEncoder.encode(otp))
                .ipAddress(ipAddress)
                .attempts(0)
                .resendCount(0)
                .lastResendAt(System.currentTimeMillis())
                .sessionCreatedAt(System.currentTimeMillis())
                .build();

        //push session to redis
        signupSessionUtil.pushToRedis(registrationId,signupSession);

        //send email to user for otp
        emailSender.sendEmail(
                requestDto.getEmail(),
                "Verify your email",
                "verify_email",
                Map.of("otp",otp)
        );

        //return response
        log.info("Successfully Initiated signup with email: {}",requestDto.getEmail());
        return InitiateSignupResponseDto.builder()
                .registrationId(registrationId).nextResendAt(signupSession.getLastResendAt()+TIME_LIMIT)
                .build();
    }

    @Override
    @Transactional
    public CompleteSignupResponseDto completeSignup(CompleteSignupRequestDto requestDto, String ipAddress) {
        log.info("Completing signup with registrationId: {}", requestDto.getRegistrationId());

        //fetch signup session from redis
        SignupSession signupSession = signupSessionUtil.fetchFromRedis(requestDto.getRegistrationId());

        if (signupSession == null) {
            throw new SessionNotFoundException("Session expired. Please register again.");
        }

        //if registration window expired then delete session
        long now = System.currentTimeMillis();
        long sessionAge = now - signupSession.getSessionCreatedAt();

        if(sessionAge > ABSOLUTE_SESSION_LIMIT){
            signupSessionUtil.deleteFromRedis(requestDto.getRegistrationId());
            throw new SecurityRiskException("Registration session timed out.");
        }

        //ip match
        if (!ipAddress.equals(signupSession.getIpAddress())) {
            signupSessionUtil.deleteFromRedis(requestDto.getRegistrationId());
            throw new SecurityRiskException("IP Address mismatch. Please register again.");
        }

        //if somehow arrive here with max attempts already
        if (signupSession.getAttempts() >= MAX_LIMIT) {
            signupSessionUtil.deleteFromRedis(requestDto.getRegistrationId());
            throw new SecurityRiskException("Maximum attempts exceeded. Please register again.");
        }

        //validate OTP
        if (!passwordEncoder.matches(requestDto.getOtp(), signupSession.getOtpHash())) {

            //increment attempts
            int newAttempts = signupSession.getAttempts() + 1;
            signupSession.setAttempts(newAttempts);

            //check if this was the final allowed attempt
            if (newAttempts >= MAX_LIMIT) {
                signupSessionUtil.deleteFromRedis(requestDto.getRegistrationId());
                throw new SecurityRiskException("Maximum attempts reached. Session invalidated.");
            }

            //update in redis for other attempts
            boolean updated = signupSessionUtil.updateInRedis(requestDto.getRegistrationId(), signupSession);
            if (!updated) {
                throw new SessionNotFoundException("Session expired during verification.");
            }

            //throw 400 Bad Request
            throw new BadRequestException("Invalid OTP. Attempts left: " + (MAX_LIMIT - newAttempts));
        }

        //if otp match build user
        User user = User.builder()
                .name(signupSession.getName())
                .email(signupSession.getEmail())
                .passwordHash(signupSession.getPasswordHash())
                .roles(Set.of(Role.GUEST)).build();

        //save user in db
        User savedUser = userRepository.saveAndFlush(user);

        //clean user from redis
        signupSessionUtil.deleteFromRedis(requestDto.getRegistrationId());

        log.info("Signup successful for userId: {}", savedUser.getId());
        //send email to user for onboarding
        emailSender.sendEmail(
                savedUser.getEmail(),
                "Welcome to Livo!",
                "welcome",
                Map.of("userName",savedUser.getName(),"userEmail",savedUser.getEmail())
        );
        return modelMapper.map(savedUser, CompleteSignupResponseDto.class);
    }

    @Override
    public ResendOtpResponseDto resendSignupOtp(ResendOtpRequestDto requestDto, String ipAddress) {
        log.info("Resending otp for signup with registrationId: {}", requestDto.getRegistrationId());
        //fetch signup session from redis
        SignupSession signupSession = signupSessionUtil.fetchFromRedis(requestDto.getRegistrationId());

        if (signupSession == null) {
            throw new SessionNotFoundException("Session expired. Please register again.");
        }

        //if registration window expired then delete session
        long now = System.currentTimeMillis();
        long sessionAge = now - signupSession.getSessionCreatedAt();

        if(sessionAge > ABSOLUTE_SESSION_LIMIT){
            signupSessionUtil.deleteFromRedis(requestDto.getRegistrationId());
            throw new SecurityRiskException("Registration session timed out.");
        }

        //ip match
        if (!ipAddress.equals(signupSession.getIpAddress())) {
            signupSessionUtil.deleteFromRedis(requestDto.getRegistrationId());
            throw new SecurityRiskException("IP Address mismatch. Please register again.");
        }

        //if somehow arrive here with max resend attempts already
        int resendCount = signupSession.getResendCount();
        if (resendCount >= MAX_LIMIT) {
            throw new BadRequestException("Maximum attempts reached.");
        }

        //check if time limit has been reached to allow resend
        long timePassed = now - signupSession.getLastResendAt();
        if(timePassed<TIME_LIMIT){
            throw new BadRequestException("Please wait for "+((TIME_LIMIT - timePassed)/1000) + " seconds");
        }

        //otherwise generate new otp
        String newOtp = otpGenerator.generate();

        //update in redis
        signupSession.setOtpHash(passwordEncoder.encode(newOtp));
        signupSession.setResendCount(resendCount+1);
        signupSession.setLastResendAt(now);
        signupSession.setAttempts(0); //new otp means new attempts

        //instead of updating we re-push to redis to give time for new otp
        //so ttl is reset
        signupSessionUtil.pushToRedis(requestDto.getRegistrationId(),signupSession);

        //send email to user for new otp
        emailSender.sendEmail(
                signupSession.getEmail(),
                "Verify your email",
                "verify_email",
                Map.of("otp",newOtp)
        );
        log.info("Successfully resent otp for signup with registrationId: {}", requestDto.getRegistrationId());
        return ResendOtpResponseDto.builder()
                .nextResendAt(now+TIME_LIMIT).build();
    }


}
