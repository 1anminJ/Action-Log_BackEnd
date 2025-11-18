package com.project.actionlog.user;

import com.project.actionlog.user.dto.AuthDTOs;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "인증 (Auth)")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "회원가입")
    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody AuthDTOs.SignupRequest request) { // [수정]
        try {
            authService.signup(request);
            return ResponseEntity.status(HttpStatus.CREATED).body("회원가입 성공");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "로그인 (토큰 발급)")
    @PostMapping("/login")
    public ResponseEntity<AuthDTOs.TokenResponse> login(@RequestBody AuthDTOs.LoginRequest request) { // [수정]
        AuthDTOs.TokenResponse response = authService.login(request); // [수정]
        return ResponseEntity.ok(response);
    }
}