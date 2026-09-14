# 0003. ErrorCode를 단일 레지스트리로 두고 문서를 파생시킨다

- 상태: 수용됨
- 관련: [설계 문서 §3.2, §6.3](../design/architecture.md)

## 맥락

에러 코드 목록이 위키에 있으면 코드와 어긋난다. 코드 문자열 · HTTP 상태 · 메시지가 따로 관리되면 "코드는 있는데 상태가 안 맞는" 응답이 나온다.

## 결정

`ErrorCode` 인터페이스가 세 가지를 한 덩어리로 들고 있고, 도메인마다 enum으로 구현한다. 런타임 응답, OpenAPI 문서, 계약 테스트가 전부 이 구현을 읽는다.

`status`의 타입은 `HttpStatus`가 아니라 `Int`다. `org.springframework.http.HttpStatus`는 `spring-web`에 있어서, 그것을 쓰는 순간 `foundation-core`의 "Spring 의존 0"이 깨진다.

## 결과

- `ErrorCodeContract` 검사기가 빌드마다 확인한다 — 코드 중복, enum 이름과 `code` 불일치, 유효하지 않은 상태 값, 없는 메시지 키.
- 문서의 `@ApiErrorCodes`는 문자열을 쓴다. 어노테이션 인자는 특정 enum 타입으로 고정되는데 도메인마다 enum이 다르기 때문이다. 대신 `ErrorCodeCatalog`에 없는 코드는 테스트가 잡는다.
- 숫자 → `HttpStatus` 변환은 `foundation-web`의 책임이 된다.
