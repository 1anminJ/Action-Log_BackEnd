package com.project.actionlog.summary; // (패키지 경로는 본인 것에 맞게 확인)

import com.project.actionlog.summary.dto.SummaryDTO;
import com.project.actionlog.user.User;
import com.project.actionlog.summary.dto.SummaryResponse;
import com.project.actionlog.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SummaryService {

    private final WebClient openAiWebClient;
    private final UserRepository userRepository;
    private final SummaryRepository summaryRepository;

    // [수정] Authentication(로그인 정보)과 title 파라미터 추가
    public Mono<SummaryResponse> summarizeAudio(MultipartFile audioFile, String title, Authentication authentication) {

        // 1. 로그인한 사용자 정보 찾기 (userId로)
        String userId = authentication.getName(); // JWT 토큰에서 userId를 가져옴
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));

        // 2. Whisper API 호출 (파일 -> 텍스트)
        return callWhisperApi(audioFile)
                // 3. GPT API 호출 (텍스트 -> 요약)
                .flatMap(this::callGptApi)
                // 4. [수정] 요약 결과를 DB에 저장
                .flatMap(summaryResponse -> {
                    Summary summary = Summary.builder()
                            .user(user)
                            .summary(summaryResponse.getSummary())
                            .decisions(summaryResponse.getDecisions())
                            .actionItems(summaryResponse.getActionItems())
                            .title(title)
                            .build();
                    summaryRepository.save(summary);

                    return Mono.just(summaryResponse); // DTO를 다시 반환
                });
    }

    // [추가] 히스토리 조회 서비스
    @Transactional(readOnly = true)
    public List<SummaryDTO.HistoryResponse> getHistory(Authentication authentication) {
        String userId = authentication.getName();
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));

        return summaryRepository.findAllByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(SummaryDTO.HistoryResponse::new) // DTO로 변환
                .collect(Collectors.toList());
    }

    // --- [바코 1단계] 삭제 로직 추가 ---
    public void deleteSummary(Long summaryId, Authentication authentication) {
        // 1. 로그인한 사용자 찾기
        String userId = authentication.getName();
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));

        // 2. 삭제할 요약본 찾기
        Summary summary = summaryRepository.findById(summaryId)
                .orElseThrow(() -> new EntityNotFoundException("요약본을 찾을 수 없습니다: " + summaryId));

        // 3. [보안] 요약본의 주인(summary.getUser())과 로그인한 사용자(user)가 일치하는지 확인
        if (!Objects.equals(summary.getUser().getId(), user.getId())) {
            throw new AccessDeniedException("삭제 권한이 없습니다.");
        }

        // 4. 삭제 실행
        summaryRepository.delete(summary);
    }


    // 1. Whisper API 호출 (STT) - (이하 로직 동일)
    private Mono<String> callWhisperApi(MultipartFile audioFile) {
        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        try {
            bodyBuilder.part("file", new ByteArrayResource(audioFile.getBytes()) {
                @Override
                public String getFilename() {
                    return audioFile.getOriginalFilename();
                }
            });
            bodyBuilder.part("model", "whisper-1");
            bodyBuilder.part("response_format", "text");

        } catch (IOException e) {
            return Mono.error(new RuntimeException("파일 처리 중 오류 발생", e));
        }

        return openAiWebClient.post()
                .uri("/audio/transcriptions")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromValue(bodyBuilder.build()))
                .retrieve()
                .bodyToMono(String.class);
    }

    // 2. GPT API 호출 (요약 및 추출) - (이하 로직 동일)
    private Mono<SummaryResponse> callGptApi(String transcribedText) {
        String systemPrompt = "너는 회의록을 전문적으로 요약하는 AI 비서야. " +
                "주어진 텍스트를 분석해서 '핵심 요약', '주요 결정 사항', '할 일 목록' 3가지로 명확히 구분해줘. " +
                "다른 설명 없이 아래와 같은 JSON 형식으로만 응답해줘.\n" +
                "{\n" +
                "  \"summary\": \"회의의 핵심 요약 내용...\",\n" +
                "  \"decisions\": \"주요 결정 사항들...\",\n" +
                "  \"actionItems\": \"담당자별 할 일 목록...\"\n" +
                "}";

        Map<String, Object> requestBody = Map.of(
                "model", "gpt-3.5-turbo-1106",
                "response_format", Map.of("type", "json_object"),
                "messages", new Object[]{
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", transcribedText)
                }
        );

        return openAiWebClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(requestBody))
                .retrieve()
                .bodyToMono(Map.class)
                .map(responseMap -> {
                    Map<String, Object> choice = ((java.util.List<Map<String, Object>>) responseMap.get("choices")).get(0);
                    Map<String, Object> message = (Map<String, Object>) choice.get("message");
                    String jsonContent = (String) message.get("content");

                    try {
                        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        return mapper.readValue(jsonContent, SummaryResponse.class);
                    } catch (Exception e) {
                        return new SummaryResponse("파싱 실패", jsonContent, "오류 발생");
                    }
                });
    }
}