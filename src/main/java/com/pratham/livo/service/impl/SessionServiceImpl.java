package com.pratham.livo.service.impl;

import com.pratham.livo.dto.auth.LoginResponseDto;
import com.pratham.livo.entity.Session;
import com.pratham.livo.entity.User;
import com.pratham.livo.exception.ResourceNotFoundException;
import com.pratham.livo.repository.SessionRepository;
import com.pratham.livo.repository.UserRepository;
import com.pratham.livo.security.JwtService;
import com.pratham.livo.service.SessionService;
import com.pratham.livo.utils.AccessTokenBlacklister;
import com.pratham.livo.utils.RefreshTokenHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {


    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Value("${session.count.max:1}")
    private int SESSION_LIMIT;

    private final SessionRepository sessionRepository;
    private final RefreshTokenHasher refreshTokenHasher;
    private final AccessTokenBlacklister accessTokenBlacklister;

    @Override
    @Transactional
    public String createSession(Long userId, String refreshToken) {
        //lock the user to prevent race conditions
        User user = userRepository.findByIdAndLock(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "user not found"
                ));
        List<Session> sessionList = sessionRepository.findByUserOrderByLastUsedAtAsc(user);
        if(sessionList.size()>=SESSION_LIMIT){
            Session toDelete = sessionList.getFirst();
            accessTokenBlacklister.blacklist(toDelete.getJti());
            sessionRepository.delete(toDelete);
            sessionRepository.flush(); //force delete now
        }

        String refreshTokenHash = refreshTokenHasher.hash(refreshToken);
        String jti = UUID.randomUUID().toString();
        Session toSave = Session.builder()
                .user(user).refreshTokenHash(refreshTokenHash)
                .jti(jti).lastUsedAt(LocalDateTime.now()).build();
        sessionRepository.save(toSave);
        return jti;
    }

    @Override
    @Transactional
    public void deleteSession(Long userId, String refreshToken) {
        String refreshTokenHash = refreshTokenHasher.hash(refreshToken);

        Optional<Session> optionalSession = sessionRepository.findByUserIdAndRefreshTokenHash(userId, refreshTokenHash);
        if(optionalSession.isEmpty()) return;

        Session session = optionalSession.get();
        String jti = session.getJti();
        sessionRepository.delete(session);
        accessTokenBlacklister.blacklist(jti);
    }

    @Override
    @Transactional
    public LoginResponseDto refreshSession(Long userId, String refreshToken) {

        //hash the incoming refresh token
        String refreshTokenHash = refreshTokenHasher.hash(refreshToken);

        //fetch the session along with its user using the userId and hash
        Session session = sessionRepository.findSessionWithUser(userId,refreshTokenHash).orElse(null);
        if(session == null){
            //possibly an attack
            List<String> jtiList = sessionRepository.findAllJti(userId);
            accessTokenBlacklister.blacklistBatch(jtiList);
            sessionRepository.deleteAllSessionsForUser(userId);
            return null;
        }

        //blacklist old jti
        accessTokenBlacklister.blacklist(session.getJti());

        //generate new jti
        String newJti = UUID.randomUUID().toString();

        //generate new access(with new jti) and refresh token
        String newAccessToken = jwtService.generateAccessToken(session.getUser(),newJti);
        String newRefreshToken = jwtService.generateRefreshToken(session.getUser().getId());

        //hash the new refresh token
        String newRefreshTokenHash = refreshTokenHasher.hash(newRefreshToken);

        //update the session
        session.setJti(newJti);
        session.setRefreshTokenHash(newRefreshTokenHash);
        session.setLastUsedAt(LocalDateTime.now());

        //save the session
        sessionRepository.save(session);

        //return tokens
        return LoginResponseDto.builder()
                .accessToken(newAccessToken).refreshToken(newRefreshToken)
                .build();
    }
}
