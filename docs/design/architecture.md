# server-foundation-template 아키텍처 설계

| 항목 | 내용 |
| --- | --- |
| 상태 | Draft v0.1 — 리뷰 대기 |
| 대상 | 이 템플릿으로 새 서버를 시작할 개발자, 표준을 유지할 리뷰어 |
| 전제 스택 | Kotlin 2.x · Spring Boot 3.x (Web MVC) · JDK 21 · Gradle Kotlin DSL |
| 함께 보기 | [설계 문서 페이지](https://claude.ai/code/artifact/87868b41-ec72-41d6-a60a-967c91e300a4) · [설계 캔버스](https://claude.ai/code/artifact/c67eedeb-8e3a-4dd0-bbd2-b08a359ae663) |

---

## 1. 배경

새 서버를 시작할 때마다 같은 일이 반복된다. 응답 JSON 모양을 다시 합의하고, 에러 코드 규칙을 다시 정하고, Swagger를 붙였다가 문서가 실제 응답과 어긋나면서 결국 아무도 안 믿게 된다.

세 가지가 각각 따로 관리되기 때문이다.

1. **실제 응답** — 컨트롤러와 예외 핸들러가 만든다.
2. **API 문서** — 사람이 어노테이션으로 따로 적는다.
3. **에러 코드 목록** — 위키나 노션에 손으로 적혀 있다.

셋이 별개인 한, 어긋나는 건 시간 문제다. 이 템플릿의 설계 목표는 **셋을 하나의 출처에서 파생시키는 것**이다.

## 2. 목표와 비목표

### 목표

- 모든 JSON 응답이 **같은 최상위 구조**를 가진다. 예외도 예외 없이.
- 에러 코드가 **코드 안 한 곳**에만 존재하고, 응답·문서·테스트가 모두 그것을 읽는다.
- API 문서가 **빌드 산출물**이고, 실제 응답과 어긋나면 **CI가 막는다**.
- 템플릿을 복제한 사람이 **표준을 지키려고 애쓸 필요가 없다**. 기본 경로가 곧 표준이다.

### 비목표

- 인증·인가 구현 — 훅 지점과 401/403 공통 코드만 준비한다.
- 데이터 접근 계층 선택 (JPA / jOOQ / Exposed) — 파운데이션은 이 선택에 의존하지 않는다.
- 배포·인프라, 메시징·비동기, 멀티테넌시.
- WebFlux 지원 — 표준화를 거는 훅(Filter / Interceptor / Advice)의 구성이 달라 별도 설계가 필요하다.

## 3. 설계 원칙

### 3.1 컨트롤러는 봉투를 모른다

컨트롤러는 도메인 DTO를 그대로 반환한다. 표준 응답으로 감싸는 일은 인프라 계층에서 일어난다.

```kotlin
// 이렇게 쓴다
@GetMapping("/{userId}")
fun get(@PathVariable userId: Long): UserResponse = userService.get(userId)

// 이렇게 쓰지 않는다
@GetMapping("/{userId}")
fun get(@PathVariable userId: Long): ApiResponse<UserResponse> =
    ApiResponse.success(userService.get(userId))
```

두 번째 형태를 허용하면 봉투를 씌우는 책임이 200개 컨트롤러 메서드로 퍼진다. 하나만 빠뜨려도 클라이언트는 두 가지 파싱 경로를 가져야 한다.

봉투를 **만드는** 곳은 정확히 세 군데이며 셋 다 `foundation-web` 안에 있다 — [§5.2](#52-봉투를-만드는-곳은-세-군데다) 참고.

### 3.2 에러 코드는 단일 레지스트리에서 나온다

`ErrorCode`가 **코드 문자열 · HTTP 상태 · 메시지 키**를 한 덩어리로 들고 있다. 이 셋이 흩어지면 "코드는 있는데 상태가 안 맞는" 응답이 나온다.

런타임 응답, OpenAPI 문서, 계약 테스트가 전부 이 레지스트리 하나를 읽는다.

### 3.3 문서는 생성하고, 별도로 검증한다

springdoc이 코드에서 OpenAPI를 뽑는다. 하지만 원칙 3.1 때문에 springdoc은 컨트롤러 시그니처만 보고 `UserResponse`를 문서에 적는다 — 실제로 나가는 건 `ApiResponse<UserResponse>`다. **이 간극이 이 템플릿이 존재하는 이유다.** 커스터마이저가 문서 쪽에 봉투를 되돌려 넣고, 별도의 응답 검증기가 실제 응답을 생성된 스펙에 대조한다 ([§7.4](#74-검증은-두-겹이다)).

## 4. 모듈 구조

Gradle 멀티모듈. 화살표는 의존 방향이다.

```
                    :sample-api            ← 복제해서 시작하는 참조 구현
                         │
        ┌────────────────┼────────────────┐
        ▼                ▼                ▼
 :foundation-web  :foundation-docs  :foundation-observability
        └────────────────┼────────────────┘
                         ▼
                  :foundation-core        ← Spring 의존 없는 순수 계약

 :foundation-test  ← 모든 모듈이 testImplementation 으로만 참조
```

| 모듈 | 책임 | 비고 |
| --- | --- | --- |
| `foundation-core` | `ApiResponse`, `PageResponse`, `ErrorCode`, `BusinessException` | Spring 의존 0. 계약 그 자체 |
| `foundation-web` | 봉투 Advice, 전역 예외 핸들러, `/error` 컨트롤러, TraceId·로깅 필터, 페이징 리졸버 | 표준화가 걸리는 지점 |
| `foundation-docs` | springdoc 커스터마이저, `@ApiErrorCodes`, Swagger UI 프리셋 | core의 에러 코드를 문서로 번역 |
| `foundation-observability` | 구조화 로그 인코더, MDC 키 정의, 마스킹 규칙 | web의 필터가 이 규칙을 가져다 쓴다 |
| `foundation-test` | 봉투 단언 헬퍼, `ErrorCodeContract` 검사기, REST Docs·스펙 검증 지원 | `testImplementation`으로만 참조 |
| `sample-api` | 위를 전부 쓰는 얇은 참조 구현 | 새 서비스의 출발점 |

`foundation-core`에 Spring 의존을 넣지 않는 이유: 계약이 프레임워크보다 오래 살아야 한다. 배치 작업이나 다른 런타임에서도 같은 `ErrorCode`를 쓸 수 있어야 한다.

그래서 **`ErrorCode.status`의 타입은 `HttpStatus`가 아니라 `Int`다.** `org.springframework.http.HttpStatus`는 `spring-web`에 있어서, 그것을 쓰는 순간 "Spring 의존 0"이 깨진다. 숫자 → `HttpStatus` 변환은 `foundation-web`이 한다.

> **열린 결정.** 팀 규모가 작으면 6개 모듈이 과할 수 있다. 단일 모듈 + 패키지 분리로 줄여도 경계는 동일하게 유지된다 — 다만 `foundation-core`의 "Spring 의존 없음"을 빌드가 강제해 주지 못한다.

## 5. 요청 라이프사이클

### 5.1 여덟 지점

요청 하나가 지나가는 지점을 여덟 개로 고정한다. **이 여덟 지점 밖에서는 응답 형식을 손대지 않는다.**

| # | 지점 | 하는 일 |
| --- | --- | --- |
| 01 | `TraceIdFilter` | 요청당 traceId를 만들거나 `traceparent`·`X-Request-Id`를 이어받는다 → MDC + 응답 헤더 `X-Trace-Id` |
| 02 | `RequestLoggingFilter` | 본문 캐싱 래퍼, 마스킹 후 한 줄 구조화 로그 |
| 03 | `SecurityFilterChain` *(선택)* | 인증. 실패도 **같은 봉투**로 나가도록 EntryPoint 교체 |
| 04 | `HandlerInterceptor` | 요청 제한 · 멱등성 키 · 감사 로그 훅. 템플릿은 비워 둔다 |
| 05 | `@Valid` + ArgumentResolver | 요청 DTO 바인딩·검증. 페이징은 공용 `PageQuery` 리졸버로 통일 |
| 06 | Controller → Service | **봉투를 모르는 유일한 구간** |
| 07 | `ResponseEnvelopeAdvice` | `ApiResponse.success()`로 래핑, `meta` 채움. 이미 `ApiResponse`인 본문은 건너뛴다 |
| 08 | Jackson Converter | 네이밍 · 날짜 · null 정책을 한 `ObjectMapper` 설정으로 적용 |

> **필터 순서.** Spring Security의 `springSecurityFilterChain`은 `-100`에 등록된다. 01과 02를 그보다 앞에 두려면 `-110`·`-105`처럼 명시적으로 순서를 지정해야 한다. 기본값으로 두면 Security가 먼저 돌고, 401 응답에는 traceId가 없다.

### 5.2 봉투를 만드는 곳은 세 군데다

원칙 3.1의 "한 곳"은 **컨트롤러가 봉투를 모른다**는 뜻이지, 생산자가 물리적으로 하나라는 뜻이 아니다.

| 생산자 | 담당 |
| --- | --- |
| `ResponseEnvelopeAdvice` | 정상 반환값. `supports()`에서 `StringHttpMessageConverter`와 이미 `ApiResponse`인 본문을 제외한다 |
| `GlobalExceptionHandler` | `DispatcherServlet` 안에서 던져진 예외. `ResponseEntity<ApiResponse<Nothing>>`를 직접 만든다 |
| `FoundationErrorController` | `/error`로 포워딩된 요청 — 매핑 없는 URL의 404, 405, 필터에서 터진 예외 |

세 번째가 없으면 표준이 샌다. Boot 3에서 매핑 없는 URL과 `HttpRequestMethodNotSupportedException`은 `BasicErrorController`로 포워딩되고, 그 경로는 `@RestControllerAdvice`도 `ResponseBodyAdvice`도 타지 않는다 — `{timestamp, status, error, path}`라는 전혀 다른 모양이 나간다. 01~03 필터 안에서 던져진 예외도 같은 경로로 빠진다.

### 5.3 예외 경로

03~06 어디서 갈라지든 07과 **같은 모양으로** 합류한다.

| 예외 | 처리 | 비고 |
| --- | --- | --- |
| `BusinessException` | `ErrorCode`가 들고 있는 상태·코드를 그대로 | 도메인이 의도한 실패 |
| `MethodArgumentNotValidException` | `COMMON_INVALID_PARAMETER` 하나로 수렴 | 필드별 사유를 `details`에 |
| Spring 6 `ErrorResponse` 계열 | `handleExceptionInternal`을 오버라이드해 봉투로 다시 감싼다 | `spring.mvc.problemdetails.enabled=false`. 안 그러면 RFC 7807 `ProblemDetail`이 `data` 안에 들어간다 |
| `Exception` (fallback) | `COMMON_INTERNAL_ERROR` | **내부 메시지 비공개**, traceId만 안내. 로그는 ERROR로 모두 남긴다 |

### 5.4 봉투 제외 경로

`/actuator/**`, `/v3/api-docs/**`, `/swagger-ui/**`, 그리고 `application/json`이 아닌 응답(파일 다운로드 등)은 07을 건너뛴다. `String` 반환도 제외한다 — 이때는 `StringHttpMessageConverter`가 선택되므로 봉투 객체를 돌려주면 `ClassCastException`이 난다.

제외 목록은 프로퍼티로 열어 두되 기본값은 코드에 박는다 — **이 목록이 길어지면 표준이 무너진 신호다.**

## 6. 응답 계약

### 6.1 봉투

모든 JSON 응답은 같은 네 개의 최상위 키를 가진다.

```jsonc
// 200 OK
{
  "success": true,
  "data": {
    "userId": 1042,
    "email": "hyunho@example.com",
    "createdAt": "2026-09-14T08:12:05.000Z"
  },
  "error": null,
  "meta": {
    "traceId": "4bf92f3577b34da6a3ce929d0e0e4736",
    "timestamp": "2026-09-14T08:12:05.331Z",
    "path": "/api/v1/users/1042"
  }
}
```

```jsonc
// 400 Bad Request — 상태 코드는 그대로 쓴다
{
  "success": false,
  "data": null,
  "error": {
    "code": "COMMON_INVALID_PARAMETER",
    "message": "요청 값이 올바르지 않습니다.",
    "details": [
      { "field": "email", "reason": "이메일 형식이 아닙니다." }
    ]
  },
  "meta": { "…": "성공 응답과 동일" }
}
```

도메인 실패는 같은 모양에 `"code": "USER_NOT_FOUND"`와 404가 들어갈 뿐이다. `details`는 검증 실패에서만 채운다.

### 6.2 필드 규격

| 필드 | 타입 | 키 존재 | 설명 |
| --- | --- | --- | --- |
| `success` | boolean | 항상 | 2xx면 `true`. HTTP 상태와 **항상 일치**한다 |
| `data` | `T \| null` | 항상 | 실패 시 `null`. 본문이 있는 모든 응답에 존재한다 — 204는 [§6.5](#65-본문이-없는-성공) |
| `error.code` | string | 실패 시 | 클라이언트가 **분기에 쓰는 유일한 값**. 공개되면 바뀌지 않는다 |
| `error.message` | string | 실패 시 | 사용자에게 그대로 보여줄 문장. `Accept-Language`로 번역 |
| `error.details[]` | array | 선택 | 필드 단위 사유. 폼 검증 오류를 입력란에 매핑 |
| `meta.traceId` | string | 항상 | 응답 헤더 `X-Trace-Id`, 로그의 traceId와 같은 값 |

### 6.3 에러 코드 체계

```kotlin
// foundation-core — Spring 타입을 쓰지 않는다. status 는 Int.
interface ErrorCode {
    val code: String
    val status: Int
    val messageKey: String
}

enum class UserErrorCode(
    override val code: String,
    override val status: Int,
    override val messageKey: String,
) : ErrorCode {
    USER_NOT_FOUND("USER_NOT_FOUND", 404, "error.user.notFound"),
    USER_EMAIL_DUPLICATED("USER_EMAIL_DUPLICATED", 409, "error.user.emailDuplicated"),
}
```

작명 규칙은 `<DOMAIN>_<REASON>` 대문자 스네이크. 도메인 접두사는 필수이고, 여러 도메인이 공유하는 실패만 `COMMON_`을 쓴다.

| 공통 코드 | 상태 |
| --- | --- |
| `COMMON_INVALID_PARAMETER` | 400 |
| `COMMON_UNAUTHORIZED` | 401 |
| `COMMON_FORBIDDEN` | 403 |
| `COMMON_NOT_FOUND` | 404 |
| `COMMON_METHOD_NOT_ALLOWED` | 405 |
| `COMMON_CONFLICT` | 409 |
| `COMMON_TOO_MANY_REQUESTS` | 429 |
| `COMMON_INTERNAL_ERROR` | 500 |

`foundation-test`의 `ErrorCodeContract` 검사기를 각 모듈의 테스트가 호출해 빌드마다 확인한다 — 코드 문자열 중복 없음, `code`와 enum 이름 일치, 상태 값이 유효한 HTTP 코드, 메시지 키가 메시지 번들에 실재함.

> **이름 충돌.** 봉투 타입 `ApiResponse`는 springdoc이 쓰는 `io.swagger.v3.oas.annotations.responses.ApiResponse`와 이름이 겹친다. 둘을 함께 쓰는 파일에서는 import alias를 강제한다.

### 6.4 페이징

```jsonc
"data": {
  "content": [ "…" ],
  "page": {
    "number": 0,
    "size": 20,
    "totalElements": 137,
    "totalPages": 7,
    "sort": "createdAt,desc"
  }
}
```

Spring의 `Page`를 그대로 직렬화하지 않는다 — 내부 구조가 버전에 따라 바뀌므로 API 계약으로 쓸 수 없다.

요청 쪽 타입도 `PageQuery`라는 자체 이름을 쓴다. `PageRequest`는 `org.springframework.data.domain`에 이미 있고, Spring Data Web이 클래스패스에 있으면 `PageableHandlerMethodArgumentResolver`가 같은 인자를 두고 경쟁한다 — 이 리졸버는 꺼 둔다.

### 6.5 본문이 없는 성공

`204 No Content`는 봉투를 쓰지 않는다. 핸들러가 `null`이나 `Unit`을 반환하면 메시지 컨버터 자체가 호출되지 않아 `beforeBodyWrite`가 실행되지 않기 때문이다 — `"data": null`인 봉투가 아니라 **본문이 아예 없는** 응답이 나간다.

이 예외를 없애고 싶으면 삭제 API가 204 대신 200 + `"data": null`을 반환하도록 정하면 된다. 둘 중 하나를 고르고 문서에 적는다 ([§10](#10-열린-질문)).

### 6.6 되짚어 볼 결정

**전부 200으로 내리지 않는다.** 상태 코드는 HTTP 계층의 사실이다. 게이트웨이·모니터링·브라우저 캐시가 먼저 읽는 값을 거짓으로 만들면 관측이 무너진다. `success`는 보조 신호다.

**키는 항상 존재한다.** Jackson을 `NON_NULL`로 두지 않는다. 키가 사라졌다 나타나면 클라이언트 파싱 코드가 두 갈래가 된다.

**`code`와 `message`의 역할을 분리한다.** `message`는 언제든 다듬을 수 있는 문구, `code`는 계약이다. 이 분리가 있어야 문구 수정이 클라이언트를 깨지 않는다.

## 7. 문서화 파이프라인

### 7.1 개발자가 쓰는 것

```kotlin
@Tag(name = "User")
@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val userService: UserService,
) {

    @Operation(summary = "사용자 단건 조회")
    @ApiErrorCodes(UserErrorCode.USER_NOT_FOUND)
    @GetMapping("/{userId}")
    fun get(@PathVariable userId: Long): UserResponse = userService.get(userId)

    // 봉투도, 에러 스키마도 쓰지 않는다. 둘 다 파운데이션이 채운다.
}
```

### 7.2 문서에 나타나는 것

```yaml
/api/v1/users/{userId}:
  get:
    summary: 사용자 단건 조회
    responses:
      '200':
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ApiResponseUserResponse'   # 주입
        headers:
          X-Trace-Id: { schema: { type: string } }                   # 주입
      '404':
        description: 'USER_NOT_FOUND — 사용자를 찾을 수 없습니다.'      # 주입
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ApiResponseVoid'
      '500':
        description: 'COMMON_INTERNAL_ERROR'                         # 전역 기본
```

스키마 이름이 `ApiResponseUserResponse`인 이유는 springdoc이 제네릭을 그렇게 평탄화하기 때문이다. `<`와 `>`는 `components.schemas` 키에 쓸 수 없다.

### 7.3 자동과 수동의 경계

| 자동으로 채워진다 | 사람이 써야 한다 |
| --- | --- |
| 봉투 래핑 스키마 `ApiResponse<T>`와 `meta` | 엔드포인트 요약과 "언제 쓰는가" |
| 에러 코드 목록 → 상태별 응답 | 의미 있는 요청·응답 예시 값 |
| 공통 응답 헤더 `X-Trace-Id` | 도메인 용어 정의 (`docs/glossary.md`) |
| 페이징 파라미터와 `PageResponse` 스키마 | 인증 방식과 권한 요구사항 |
| 전역 기본 응답 (400 · 401 · 403 · 500) | 결정의 배경 — ADR |

### 7.4 검증은 두 겹이다

흔히 뭉뚱그리는 지점이 있다. **REST Docs는 OpenAPI 문서를 검증하지 않는다.** REST Docs가 보장하는 건 "문서에 실린 예시가 실제로 테스트가 받아낸 응답"이라는 것뿐이고, springdoc이 만든 `openapi.json`의 존재조차 모른다.

| 겹 | 보장하는 것 | 보장하지 않는 것 |
| --- | --- | --- |
| REST Docs | 문서의 예시·필드 설명이 실재하는 응답에서 나왔다 | 스펙 전체가 맞는지는 모른다 |
| 스펙 응답 검증 | MockMvc가 받은 실제 응답이 `openapi.json`의 해당 스키마를 만족한다 | 테스트가 없는 엔드포인트는 검증되지 않는다 |

두 번째 겹이 `foundation-test`가 제공하는 핵심이다. OpenAPI 응답 검증기(예: `swagger-request-validator`)에 생성된 스펙을 물리고, 모든 컨트롤러 테스트가 그 검증기를 통과하게 한다. 커스터마이저가 봉투를 잘못 주입하면 여기서 잡힌다.

### 7.5 CI 게이트

| 태스크 | 막는 것 |
| --- | --- |
| `./gradlew check` | REST Docs 스니펫은 테스트가 통과해야 나온다. 스펙 응답 검증도 여기서 돈다 |
| `./gradlew verifyOpenApi` | 스펙을 재생성해 커밋된 `openapi.json`과 비교. 차이가 있으면 실패 — 갱신 누락 차단 |
| `./gradlew diffOpenApi` | 기준 브랜치 대비 필드 삭제·타입 변경·에러 코드 제거를 찾아 PR 코멘트 |

## 8. 저장소 레이아웃

```
server-foundation-template/
├─ settings.gradle.kts
├─ build.gradle.kts
├─ gradle/libs.versions.toml                  ← 모든 버전을 여기 한 곳에서 고정
│
├─ foundation-core/                           Spring 의존 없음
│  ├─ main/kotlin/response/ApiResponse.kt      ← 봉투 + meta
│  │                response/PageResponse.kt
│  │                error/ErrorCode.kt         ← status 는 Int
│  │                error/CommonErrorCode.kt
│  │                error/BusinessException.kt
│  └─ test/kotlin/CommonErrorCodeContractTest.kt
│
├─ foundation-web/
│  ├─ main/kotlin/advice/ResponseEnvelopeAdvice.kt
│  │                advice/GlobalExceptionHandler.kt
│  │                advice/FoundationErrorController.kt   ← /error 경로
│  │                filter/TraceIdFilter.kt    ← order -110
│  │                filter/RequestLoggingFilter.kt
│  │                resolver/PageQueryResolver.kt
│  │                config/WebAutoConfiguration.kt
│  └─ main/resources/META-INF/spring/
│       org.springframework.boot.autoconfigure.AutoConfiguration.imports
│                                              ← Boot 3 의 자동 등록 지점
│
├─ foundation-docs/
│  └─ main/kotlin/ApiErrorCodes.kt             ← 엔드포인트에 다는 어노테이션
│                  EnvelopeSchemaCustomizer.kt ← 봉투를 문서에 되돌려 넣는다
│                  ErrorResponseCustomizer.kt
│                  DocsAutoConfiguration.kt
│
├─ foundation-observability/
│  └─ main/kotlin/logging/StructuredLogEncoder.kt
│                  logging/MaskingRules.kt     ← 마스킹 규칙의 단일 출처
│                  logging/MdcKeys.kt
│
├─ foundation-test/                           testImplementation 전용
│  └─ main/kotlin/ApiResponseAssertions.kt
│                  ErrorCodeContract.kt        ← 각 모듈 테스트가 호출하는 검사기
│                  OpenApiResponseValidator.kt
│                  RestDocsSupport.kt
│
├─ sample-api/                                여기부터가 복제해서 고칠 코드다
│  └─ main/kotlin/user/UserController.kt
│                  user/UserService.kt
│                  user/UserErrorCode.kt
│                  user/dto/UserResponse.kt
│
└─ docs/
   ├─ design/architecture.md                   ← 이 문서
   ├─ glossary.md
   ├─ adr/0001-response-envelope.md
   └─ openapi/openapi.json                     ← 빌드 산출물이지만 커밋한다
```

세 가지 규칙:

- **기능으로 나눈다.** `sample-api`는 controller/service/repository가 아니라 `user/` 같은 기능 패키지로 나눈다. 복제 단위가 곧 기능이 된다.
- **설정은 자동으로.** `foundation-web`과 `foundation-docs`는 auto-configuration으로 등록된다. Boot 3는 `spring.factories`를 더 이상 읽지 않으므로 등록 지점은 `META-INF/spring/…AutoConfiguration.imports`다. 애플리케이션에 복사해 붙일 `@Configuration`이 없어야 표준이 유지된다.
- **`openapi.json`을 커밋한다.** 생성물이지만 리뷰 대상이다. diff에 스펙 변경이 보여야 호환성이 깨지는 변경을 PR에서 잡는다.

## 9. 구현 순서

각 단계는 "코드를 썼다"가 아니라 **확인 가능한 사실**로 끝난다. 완료 기준을 못 적는 단계는 아직 설계가 덜 된 단계다.

| 단계 | 이름 | 산출물 | 완료 기준 |
| --- | --- | --- | --- |
| **M0** | 골격 | 멀티모듈 6개, 버전 카탈로그, ktlint/detekt, CI 스켈레톤 | 빈 모듈 상태로 `./gradlew build`가 CI에서 통과한다 |
| **M1** | 계약 | `foundation-core` — 봉투, 페이지, `ErrorCode`, 공통 코드 | 의존성 그래프에 Spring이 없고, 에러 코드 계약 테스트가 통과한다 |
| **M2** | 표준화 | `foundation-web` + `foundation-observability` — 봉투 Advice, 예외 핸들러, `/error`, TraceId, 마스킹 | 성공 · 검증 실패 · 미처리 예외 · 매핑 없는 URL 네 응답이 모두 같은 봉투로 나온다 |
| **M3** | 문서화 | `foundation-docs` — 스키마 커스터마이저, `@ApiErrorCodes` | Swagger UI에서 200이 봉투로, 404가 실제 코드로 보인다 |
| **M4** | 검증 | REST Docs 지원, 스펙 응답 검증기, `verifyOpenApi` / `diffOpenApi` | 응답 필드를 하나 바꾸면 문서 갱신 없이는 CI가 실패한다 |
| **M5** | 템플릿화 | GitHub template 설정, 패키지명 치환 스크립트, README · ADR · 용어집 | 새 저장소에서 첫 엔드포인트와 문서까지 10분 |

순서가 중요하다. **M1~M2가 "요청·응답 표준화", M3~M4가 "문서화"** 다 — 표준이 없으면 자동 문서화할 대상 자체가 없다.

## 10. 열린 질문

리뷰에서 정해야 할 것들.

1. **모듈 개수.** 6개 멀티모듈 vs 단일 모듈 + 패키지 분리. 팀 규모와 이 파운데이션을 다른 저장소에서도 쓸 계획이 있는지에 달렸다.
2. **인증 방식.** 범위 밖으로 뒀지만, JWT인지 세션인지에 따라 `SecurityFilterChain` 자리의 기본 제공 수준이 달라진다.
3. **필드 네이밍.** 위 예시는 camelCase다. 기존 클라이언트가 snake_case를 쓴다면 `ObjectMapper` 전략을 M1에서 정해야 한다.
4. **다국어.** `Accept-Language` 기반 메시지 번역을 M2에 넣을지, 한국어 고정으로 시작할지.
5. **204 처리.** 본문 없는 성공을 204로 둘지, 200 + `"data": null`로 통일할지 ([§6.5](#65-본문이-없는-성공)).
6. **버전 고정.** 본문의 Kotlin 2.x / Boot 3.x / JDK 21은 제안값이다. M0에서 정확한 버전으로 고정한다.

## 부록. 남길 ADR

| 번호 | 제목 |
| --- | --- |
| 0001 | 표준 응답 봉투를 `ResponseBodyAdvice`에서 씌운다 |
| 0002 | 실패 응답도 실제 HTTP 상태 코드를 사용한다 |
| 0003 | `ErrorCode`를 단일 레지스트리로 두고 문서를 파생시킨다 |
| 0004 | `openapi.json`을 저장소에 커밋하고 CI에서 검증한다 |
| 0005 | Web MVC를 선택하고 WebFlux를 범위에서 제외한다 |
| 0006 | `/error` 경로까지 봉투를 확장하고 `ProblemDetail`을 끈다 |
