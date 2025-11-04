package com.project.actionlog.summary;

import com.project.actionlog.summary.dto.SummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class SummaryController {

    private final SummaryService summaryService;

    // [수정된 부분] consumes 와 @Operation, @Parameter 추가
    @Operation(summary = "음성 파일 요약", description = "오디오 파일(mp3, m4a 등)을 업로드하면 AI가 요약/결정사항/할일을 추출합니다.")
    @PostMapping(value = "/summarize", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<SummaryResponse>> summarizeAudio(
            @Parameter(description = "요약할 오디오 파일", required = true)
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            SummaryResponse errorResponse = new SummaryResponse("파일이 비어있습니다.", "", "");
            return Mono.just(ResponseEntity.badRequest().body(errorResponse));
        }

        return summaryService.summarizeAudio(file)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    SummaryResponse errorResponse = new SummaryResponse("처리 중 오류 발생: " + e.getMessage(), "", "");
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
                });
    }
}