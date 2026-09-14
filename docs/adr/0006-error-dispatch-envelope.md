# 0006. /error 경로까지 봉투를 확장하고 ProblemDetail을 끈다

- 상태: 수용됨
- 관련: [설계 문서 §5.2, §5.3](../design/architecture.md)

## 맥락

[0001](0001-response-envelope.md)의 Advice는 `DispatcherServlet`이 핸들러를 찾은 요청에만 닿는다. Spring Boot 3에서 매핑 없는 URL의 404, `HttpRequestMethodNotSupportedException`의 405, 그리고 필터 안에서 터진 예외는 `/error`로 포워딩되어 `BasicErrorController`가 처리한다 — `{timestamp, status, error, path}`라는 전혀 다른 모양이다.

또 Spring 6는 프레임워크 예외를 RFC 7807 `ProblemDetail`로 내보낸다. 그대로 두면 같은 API에서 두 가지 실패 모양이 나간다.

## 결정

`ErrorController`를 직접 구현해 `/error` 응답도 봉투로 만든다. 자동 설정은 `ErrorMvcAutoConfiguration`보다 **먼저** 등록한다. `spring.mvc.problemdetails.enabled=false`로 두고, `ResponseEntityExceptionHandler.handleExceptionInternal`을 덮어 프레임워크가 만든 본문을 봉투로 교체한다.

## 결과

- 봉투를 만드는 곳이 셋이 된다: Advice(정상), 전역 예외 핸들러(예외), `/error` 컨트롤러(포워딩).
- 자동 설정 순서가 뒤집히면 `/error` 매핑이 둘이 되어 컨텍스트가 아예 기동하지 못한다. 구현 중에 실제로 겪었고, 그래서 `@AutoConfiguration(before = ...)`을 명시한다.
- `/error`는 내부 경로일 뿐 API가 아니므로 OpenAPI 문서에서 제외한다.
