package com.project.actionlog.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// OncePerRequestFilter: 모든 요청마다 실행되는 필터
// JwtTokenFilter.java
@RequiredArgsConstructor
public class JwtTokenFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // --- [디버깅 로그 추가] ---
        String path = request.getRequestURI();
        System.out.println("--- JWT Filter ---");
        System.out.println("Request Path: " + path);
        // --- ---

        String token = resolveToken(request);

        // --- [디버깅 로그 추가] ---
        if (token != null) {
            System.out.println("Token: " + token);
        } else {
            System.out.println("Token: null (토큰 없음!)");
        }
        // --- ---

        if (token != null && jwtTokenProvider.validateToken(token)) {
            // --- [디버깅 로그 추가] ---
            System.out.println("Token Validation: SUCCESS");
            // --- ---
            Authentication authentication = jwtTokenProvider.getAuthentication(token);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } else if (token != null) {
            // --- [디버깅 로그 추가] ---
            System.out.println("Token Validation: FAILED (토큰 유효하지 않음)");
            // --- ---
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        // --- [디G버깅 로그 추가] ---
        if (bearerToken != null) {
            System.out.println("Authorization Header: " + bearerToken);
        } else {
            System.out.println("Authorization Header: null (헤더 없음!)");
        }
        // --- ---
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}