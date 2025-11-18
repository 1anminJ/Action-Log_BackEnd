package com.project.actionlog.user.dto;


import lombok.Getter;
import lombok.NoArgsConstructor;

public class AuthDTOs {

    @Getter
    @NoArgsConstructor
    public static class SignupRequest {
        private String userId; // [추가]
        private String password;
        private String name; // [추가]
        private String email; // [추가]
    }

    @Getter
    @NoArgsConstructor
    public static class LoginRequest {
        private String userId; // [수정] email -> userId
        private String password;
    }

    @Getter
    public static class TokenResponse {
        private String accessToken;
        private String userId; // [수정] email -> userId

        public TokenResponse(String accessToken, String userId) {
            this.accessToken = accessToken;
            this.userId = userId;
        }
    }
}
