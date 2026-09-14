# 용어집

이 저장소의 문서와 코드가 같은 단어를 같은 뜻으로 쓰기 위한 목록. 새 용어는 쓰기 전에 여기 먼저 적는다.

| 용어 | 뜻 |
| --- | --- |
| **봉투 (envelope)** | 모든 JSON 응답이 공유하는 최상위 구조 `{success, data, error, meta}`. 타입은 `ApiResponse<T>` |
| **에러 코드 (error code)** | `error.code`에 실리는 `<DOMAIN>_<REASON>` 대문자 스네이크 문자열. 클라이언트가 분기에 쓰는 유일한 값이며, 한 번 공개되면 바뀌지 않는다 |
| **레지스트리 (registry)** | 에러 코드의 단일 출처. `ErrorCode` 구현들과 이를 모은 `ErrorCodeCatalog` |
| **파운데이션 (foundation)** | `foundation-*` 모듈 전체. 애플리케이션이 의존성만 추가하면 켜지는 표준 |
| **생산자 (producer)** | 봉투를 실제로 만드는 코드. 정확히 셋이다 — `ResponseEnvelopeAdvice`, `GlobalExceptionHandler`, `FoundationErrorController` |
| **제외 경로 (excluded path)** | 봉투를 씌우지 않는 경로. `/actuator/**`, `/v3/api-docs/**`, `/swagger-ui/**`. 이 목록이 길어지면 표준이 무너진 신호다 |
| **traceId** | 요청 하나를 가리키는 32자 16진수. 응답 `meta.traceId`, 응답 헤더 `X-Trace-Id`, 로그의 `traceId`가 모두 같은 값 |
| **계약 테스트 (contract test)** | 규격 자체를 검사하는 테스트. `ErrorCodeContract`(에러 코드), `EnvelopeContractTest`(봉투), `OpenApiContractTest`(문서) |
| **첫 번째 겹 / 두 번째 겹** | 문서 검증의 두 층. 첫째는 REST Docs — 문서의 예시가 실재하는가. 둘째는 스펙 응답 검증 — 실제 응답이 스키마를 만족하는가. [ADR 0008](adr/0008-two-layer-doc-verification.md) |
| **스냅샷 (snapshot)** | 커밋된 `docs/openapi/openapi.json`. 생성물이지만 리뷰 대상이다. [ADR 0004](adr/0004-commit-openapi-json.md) |
| **호환성이 깨지는 변경 (breaking change)** | 경로·오퍼레이션·응답 상태·에러 코드·필드의 제거, 필드 타입 변경, 선택 필드의 필수 전환 |
| **완료 기준 (exit criteria)** | 각 마일스톤이 끝났음을 확인할 수 있는 사실. "코드를 썼다"는 완료 기준이 아니다 |
