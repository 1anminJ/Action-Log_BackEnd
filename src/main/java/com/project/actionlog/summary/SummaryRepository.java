package com.project.actionlog.summary;

import com.project.actionlog.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SummaryRepository extends JpaRepository<Summary, Long> {
    // 특정 사용자의 모든 요약본을 최신순으로 정렬하여 조회
    List<Summary> findAllByUserOrderByCreatedAtDesc(User user);
}