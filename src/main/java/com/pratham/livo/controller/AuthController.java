package com.pratham.livo.controller;

import com.pratham.livo.dto.auth.LoginRequestDto;
import com.pratham.livo.dto.auth.LoginResponseDto;
import com.pratham.livo.dto.auth.SignupRequestDto;
import com.pratham.livo.dto.auth.SignupResponseDto;
import com.pratham.livo.service.AuthService;
//import com.pratham.livo.utils.EmailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

//import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    //private final EmailSender emailSender;

    @PostMapping("/signup")
    public ResponseEntity<SignupResponseDto> signup(@RequestBody SignupRequestDto signupRequestDto){
        log.info("Attempting to register user with email: {}",signupRequestDto.getEmail());
        SignupResponseDto signupResponseDto = authService.signup(signupRequestDto);
        /*
        emailSender.sendEmail(
                signupResponseDto.getEmail(),
                "Welcome to Livo!",
                "welcome",
                Map.of("userName",signupResponseDto.getName())
        );*/
        return ResponseEntity.status(HttpStatus.CREATED).body(signupResponseDto);
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
