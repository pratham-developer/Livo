package com.pratham.livo.dto.auth.otp;

import com.pratham.livo.enums.OtpType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
//this session will be pushed to redis on /signup/initiate
@Builder
public class OtpSession implements Serializable {

    private String email;

    private String otpHash; //hashed otp for email verification
    private String ipAddress; //ip address to make less vulnerable

    private int attempts; //wrong otp attempts
    private int resendCount; //allow max 3 times to hit resend otp
    private long lastResendAt; //time at which last resend was hit
    private long sessionCreatedAt;

    private Map<String, String> payload; //to store name, passwordHash etc.
    private OtpType otpType;
}
