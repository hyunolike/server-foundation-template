# Architecture Decision Records

결정의 배경은 코드에 남지 않는다. 왜 그렇게 했는지를 모르면 다음 사람이 되돌린다.

한 파일은 하나의 결정을 담고, 한 번 `수용됨`이 되면 고치지 않는다. 생각이 바뀌면 새 ADR을 쓰고 이전 것을 `대체됨`으로 표시한다.

| 번호 | 제목 | 상태 |
| --- | --- | --- |
| [0001](0001-response-envelope.md) | 표준 응답 봉투를 ResponseBodyAdvice에서 씌운다 | 수용됨 |
| [0002](0002-real-http-status.md) | 실패 응답도 실제 HTTP 상태 코드를 사용한다 | 수용됨 |
| [0003](0003-error-code-registry.md) | ErrorCode를 단일 레지스트리로 두고 문서를 파생시킨다 | 수용됨 |
| [0004](0004-commit-openapi-json.md) | openapi.json을 저장소에 커밋하고 CI에서 검증한다 | 수용됨 |
| [0005](0005-web-mvc-not-webflux.md) | Web MVC를 선택하고 WebFlux를 범위에서 제외한다 | 수용됨 |
| [0006](0006-error-dispatch-envelope.md) | /error 경로까지 봉투를 확장하고 ProblemDetail을 끈다 | 수용됨 |
| [0007](0007-no-envelope-for-204.md) | 본문 없는 성공은 204로 내보내고 봉투를 쓰지 않는다 | 수용됨 |
| [0008](0008-two-layer-doc-verification.md) | 문서 검증을 두 겹으로 나눈다 | 수용됨 |
