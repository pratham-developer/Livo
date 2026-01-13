package com.pratham.livo.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
//this session will be pushed to redis on /signup/initiate
@Builder
public class SignupSession implements Serializable {
    private String name;
    private String email;
    private String passwordHash; //hashed password
    private String otpHash; //hashed otp for email verification
    private String ipAddress; //ip address to make less vulnerable

    private int attempts; //wrong otp attempts
    private int resendCount; //allow max 3 times to hit resend otp
    private long lastResendAt; //time at which last resend was hit
    private long sessionCreatedAt;
}
