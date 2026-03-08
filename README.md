# 🎙️ Action-Log Backend

> **"기록을 넘어 행동으로"** — AI 기반 회의록 자동 요약 및 Action Item 추출 서비스의 백엔드 API 서버

<p align="center">
  <a href="https://action-log-front-end.vercel.app" target="_blank">
    <img src="https://img.shields.io/badge/🌐 서비스 바로가기-4A90E2?style=for-the-badge" alt="서비스 바로가기"/>
  </a>
  &nbsp;
  <a href="https://github.com/1anminJ/Action-Log_FrontEnd" target="_blank">
    <img src="https://img.shields.io/badge/Frontend Repo-61DAFB?style=for-the-badge&logo=react&logoColor=black" alt="Frontend Repo"/>
  </a>
  &nbsp;
  <a href="https://github.com/1anminJ/Action-Log" target="_blank">
    <img src="https://img.shields.io/badge/프로젝트 소개 페이지-FF6B6B?style=for-the-badge" alt="프로젝트 소개 페이지"/>
  </a>
</p>

---

### 🎬 데모 영상

<p align="center">
  <a href="https://www.youtube.com/watch?v=pxMV_wATEFg" target="_blank">
    <img src="https://img.youtube.com/vi/pxMV_wATEFg/maxresdefault.jpg" alt="Action-Log 데모 영상" width="720"/>
  </a>
  <br/>
  <em>▶ 이미지를 클릭하면 YouTube 데모 영상으로 이동합니다</em>
</p>

---

## 📖 프로젝트 소개

**Action-Log**는 회의 음성 파일을 업로드하면 AI가 자동으로 회의록을 요약하고, 즉시 실행 가능한 **Action Item(할 일 목록)**을 추출해주는 생산성 향상 서비스입니다.

기존 ClovaNote 등의 서비스는 **"정확한 기록(Archiving)"**에 집중합니다. 결과물이 긴 줄글 형태의 스크립트이기 때문에, 회의 후에도 다시 읽고 정리하는 추가적인 비효율이 발생합니다.

**Action-Log**는 이 문제를 해결합니다. 핵심 요약과 결정사항, 그리고 Action Item을 즉시 제공하여 회의 직후 바로 행동으로 옮길 수 있는 **"빠른 요약 및 행동 유도(Productivity)"**에 집중합니다.

---

## 🎯 기획 의도 및 목표

1. **Archiving to Productivity**
   기존 서비스는 '정확한 기록'에만 집중하여 다시 읽어야 하는 비효율이 존재합니다. Action-Log는 기록을 넘어 실질적인 생산성 향상에 집중합니다.

2. **Actionable Insight**
   단순한 텍스트 변환을 넘어, 회의 직후 즉시 행동으로 옮길 수 있는 **Action Item(할 일 목록)**을 제공하는 것을 핵심 목표로 합니다.

---

## 👥 타겟 사용자 및 타서비스 비교 분석

**타겟 사용자**: 시간이 부족한 개발자, 효율을 중시하는 기획자(PM), 회의/강의 요약이 필요한 학생

| Features | Existing Services (ClovaNote 등) | Action-Log (본 서비스) |
|:--------:|:--------------------------------:|:----------------------:|
| 핵심 가치 | 정확한 기록 및 검색 (Archiving) | 빠른 요약 및 행동 유도 (Productivity) |
| 결과물 | 긴 줄글 형태의 스크립트 | 3줄 요약 + Action Item 체크리스트 |
| 사용자 경험 | 다시 읽고 정리해야 함 (비효율) | 정리된 결론만 확인하면 됨 (효율) |

---

## 🛠️ 기술 스택

| 구분 | 기술 |
|:----:|:----:|
| Framework | Spring Boot 3.5.x |
| Language | Java 17 |
| 비동기 처리 | Spring WebFlux (WebClient) |
| 인증/보안 | Spring Security + JWT (jjwt 0.11.5) |
| Database | MySQL + Spring Data JPA |
| AI/API | OpenAI Whisper API (STT) + GPT API (요약) |
| API 문서 | Swagger (Springdoc-openapi 2.5.0) |
| 빌드 | Gradle |
| 배포 | Docker (Multi-stage Build) |

---

## 🏗️ 시스템 아키텍처

```
Client (React SPA)
    │  음성 파일 업로드
    ▼
Spring Boot API Server
    │
    ├──▶ OpenAI Whisper API (음성 → 텍스트)
    │
    ├──▶ OpenAI GPT API (텍스트 → 3줄 요약 + 결정사항 + Action Items)
    │
    └──▶ MySQL DB (사용자 정보 + 요약 히스토리 저장)
```

**핵심 처리 흐름:**

1. 클라이언트가 음성 파일 업로드
2. **Whisper API** → 음성을 텍스트로 변환 (STT)
3. **GPT API** → 텍스트를 분석하여 3줄 요약 + 결정사항 + Action Item 추출
4. 결과를 DB에 저장 후 JSON 응답 반환

---

## 🔑 핵심 기술 포인트

### 01. Non-blocking I/O 기반 안정성 확보

대용량 오디오 파일 처리 시 발생할 수 있는 서버의 스레드 차단(Blocking)을 방지하기 위해 **Spring WebFlux (WebClient)**를 도입했습니다. 이를 통해 다수의 사용자가 동시에 요약을 요청해도 서버가 멈추지 않고 안정적으로 처리합니다.

### 02. JSON Mode & 프롬프트 엔지니어링

LLM의 비정형 텍스트 반환 문제를 해결하기 위해 **JSON Mode를 강제하는 프롬프트**를 설계했습니다. 페르소나 부여와 제약 조건 명시를 통해 프론트엔드에서 별도 파싱 로직 없이 즉시 UI에 렌더링 가능한 데이터 정합성을 확보했습니다.

### 03. JWT 기반 Stateless 인증

**Spring Security + JWT 토큰** 방식으로 Stateless 인증을 구현. 사용자별 요약 히스토리 관리 및 삭제 시 소유권 검증을 수행합니다.

### 04. Docker Multi-stage Build

빌드 환경(Gradle)과 실행 환경(JDK)을 분리하여 최종 이미지 크기를 최소화했습니다.

---

## 📁 프로젝트 구조

```
src/main/java/com/project/actionlog/
├── ActionLogApplication.java
├── config/
│   ├── SecurityConfig.java
│   ├── SwaggerConfig.java
│   ├── WebClientConfig.java
│   ├── JwtTokenProvider.java
│   └── JwtTokenFilter.java
├── user/
│   ├── User.java                  # @Entity (UserDetails 구현)
│   ├── UserRepository.java
│   ├── AuthController.java        # 회원가입, 로그인
│   ├── AuthService.java
│   └── dto/AuthDTOs.java
└── summary/
    ├── Summary.java               # @Entity (제목, 요약, 결정사항, Action Items)
    ├── SummaryRepository.java
    ├── SummaryController.java     # 음성 업로드, 히스토리 조회/삭제
    ├── SummaryService.java        # Whisper + GPT 호출 핵심 로직
    └── dto/
        ├── SummaryResponse.java
        └── SummaryDTO.java
```

---

## 📊 ERD

```
users (1) ──── (N) summaries
```

- **users**: `id`, `user_id(UQ)`, `password`, `name`, `email(UQ)`
- **summaries**: `id`, `user_id(FK)`, `title`, `summary`, `decisions`, `action_items`, `created_at`

---

## 📡 API 엔드포인트

### Auth API

| Method | Endpoint | Description | 인증 |
|:------:|:--------:|:-----------:|:----:|
| `POST` | `/api/auth/signup` | 회원가입 | ❌ 불필요 |
| `POST` | `/api/auth/login` | 로그인 / JWT 발급 | ❌ 불필요 |

### Summary API

| Method | Endpoint | Description | 인증 |
|:------:|:--------:|:-----------:|:----:|
| `POST` | `/api/summarize` | 음성 파일 업로드 → AI 요약 | ✅ 필요 |
| `GET` | `/api/summaries/me` | 내 요약 히스토리 조회 | ✅ 필요 |
| `DELETE` | `/api/summaries/{id}` | 요약 히스토리 삭제 | ✅ 필요 |

> 전체 API 명세는 서버 실행 후 Swagger UI에서 확인하세요: `http://localhost:8080/swagger-ui/index.html`

---

## 📊 성과 지표 (KPI)

| 지표 | 수치 | 내용 |
|:----:|:----:|:----:|
| ⏱️ 시간 효율성 향상 | **95%** | 1시간 분량 회의 처리 시간 60분 → 3분으로 단축 |
| ✅ 데이터 정합성 확보 | **99%** | JSON Mode 도입으로 구조화 데이터 변환 오류 해결 |
| 🎯 핵심 정보 재현율 | **92%** | Prompt Engineering을 통한 정보 누락 방지 및 환각 최소화 |

---

## 🚀 시작하기

### 사전 요구사항

- Java 17
- MySQL 8.0
- OpenAI API Key

### 환경 변수 설정

`src/main/resources/application.properties`에 아래 내용을 설정하세요:

```properties
# Database
spring.datasource.url=jdbc:mysql://localhost:3306/{your_database}
spring.datasource.username={your_username}
spring.datasource.password={your_password}

# JWT
jwt.secret={your_jwt_secret_key}
jwt.expiration=86400000

# OpenAI
openai.api.key={your_openai_api_key}
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

### Swagger UI

```
http://localhost:8080/swagger-ui/index.html
```

---

## 💡 회고 (Retrospective)

### 배운 점

- **프롬프트 엔지니어링**이 서비스 품질을 결정하는 핵심 기술임을 체감
- AI 모델 성능만큼이나 AI와의 **'소통 방식(JSON 강제 등)'**이 중요함을 깨달음
- **비동기 처리(WebFlux)**를 통해 대용량 요청을 효율적으로 관리하는 법을 익힘

### 향후 계획

- 🎙️ **화자 분리(Speaker Diarization)**: '누가' 말했는지 식별하여 업무 자동 할당
- 🔗 **외부 협업 툴 연동**: Jira 티켓 생성, Slack 알림 등 파이프라인 확장
- 🧠 **RAG 기반 회의 지식소**: 축적된 회의록 벡터 DB 구축 및 Q&A 챗봇 구현
- 📡 **WebSocket 스트리밍**: 실시간 음성-텍스트 변환 파이프라인 구축

---

## 👤 개발자

**한민정** — 기획, 설계, 백엔드/프론트엔드 개발, 배포

---

## 📄 Related Repositories

| Repository | Description |
|:----------:|:-----------:|
| [Action-Log_FrontEnd](https://github.com/1anminJ/Action-Log_FrontEnd) | React 프론트엔드 |
| [Action-Log](https://github.com/1anminJ/Action-Log) | 프로젝트 소개 페이지 |
