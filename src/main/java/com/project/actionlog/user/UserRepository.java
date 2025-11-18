package com.project.actionlog.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // [수정] email 대신 userId로 유저를 찾음
    Optional<User> findByUserId(String userId);

    // [추가] 아이디 중복 검사용
    boolean existsByUserId(String userId);

    // [유지] 이메일 중복 검사용
    boolean existsByEmail(String email);
}