package com.pratham.livo.security;

import com.pratham.livo.dto.auth.AccessTokenClaims;
import com.pratham.livo.dto.auth.AuthenticatedUser;
import com.pratham.livo.enums.Role;
import com.pratham.livo.exception.SessionNotFoundException;
import com.pratham.livo.utils.AccessTokenBlacklister;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final AccessTokenBlacklister accessTokenBlacklister;
    private final HandlerExceptionResolver handlerExceptionResolver;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        //get auth header
        String authHeader = request.getHeader("Authorization");
        //if auth header is null or invalid then go to next filter without authentication
        if(authHeader == null || !authHeader.startsWith("Bearer ")){
            filterChain.doFilter(request,response);
            return;
        }

        try{
            //if authentication doesn't exist
            if(SecurityContextHolder.getContext().getAuthentication() == null){
                //get the access token
                String accessToken = authHeader.substring(7);
                //get the claims from token
                AccessTokenClaims claims = jwtService.parseAccessToken(accessToken);

                //throw exception if token blacklisted
                if(accessTokenBlacklister.isBlacklisted(claims.getJti())){
                    throw new SessionNotFoundException("Session expired.");
                }

                //get the set of roles from token
                Set<Role> roles = claims.getRoles().stream()
                        .map(role -> Role.valueOf(role))
                        .collect(Collectors.toSet());

                //build authenticated user from the token
                AuthenticatedUser authenticatedUser = AuthenticatedUser.builder()
                        .id(claims.getUserId())
                        .name(claims.getName())
                        .email(claims.getEmail())
                        .roles(roles)
                        .build();

                //build authentication token using this user and its authorities
                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(authenticatedUser,null,authenticatedUser.getAuthorities());

                //set details in this authentication token
                authenticationToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));

                //set this authentication token in the security context
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
        }catch (Exception e){
            SecurityContextHolder.clearContext();
            handlerExceptionResolver.resolveException(request,response,null,e);
            return;
        }
        filterChain.doFilter(request,response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getServletPath().startsWith("/auth/") && !request.getServletPath().startsWith("/auth/logout");
    }
}
