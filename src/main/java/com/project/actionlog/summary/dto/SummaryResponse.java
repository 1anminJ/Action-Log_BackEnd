package com.project.actionlog.summary.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SummaryResponse {
    private String summary; // 핵심 요약
    private String decisions; // 주요 결정 사항
    private String actionItems; // 할 일 목록
}
