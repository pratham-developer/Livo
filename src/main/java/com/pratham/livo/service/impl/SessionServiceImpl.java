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
    public String createSession(Long userId, String refreshToken, String familyId) {
        //lock the user to prevent race conditions
        User user = userRepository.findByIdAndLock(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "user not found"
                ));
        List<Session> sessionList = sessionRepository.findByUserOrderByLastUsedAtAsc(user);
        int excess = sessionList.size() - SESSION_LIMIT + 1;
        if(excess>0){
            List<Session> toDeleteList = sessionList.subList(0,excess);
            List<String> jtiList = toDeleteList.stream().map(session -> session.getJti()).toList();
            sessionRepository.deleteAllInBatch(toDeleteList); //auto executes sql query at the same instant
            //bypasses persistence context, so flush is not required
            accessTokenBlacklister.blacklistBatch(jtiList);
        }

        String refreshTokenHash = refreshTokenHasher.hash(refreshToken);
        String jti = UUID.randomUUID().toString();
        Session toSave = Session.builder()
                .user(user).refreshTokenHash(refreshTokenHash).familyId(familyId)
                .jti(jti).lastUsedAt(LocalDateTime.now()).build();
        sessionRepository.save(toSave);
        return jti;
    }

    @Override
    @Transactional
    public void deleteSession(Long userId, String familyId) {
        Optional<Session> optionalSession = sessionRepository.findByUserIdAndFamilyId(userId, familyId);
        if(optionalSession.isEmpty()) return;

        Session session = optionalSession.get();
        String jti = session.getJti();
        sessionRepository.delete(session);
        sessionRepository.flush();
        accessTokenBlacklister.blacklist(jti);
    }

    @Override
    @Transactional
    public LoginResponseDto refreshSession(Long userId, String refreshToken, String familyId) {

        //fetch the session along with its user using the userId and familyId
        Session session = sessionRepository.findSessionWithUser(userId,familyId).orElse(null);
        if(session == null){
            //possibly session got deleted due to session limit
            return null;
        }

        //hash the incoming refresh token
        String refreshTokenHash = refreshTokenHasher.hash(refreshToken);

        if(!refreshTokenHash.equals(session.getRefreshTokenHash())){
            if (session.getLastUsedAt().isAfter(LocalDateTime.now().minusSeconds(20))) {
                // likely a race condition
                return null;
            }
            //possibly an attack to use old token
            List<String> jtiList = sessionRepository.findAllJti(userId);
            sessionRepository.deleteAllSessionsForUser(userId);
            accessTokenBlacklister.blacklistBatch(jtiList);
            return null;
        }

        //get old jti
        String jtiToBlacklist = session.getJti();

        //generate new jti
        String newJti = UUID.randomUUID().toString();

        //generate new access(with new jti) and refresh token
        String newAccessToken = jwtService.generateAccessToken(session.getUser(),newJti);
        String newRefreshToken = jwtService.generateRefreshToken(session.getUser().getId(),session.getFamilyId());

        //hash the new refresh token
        String newRefreshTokenHash = refreshTokenHasher.hash(newRefreshToken);

        //update the session
        session.setJti(newJti);
        session.setRefreshTokenHash(newRefreshTokenHash);
        session.setLastUsedAt(LocalDateTime.now());

        //save the session
        sessionRepository.save(session);
        sessionRepository.flush();

        //blacklist
        accessTokenBlacklister.blacklist(jtiToBlacklist);

        //return tokens
        return LoginResponseDto.builder()
                .accessToken(newAccessToken).refreshToken(newRefreshToken)
                .build();
    }
}
