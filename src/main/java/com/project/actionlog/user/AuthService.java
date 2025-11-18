package com.project.actionlog.user;

import com.project.actionlog.config.JwtTokenProvider;
import com.project.actionlog.user.dto.AuthDTOs;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Lazy JwtTokenProvider jwtTokenProvider,
            @Lazy AuthenticationManager authenticationManager
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.authenticationManager = authenticationManager;
    }

    // 1. 회원가입
    @Transactional
    public Long signup(AuthDTOs.SignupRequest request) {
        // [수정] 아이디, 이메일 중복 검사
        if (userRepository.existsByUserId(request.getUserId())) {
            throw new IllegalArgumentException("이미 가입된 아이디입니다.");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }

        // [수정] 모든 필드 저장
        User user = User.builder()
                .userId(request.getUserId())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .email(request.getEmail())
                .build();

        return userRepository.save(user).getId();
    }

    // 2. 로그인 (토큰 발급)
    @Transactional
    public AuthDTOs.TokenResponse login(AuthDTOs.LoginRequest request) {
        // [수정] email -> userId로 인증 토큰 생성
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(request.getUserId(), request.getPassword());

        Authentication authentication = authenticationManager.authenticate(authenticationToken);

        String accessToken = jwtTokenProvider.generateToken(authentication);

        return new AuthDTOs.TokenResponse(accessToken, authentication.getName());
    }

    // 3. Spring Security가 email(이제는 userId)로 User를 찾는 데 사용할 메소드
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // [수정] loadUserByUsername이 실제로는 userId로 찾도록 변경
        return userRepository.findByUserId(username)
                .orElseThrow(() -> new UsernameNotFoundException("해당 아이디를 찾을 수 없습니다: " + username));
    }
}