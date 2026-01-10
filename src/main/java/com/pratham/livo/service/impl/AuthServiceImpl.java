package com.pratham.livo.service.impl;

import com.pratham.livo.dto.auth.*;
import com.pratham.livo.entity.User;
import com.pratham.livo.enums.Role;
import com.pratham.livo.exception.BadRequestException;
import com.pratham.livo.exception.SecurityRiskException;
import com.pratham.livo.repository.UserRepository;
import com.pratham.livo.security.JwtService;
import com.pratham.livo.security.SecurityHelper;
import com.pratham.livo.service.AuthService;
import com.pratham.livo.service.SessionService;
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

import java.util.Set;

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

    @Override
    @Transactional
    public SignupResponseDto signup(SignupRequestDto signupRequestDto) {
        log.info("Registering user with email: {}",signupRequestDto.getEmail());
        if(userRepository.existsByEmail(signupRequestDto.getEmail())){
            throw new BadRequestException("An account already exists with this email.");
        }
        User user = User.builder()
                .name(signupRequestDto.getName()).email(signupRequestDto.getEmail())
                .passwordHash(passwordEncoder.encode(signupRequestDto.getPassword()))
                .roles(Set.of(Role.GUEST)).build();

        User savedUser = userRepository.save(user);
        log.info("Successfully registered user with email: {}",signupRequestDto.getEmail());
        return modelMapper.map(savedUser, SignupResponseDto.class);
    }

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
}
