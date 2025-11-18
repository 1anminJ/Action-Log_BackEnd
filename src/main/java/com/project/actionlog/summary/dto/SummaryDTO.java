package com.project.actionlog.summary.dto;

import com.project.actionlog.summary.Summary;
import lombok.Getter;

import java.time.LocalDateTime;

public class SummaryDTO {

    @Getter
    public static class HistoryResponse {
        private final Long id;
        private final String title;
        private final String summary;
        private final String decisions;
        private final String actionItems;
        private final LocalDateTime createdAt;

        public HistoryResponse(Summary summary) {
            this.id = summary.getId();
            this.title = summary.getTitle();
            this.summary = summary.getSummary();
            this.decisions = summary.getDecisions();
            this.actionItems = summary.getActionItems();
            this.createdAt = summary.getCreatedAt();
        }
    }
}