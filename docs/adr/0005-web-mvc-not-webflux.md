# 0005. Web MVC를 선택하고 WebFlux를 범위에서 제외한다

- 상태: 수용됨
- 관련: [설계 문서 §2](../design/architecture.md)

## 맥락

표준화는 Filter · Interceptor · Advice라는 훅 위에 서 있다. WebFlux는 이 훅의 구성이 다르다 — `WebFilter`, `ResponseBodyResultHandler`, 리액티브 컨텍스트 기반 MDC 전파.

## 결정

Web MVC만 지원한다. WebFlux 지원은 별도 설계로 다룬다.

## 결과

- `foundation-web`은 `jakarta.servlet` API에 의존한다. 자동 설정에 `@ConditionalOnWebApplication(type = SERVLET)`을 건다.
- traceId를 MDC로 전파하는 단순한 방식이 성립한다. WebFlux였다면 리액터 컨텍스트를 써야 한다.
- 리액티브가 필요해지면 `foundation-core`는 그대로 쓸 수 있다 — 그래서 core에 Spring 의존을 두지 않았다.
