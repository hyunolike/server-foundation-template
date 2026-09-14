# server-foundation-template

📘 문서화와 표준화 처리(요청, 응답)가 결합된 스마트 서버 개발 템플릿

Kotlin · Spring Boot 3.x 기반. 컨트롤러는 도메인 DTO만 반환하고, 표준 응답 봉투와 API 문서는 파운데이션이 채운다.

- **설계 문서** — [`docs/design/architecture.md`](docs/design/architecture.md)
- **생성된 API 스펙** — [`docs/openapi/openapi.json`](docs/openapi/openapi.json) (빌드 산출물이지만 커밋한다)

## 무엇이 표준화되는가

```jsonc
// 모든 JSON 응답이 같은 네 개의 최상위 키를 가진다
{
  "success": true,
  "data": { "userId": 1042, "email": "hyunho@example.com" },
  "error": null,
  "meta": { "traceId": "…", "timestamp": "…", "path": "/api/v1/users/1042" }
}
```

컨트롤러는 이렇게만 쓴다.

```kotlin
@Operation(summary = "사용자 단건 조회")
@ApiErrorCodes("USER_NOT_FOUND")
@GetMapping("/{userId}")
fun get(@PathVariable userId: Long): UserResponse = userService.get(userId)
```

봉투도 에러 스키마도 적지 않는다. 실제 응답과 OpenAPI 문서 양쪽을 파운데이션이 채우고, 둘이 어긋나면 CI가 막는다.

## 모듈

| 모듈 | 책임 |
| --- | --- |
| `foundation-core` | `ApiResponse`, `PageResponse`, `ErrorCode`, `BusinessException`. **Spring 의존 0** |
| `foundation-web` | 봉투 Advice, 전역 예외 핸들러, `/error` 컨트롤러, TraceId·로깅 필터, 페이징 리졸버 |
| `foundation-docs` | springdoc 커스터마이저, `@ApiErrorCodes`, 호환성 검사기 |
| `foundation-observability` | 구조화 로그, MDC 키, 마스킹 규칙 |
| `foundation-test` | 봉투 단언, 에러 코드 계약 검사기, 스펙 응답 검증기 |
| `sample-api` | 참조 구현. **여기부터가 복제해서 고칠 코드다** |

## 시작하기

```bash
./gradlew check                      # 린트 · 계약 테스트 · 봉투 테스트 · 스펙 검증
./gradlew :sample-api:bootRun        # http://localhost:8080/swagger-ui.html
./gradlew :sample-api:updateOpenApi  # 스펙을 바꿨을 때 docs/openapi/openapi.json 갱신
```

> 소스와 테스트 이름에 한글을 쓰므로 빌드는 UTF-8 로케일에서 실행해야 한다 (`LANG=C.UTF-8`).
> `sun.jnu.encoding` 은 `-D` 로 바뀌지 않는다.

## 새 서비스 시작하기

1. 이 저장소를 템플릿으로 새 저장소를 만든다.
2. `sample-api` 를 복제해 도메인 이름으로 바꾼다.
3. 도메인의 `ErrorCode` enum 을 만들고 `ErrorCodeCatalog` 빈에 등록한다.
4. 컨트롤러를 쓴다. 봉투는 건드리지 않는다.

## 검증은 두 겹이다

`./gradlew check` 가 막는 것:

| 검사 | 막는 것 |
| --- | --- |
| 에러 코드 계약 | 코드 중복, enum 이름 불일치, 유효하지 않은 상태 코드, 없는 메시지 키 |
| 봉투 계약 | 성공 · 검증 실패 · 미처리 예외 · 매핑 없는 URL 이 서로 다른 모양으로 나가는 것 |
| 스펙 응답 검증 | 실제 응답이 생성된 OpenAPI 스키마를 만족하지 않는 것 |
| 스펙 스냅샷 | 코드는 바꾸고 `openapi.json` 갱신을 잊는 것 |
| 호환성 검사 (PR) | 필드 삭제 · 타입 변경 · 에러 코드 제거 |
