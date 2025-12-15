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

    // 2. GPT API 호출 (요약 및 추출)
    private Mono<SummaryResponse> callGptApi(String transcribedText) {
        String systemPrompt = "당신은 회의록을 전문적으로 분석하고 요약하는 AI 비서입니다. " +
                "주어진 회의 스크립트 텍스트를 분석하여, 다음 3가지 항목을 추출하고 반드시 지정된 JSON 형식으로만 응답해야 합니다. " +
                "JSON 외에 어떤 설명이나 인사말도 포함하지 마십시오.\n\n" +
                "--- 항목별 상세 지침 ---\n" +
                "1.  summary: 회의의 핵심 목적, 주요 논의, 그리고 전반적인 결론을 2-4문장으로 간결하게 요약합니다.\n" +
                "2.  decisions: 회의에서 명시적으로 '결정', '승인', '합의'된 사항들을 **불렛 포인트(-)를 사용한 단일 문자열**로 나열합니다. " +
                "예: \"- A 안건 승인\\n- B안은 12월 1일까지 보류\". " +
                "만약 명확한 결정 사항이 없으면 \"해당 없음\"을 값으로 합니다.\n" +
                "3.  actionItems: 회의 결과 실행해야 할 '할 일(Action Item)'을 불렛 포인트(-)를 사용한 단일 문자열로 나열합니다. " +
                "각 항목은 [담당자] [작업 내용] (~기한) 형식을 따라야 합니다. " +
                "담당자나 기한이 불명확할 경우 '미지정'으로 표기합니다. " +
                "예: \"- [홍길동] A 리포트 작성 (~11/20)\\n- [미지정] B 자료 조사 (~금주 말)\". " +
                "만약 할 일이 없으면 \"해당 없음\"을 값으로 합니다.\n\n" +
                "--- JSON 출력 형식 ---\n" +
                "{\n" +
                "  \"summary\": \"(여기에 2-4 문장의 핵심 요약)\",\n" +
                "  \"decisions\": \"(여기에 불렛 포인트를 사용한 결정 사항 목록)\",\n" +
                "  \"actionItems\": \"(여기에 [담당자] [작업 내용] (~기한) 형식의 할 일 목록)\"\n" +
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