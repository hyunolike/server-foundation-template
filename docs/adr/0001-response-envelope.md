# 0001. 표준 응답 봉투를 ResponseBodyAdvice에서 씌운다

- 상태: 수용됨
- 관련: [설계 문서 §3.1](../design/architecture.md)

## 맥락

모든 JSON 응답이 같은 최상위 구조를 갖게 하려면 누군가는 도메인 DTO를 감싸야 한다. 컨트롤러가 직접 감싸면 표준이 200개 메서드로 퍼진다. 하나만 빠뜨려도 클라이언트는 두 가지 파싱 경로를 가져야 한다.

## 결정

컨트롤러는 도메인 DTO를 그대로 반환한다. 봉투는 `ResponseBodyAdvice` 구현 하나가 씌운다. 컨트롤러가 `ApiResponse`를 반환하는 코드는 리뷰에서 막는다.

## 결과

- 서비스 계층에 응답 규격이 새어 들어가지 않는다.
- 대신 springdoc이 봉투를 모르게 된다 — 문서 쪽을 따로 메워야 한다 ([0003](0003-error-code-registry.md), [0008](0008-two-layer-doc-verification.md)).
- 봉투를 만드는 곳이 하나는 아니다. 예외 경로와 `/error` 경로에 각각 생산자가 필요하다 ([0006](0006-error-dispatch-envelope.md)).
- `supports()`가 Jackson 컨버터만 받아야 한다. `String` 반환에는 `StringHttpMessageConverter`가 선택되므로, 거기서 봉투를 돌려주면 `ClassCastException`이 난다.
