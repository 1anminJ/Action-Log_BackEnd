package com.project.actionlog.summary;

import com.project.actionlog.summary.dto.SummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SummaryService {

    private final WebClient openAiWebClient;

    // 최종 컨트롤러가 호출할 메인 메소드
    public Mono<SummaryResponse> summarizeAudio(MultipartFile audioFile) {
        // 1. Whisper API 호출 (파일 -> 텍스트)
        return callWhisperApi(audioFile)
                // 2. GPT API 호출 (텍스트 -> 요약)
                .flatMap(this::callGptApi);
    }

    // 1. Whisper API 호출 (STT)
    private Mono<String> callWhisperApi(MultipartFile audioFile) {
        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        try {
            // 파일을 ByteArrayResource로 변환하여 추가
            bodyBuilder.part("file", new ByteArrayResource(audioFile.getBytes()) {
                @Override
                public String getFilename() {
                    return audioFile.getOriginalFilename();
                }
            });
            bodyBuilder.part("model", "whisper-1");
            bodyBuilder.part("response_format", "text"); // 텍스트로 바로 받음

        } catch (IOException e) {
            return Mono.error(new RuntimeException("파일 처리 중 오류 발생", e));
        }

        return openAiWebClient.post()
                .uri("/audio/transcriptions")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromValue(bodyBuilder.build()))
                .retrieve()
                .bodyToMono(String.class); // 텍스트(String)로 응답을 받음
    }

    // 2. GPT API 호출 (요약 및 추출)
    private Mono<SummaryResponse> callGptApi(String transcribedText) {
        // [핵심] GPT에게 보낼 프롬프트 (명령어)
        String systemPrompt = "너는 회의록을 전문적으로 요약하는 AI 비서야. " +
                "주어진 텍스트를 분석해서 '핵심 요약', '주요 결정 사항', '할 일 목록' 3가지로 명확히 구분해줘. " +
                "다른 설명 없이 아래와 같은 JSON 형식으로만 응답해줘.\n" +
                "{\n" +
                "  \"summary\": \"회의의 핵심 요약 내용...\",\n" +
                "  \"decisions\": \"주요 결정 사항들...\",\n" +
                "  \"actionItems\": \"담당자별 할 일 목록...\"\n" +
                "}";

        Map<String, Object> requestBody = Map.of(
                "model", "gpt-3.5-turbo-1106", // gpt-4-turbo가 더 좋지만, 3.5가 빠름
                "response_format", Map.of("type", "json_object"), // JSON으로 응답받기!
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
                .bodyToMono(Map.class) // 응답을 Map으로 받음
                .map(responseMap -> {
                    // GPT 응답 구조 파싱 (choices[0].message.content)
                    Map<String, Object> choice = ((java.util.List<Map<String, Object>>) responseMap.get("choices")).get(0);
                    Map<String, Object> message = (Map<String, Object>) choice.get("message");
                    String jsonContent = (String) message.get("content");

                    // GPT가 JSON 형식의 "문자열"을 반환했으므로, 이걸 SummaryResponse 객체로 파싱
                    // (이 부분은 Json 파싱 라이브러리(Jackson 등)를 쓰는 게 더 좋지만, 일단 간단하게...)
                    // 실제로는 String을 SummaryResponse.class로 변환해야 함
                    // 여기서는 GPT가 SummaryResponse 형식의 JSON 문자열을 반환한다고 가정

                    // *** 여기서 jsonContent(String)를 SummaryResponse 객체로 변환해야 합니다 ***
                    // Spring Boot는 기본으로 Jackson을 내장하고 있으므로, ObjectMapper를 주입받아 사용하면 됩니다.
                    // 여기서는 임시로 간단히 구성합니다.
                    // 실제 코드에서는 ObjectMapper를 사용하세요.

                    // 임시 코드 (실제로는 ObjectMapper 사용 필요)
                    // 예시: return new ObjectMapper().readValue(jsonContent, SummaryResponse.class);
                    // 지금은 간단히 수동으로 파싱해봅니다. (매우 불안정)

                    // GPT가 JSON 객체를 반환했다고 가정하고 파싱 (모델이 json_object 지원 시)
                    try {
                        // com.fasterxml.jackson.databind.ObjectMapper 사용
                        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        return mapper.readValue(jsonContent, SummaryResponse.class);
                    } catch (Exception e) {
                        // 파싱 실패 시
                        return new SummaryResponse("파싱 실패", jsonContent, "오류 발생");
                    }
                });
    }
}