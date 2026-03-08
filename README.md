# 🎙️ Action-Log Backend

> **"기록을 넘어 행동으로"** — AI 기반 회의록 자동 요약 및 Action Item 추출 서비스의 백엔드 API 서버

<p align="center">
  <a href="https://action-log-front-end.vercel.app">🚀 서비스 바로가기</a> ·
  <a href="https://github.com/1anminJ/Action-Log_FrontEnd">💻 Frontend Repo</a> ·
  <a href="https://github.com/1anminJ/Action-Log">📄 프로젝트 소개 페이지</a>
</p>

<p align="center">
  <a href="https://www.youtube.com/watch?v=pxMV_wATEFg">
    <img src="https://img.youtube.com/vi/pxMV_wATEFg/maxresdefault.jpg" alt="Action-Log Demo Video" width="600">
  </a>
  <br>
  <em>▲ 클릭하면 데모 영상을 볼 수 있습니다</em>
</p>

---

## 📖 프로젝트 소개

Action-Log는 회의/강의 녹음 파일을 업로드하면 **AI가 핵심 요약 + 주요 결정사항 + Action Item(할 일 목록)** 을 자동으로 추출해주는 서비스입니다.

이 레포지토리는 Action-Log 서비스의 **Spring Boot 백엔드 API 서버**입니다.

---

## 🎯 기획 의도 및 목표

### Archiving → Productivity
기존 ClovaNote 등의 서비스가 **"정확한 기록(Archiving)"** 에 집중하는 것과 달리, Action-Log는 **"빠른 요약 및 행동 유도(Productivity)"** 를 핵심 가치로 삼고 있습니다.

### Actionable Insight
단순한 텍스트 변환을 넘어, 회의 직후 즉시 행동으로 옮길 수 있는 **Action Item(할 일 목록)** 을 제공하는 것을 핵심 목표로 삼았습니다.

---

## 👥 타겟 사용자 및 타서비스 비교 분석

**🎯 Target Audience**: `시간이 부족한 개발자` · `효율을 중시하는 기획자(PM)` · `회의/강의 요약이 필요한 학생`

| Features | Existing Services (ClovaNote 등) | **Action-Log (본 서비스)** |
|:--------:|:--------------------------------:|:------------------------:|
| **핵심 가치** | 정확한 기록 및 검색 (Archiving) | **빠른 요약 및 행동 유도 (Productivity)** |
| **결과물** | 긴 줄글 형태의 스크립트 | **3줄 요약 + Action Item 체크리스트** |
| **사용자 경험** | 다시 읽고 정리해야 함 (비효율) | **정리된 결론만 확인하면 됨 (효율)** |

---

## 🛠️ 기술 스택

| 구분 | 기술 |
|:----:|:----:|
| **Framework** | Spring Boot 3.5.x |
| **Language** | Java 17 |
| **비동기 처리** | Spring WebFlux (WebClient) |
| **인증/보안** | Spring Security + JWT (jjwt 0.11.5) |
| **Database** | MySQL + Spring Data JPA |
| **AI/API** | OpenAI Whisper API (STT) + GPT API (요약) |
| **API 문서** | Swagger (Springdoc-openapi 2.5.0) |
| **빌드** | Gradle |
| **배포** | Docker (Multi-stage Build) |

---

## 🏗️ 시스템 아키텍처

```
React(SPA) + Spring Boot (WebFlux) + OpenAI API
```

```
┌─────────────┐     음성 파일       ┌──────────────────┐
│   React     │  ──────────────▶  │  Spring Boot     │
│  (Frontend) │                   │  (Backend API)   │
│             │  ◀──────────────  │                  │
└─────────────┘   JSON 응답        └────────┬─────────┘
                                           │
                          ┌────────────────┼────────────────┐
                          ▼                ▼                ▼
                  ┌──────────────┐ ┌──────────────┐ ┌──────────┐
                  │ OpenAI       │ │ OpenAI       │ │  MySQL   │
                  │ Whisper API  │ │ GPT API      │ │  DB      │
                  │ (STT)        │ │ (요약/추출)    │ │          │
                  └──────────────┘ └──────────────┘ └──────────┘
```

**핵심 처리 흐름:**
1. 클라이언트가 음성 파일 업로드
2. **Whisper API** → 음성을 텍스트로 변환 (STT)
3. **GPT API** → 텍스트를 분석하여 3줄 요약 + 결정사항 + Action Item 추출
4. 결과를 **DB에 저장** 후 JSON 응답 반환

> ⚡ **Non-blocking I/O**: WebFlux의 `WebClient`를 사용하여 OpenAI API 호출 시 스레드가 블로킹되지 않도록 처리했습니다.

---

## 🔑 핵심 기술 포인트

### 01. Non-blocking I/O 기반 안정성 확보
대용량 오디오 파일 처리 시 발생할 수 있는 서버의 스레드 차단(Blocking)을 방지하기 위해 **Spring WebFlux (WebClient)** 를 도입했습니다. 이를 통해 다수의 사용자가 동시에 요약을 요청해도 서버가 멈추지 않고 안정적으로 처리합니다.

### 02. JSON Mode & 프롬프트 엔지니어링
LLM의 비정형 텍스트 반환 문제를 해결하기 위해 **JSON Mode**를 강제하는 프롬프트를 설계했습니다. 페르소나 부여와 제약 조건 명시를 통해 프론트엔드에서 별도 파싱 로직 없이 즉시 UI에 렌더링 가능한 데이터 정합성을 확보했습니다.

### 03. JWT 기반 Stateless 인증
Spring Security + JWT 토큰 방식으로 Stateless 인증을 구현했습니다. 사용자별 요약 히스토리 관리 및 삭제 시 소유권 검증을 수행합니다.

### 04. Docker Multi-stage Build
빌드 환경(Gradle)과 실행 환경(JDK)을 분리하여 최종 이미지 크기를 최소화했습니다.

---

## 📁 프로젝트 구조

```
src/main/java/com/project/actionlog/
├── ActionLogApplication.java          # 메인 애플리케이션
│
├── config/                            # 설정
│   ├── SecurityConfig.java            # Spring Security 설정 (CORS, JWT 필터 등)
│   ├── OpenAiConfig.java              # OpenAI WebClient Bean 설정
│   └── JpaAuditingConfig.java         # JPA Auditing (createdAt 자동 기록)
│
├── user/                              # 사용자 도메인
│   ├── User.java                      # 사용자 엔티티 (UserDetails 구현)
│   ├── UserRepository.java            # 사용자 JPA Repository
│   ├── AuthController.java            # 인증 API (회원가입, 로그인)
│   ├── AuthService.java               # 인증 비즈니스 로직
│   └── dto/
│       └── AuthDTOs.java              # 인증 관련 DTO
│
├── summary/                           # 요약 도메인
│   ├── Summary.java                   # 요약 엔티티 (제목, 요약, 결정사항, Action Items)
│   ├── SummaryRepository.java         # 요약 JPA Repository
│   ├── SummaryController.java         # 요약 API (업로드, 히스토리 조회, 삭제)
│   ├── SummaryService.java            # 핵심 비즈니스 로직 (Whisper + GPT 호출)
│   └── dto/
│       ├── SummaryResponse.java       # 요약 결과 응답 DTO
│       └── SummaryDTO.java            # 히스토리 관련 DTO
│
└── security/                          # JWT 보안
    ├── JwtTokenProvider.java          # JWT 토큰 생성/검증
    └── JwtAuthenticationFilter.java   # JWT 인증 필터
```

---

## 📊 ERD

```
┌──────────────────┐         ┌──────────────────┐
│      users       │         │    summaries     │
├──────────────────┤         ├──────────────────┤
│ id (PK)          │◄───┐    │ id (PK)          │
│ user_id (UQ)     │    └────│ user_id (FK)     │
│ password         │         │ title            │
│ name             │         │ summary          │
│ email (UQ)       │         │ decisions        │
│                  │         │ action_items     │
│                  │         │ created_at       │
└──────────────────┘         └──────────────────┘
```

---

## 📡 API 엔드포인트

### Auth API (인증)
| Method | Endpoint | Description | 인증 |
|:------:|:--------:|:-----------:|:----:|
| POST | `/api/auth/signup` | 회원가입 | ❌ |
| POST | `/api/auth/login` | 로그인 (JWT 토큰 발급) | ❌ |

### Summary API (요약)
| Method | Endpoint | Description | 인증 |
|:------:|:--------:|:-----------:|:----:|
| POST | `/api/summarize` | 음성 파일 업로드 → AI 요약 | ✅ |
| GET | `/api/summaries/me` | 내 요약 히스토리 조회 (최신순) | ✅ |
| DELETE | `/api/summaries/{id}` | 요약 히스토리 삭제 | ✅ |

---

## 📊 성과 지표 (KPI)

| 지표 | 수치 | 설명 |
|:----:|:----:|:----:|
| ⏱️ 시간 효율성 향상 | **95%** | 1시간 분량 회의 처리: **60분 → 3분**으로 단축 |
| 🔒 데이터 정합성 확보 | **99%** | JSON Mode 도입으로 구조화 데이터 변환 오류 해결 |
| 🎯 핵심 정보 재현율 | **92%** | Prompt Engineering으로 정보 누락 방지 및 환각 최소화 |

---

## 🚀 시작하기

### 사전 요구사항
- Java 17 이상
- MySQL 8.0
- OpenAI API Key

### 환경 변수 설정

`application.properties` 또는 환경변수로 아래 값을 설정��니다:

```properties
# Database
spring.datasource.url=jdbc:mysql://localhost:3306/actionlog
spring.datasource.username=your_username
spring.datasource.password=your_password

# OpenAI
openai.api.key=your_openai_api_key

# JWT
jwt.secret=your_jwt_secret_key
```

### 로컬 실행

```bash
./gradlew bootRun
```

### Docker 실행

```bash
docker build -t action-log-backend .
docker run -p 8080:8080 action-log-backend
```

### Swagger API 문서

서버 실행 후 아래 URL에서 확인:
```
http://localhost:8080/swagger-ui/index.html
```

---

## 💡 회고 (Retrospective)

### 🙆‍♂️ 배운 점 (Learned)
- 프롬프트 엔지니어링이 서비스 품질을 결정하는 핵심 기술임을 체감했습니다.
- AI 모델 성능만큼이나 AI와의 '소통 방식(JSON 강제 등)'이 중요함을 깨달았습니다.
- 비동기 처리(WebFlux)를 통해 대용량 요청을 효율적으로 관리하는 법을 익혔습니다.

### 🚀 향후 계획 (Future Plan)
- **화자 분리(Speaker Diarization)**: '누가' 말했는지 식별하여 업무 자동 할당
- **외부 협업 툴 연동**: Jira 티켓 생성, Slack 알림 등 파이프라인 확장
- **RAG 기반 회의 지식소**: 축적된 회의록 벡터 DB 구축 및 Q&A 챗봇 구현
- **WebSocket 스트리밍**: 실시간 음성-텍스트 변환 파이프라인 구축

---

## 👤 개발자

**한민정** — 기획, 설계, 백엔드/프론트엔드 개발, 배포

[![GitHub](https://img.shields.io/badge/GitHub-1anminJ-181717?style=flat-square&logo=github)](https://github.com/1anminJ)
[![LinkedIn](https://img.shields.io/badge/LinkedIn-1anminJ-0A66C2?style=flat-square&logo=linkedin)](https://linkedin.com/in/1anminJ)
[![Email](https://img.shields.io/badge/Email-mjeoung413@gmail.com-EA4335?style=flat-square&logo=gmail)](mailto:mjeoung413@gmail.com)

---

## 📄 Related Repositories

| Repository | Description |
|:----------:|:-----------:|
| [Action-Log_FrontEnd](https://github.com/1anminJ/Action-Log_FrontEnd) | React 프론트엔드 |
| [Action-Log](https://github.com/1anminJ/Action-Log) | 프로젝트 소개 페이지 |
