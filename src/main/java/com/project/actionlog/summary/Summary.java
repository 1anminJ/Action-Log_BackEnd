package com.project.actionlog.summary;

import com.project.actionlog.user.User; // [중요] User 엔티티의 경로 확인
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "summaries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class) // 생성 시간 자동 기록
public class Summary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;   // 제목

    @Column(nullable = false, length = 2000) // 텍스트 길이에 맞게 조절
    private String summary; // 핵심 요약

    @Column(length = 2000)
    private String decisions; // 주요 결정 사항

    @Column(length = 2000)
    private String actionItems; // 할 일

    @CreatedDate // 엔티티 생성 시 시간 자동 저장
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // --- 연관 관계 ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Builder
    public Summary(String summary, String decisions, String actionItems, User user, String title) {
        this.summary = summary;
        this.decisions = decisions;
        this.actionItems = actionItems;
        this.user = user;
        this.title = title;
    }
}