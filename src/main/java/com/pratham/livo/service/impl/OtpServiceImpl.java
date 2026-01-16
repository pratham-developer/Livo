package com.pratham.livo.service.impl;

import com.pratham.livo.dto.auth.otp.OtpHelperDto;
import com.pratham.livo.dto.auth.otp.OtpSession;
import com.pratham.livo.enums.OtpType;
import com.pratham.livo.exception.BadRequestException;
import com.pratham.livo.exception.SecurityRiskException;
import com.pratham.livo.exception.SessionNotFoundException;
import com.pratham.livo.service.OtpService;
import com.pratham.livo.utils.OtpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpServiceImpl implements OtpService {

    private final OtpUtil otpUtil;
    private final PasswordEncoder passwordEncoder;

    private static final int MAX_ATTEMPTS = 3;
    private static final long RESEND_TIME_LIMIT = 60000; //one minute
    //max time to complete the entire flow
    private static final long ABSOLUTE_SESSION_LIMIT = 1200000;

    @Override
    public OtpHelperDto createOtpSession(String email, String ipAddress, OtpType otpType, Map<String, String> payload) {
        //generate otp session
        String registrationId = UUID.randomUUID().toString(); //reg id for ensuring same device
        String otp = otpUtil.generateOtp(); //otp for verification

        //generate otp session
        OtpSession otpSession = OtpSession.builder()
                .payload(payload)
                .email(email)
                .otpHash(passwordEncoder.encode(otp))
                .ipAddress(ipAddress)
                .attempts(0).resendCount(0)
                .lastResendAt(System.currentTimeMillis())
                .sessionCreatedAt(System.currentTimeMillis())
                .otpType(otpType)
                .build();

        //push session to redis
        otpUtil.pushToRedis(registrationId,otpSession);
        return OtpHelperDto.builder()
                .registrationId(registrationId)
                .otp(otp)
                .nextResendAt(otpSession.getLastResendAt()+RESEND_TIME_LIMIT)
                .build();
    }

    @Override
    public OtpSession verifyOtp(String registrationId, String ipAddress, OtpType otpType, String inputOtp) {
        long now = System.currentTimeMillis();
        OtpSession otpSession = validateSecurity(registrationId,ipAddress,otpType,now);
        //validate OTP
        if (!passwordEncoder.matches(inputOtp, otpSession.getOtpHash())) {
            handleWrongOtp(registrationId,otpSession); //throws exception
            return null; //unreachable
        }
        return otpSession;
    }

    @Override
    public OtpHelperDto resendOtp(String registrationId, String ipAddress, OtpType otpType) {
        //validate security
        long now = System.currentTimeMillis();
        OtpSession otpSession = validateSecurity(registrationId,ipAddress,otpType,now);

        //if somehow arrive here with max resend attempts already
        int resendCount = otpSession.getResendCount();
        if (resendCount >= MAX_ATTEMPTS) {
            throw new BadRequestException("Maximum attempts reached.");
        }

        //check if time limit has been reached to allow resend
        long timePassed = now - otpSession.getLastResendAt();
        if(timePassed<RESEND_TIME_LIMIT){
            throw new BadRequestException("Please wait for "+((RESEND_TIME_LIMIT - timePassed)/1000) + " seconds");
        }

        //otherwise generate new otp
        String newOtp = otpUtil.generateOtp();

        //update in redis
        otpSession.setOtpHash(passwordEncoder.encode(newOtp));
        otpSession.setResendCount(resendCount+1);
        otpSession.setLastResendAt(now);
        otpSession.setAttempts(0); //new otp means new attempts

        //instead of updating we re-push to redis to give time for new otp
        //so ttl is reset
        otpUtil.pushToRedis(registrationId,otpSession);
        return OtpHelperDto.builder()
                .otp(newOtp).email(otpSession.getEmail())
                .nextResendAt(now+RESEND_TIME_LIMIT).build();
    }

    private OtpSession validateSecurity(String registrationId, String ipAddress, OtpType otpType, long now){
        //fetch otp session from redis
        OtpSession otpSession = otpUtil.fetchFromRedis(registrationId,otpType);

        //if null then session expired
        if (otpSession== null) {
            throw new SessionNotFoundException("Session expired.");
        }

        //if registration window expired then delete session
        long sessionAge = now - otpSession.getSessionCreatedAt();

        if(sessionAge > ABSOLUTE_SESSION_LIMIT){
            deleteOtpSession(registrationId,otpSession);
            throw new SecurityRiskException("Session expired.");
        }

        //ip match
        if (!ipAddress.equals(otpSession.getIpAddress())) {
            deleteOtpSession(registrationId,otpSession);
            throw new SecurityRiskException("IP Address mismatch.");
        }

        //if somehow arrive here with max attempts already
        if (otpSession.getAttempts() >= MAX_ATTEMPTS) {
            deleteOtpSession(registrationId,otpSession);
            throw new SecurityRiskException("Maximum attempts exceeded.");
        }
        return otpSession;
    }

    private void handleWrongOtp(String registrationId, OtpSession otpSession){
        //increment attempts
        int newAttempts = otpSession.getAttempts() + 1;
        otpSession.setAttempts(newAttempts);

        //check if this was the final allowed attempt
        if (newAttempts >= MAX_ATTEMPTS) {
            deleteOtpSession(registrationId,otpSession);
            throw new SecurityRiskException("Maximum attempts reached. Session invalidated.");
        }

        //update in redis for other attempts
        boolean updated = otpUtil.updateInRedis(registrationId, otpSession);
        if (!updated) {
            throw new SessionNotFoundException("Session expired during verification.");
        }

        //throw 400 Bad Request
        throw new BadRequestException("Invalid OTP. Attempts left: " + (MAX_ATTEMPTS - newAttempts));
    }

    @Override
    public void deleteOtpSession(String registrationId, OtpSession otpSession){
        otpUtil.deleteFromRedis(registrationId, otpSession.getOtpType());
    }
}
