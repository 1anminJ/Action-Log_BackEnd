package com.project.actionlog.summary;

import com.project.actionlog.summary.dto.SummaryDTO;
import com.project.actionlog.summary.dto.SummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import java.util.List;

@Tag(name = "요약 (Summary)")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SummaryController {

    private final SummaryService summaryService;

    @Operation(summary = "음성 파일 요약 (인증 필요)",
            description = "오디오 파일을 업로드하면 AI가 요약/결정사항/할일을 추출하고, 결과를 사용자의 히스토리에 저장합니다.",
            security = { @SecurityRequirement(name = "BearerAuth") })
    @PostMapping(value = "/summarize", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<SummaryResponse>> summarizeAudio(
            @Parameter(description = "요약할 오디오 파일", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "요약본 제목", required = true)
            @RequestParam("title") String title, // [추가]
            Authentication authentication
    ) {

        if (file.isEmpty()) {
            SummaryResponse errorResponse = new SummaryResponse("파일이 비어있습니다.", "", "");
            return Mono.just(ResponseEntity.badRequest().body(errorResponse));
        }

        // [수정] title을 서비스로 전달
        return summaryService.summarizeAudio(file, title, authentication)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    SummaryResponse errorResponse = new SummaryResponse("처리 중 오류 발생: " + e.getMessage(), "", "");
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
                });
    }

    @Operation(summary = "내 요약 히스토리 조회 (인증 필요)",
            description = "로그인한 사용자의 모든 요약 히스토리를 최신순으로 조회합니다.",
            security = { @SecurityRequirement(name = "BearerAuth") })
    @GetMapping("/summaries/me")
    public ResponseEntity<List<SummaryDTO.HistoryResponse>> getMyHistory(
            Authentication authentication
    ) {
        List<SummaryDTO.HistoryResponse> history = summaryService.getHistory(authentication);
        return ResponseEntity.ok(history);
    }

    @Operation(summary = "요약 히스토리 삭제 (인증 필요)",
            description = "로그인한 사용자가 자신의 요약 히스토리(ID 기준)를 삭제합니다.",
            security = { @SecurityRequirement(name = "BearerAuth") })
    @DeleteMapping("/summaries/{id}")
    public ResponseEntity<String> deleteSummary(
            @Parameter(description = "삭제할 요약본의 ID", required = true)
            @PathVariable("id") Long summaryId,
            Authentication authentication
    ) {
        try {
            summaryService.deleteSummary(summaryId, authentication);
            return ResponseEntity.ok("요약본이 삭제되었습니다.");
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("삭제 중 오류가 발생했습니다.");
        }
    }
}