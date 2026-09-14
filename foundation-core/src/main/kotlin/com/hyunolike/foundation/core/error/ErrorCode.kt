package com.hyunolike.foundation.core.error

/**
 * 에러 코드의 단일 레지스트리. (설계 문서 §3.2)
 *
 * 코드 문자열 · HTTP 상태 · 메시지 키를 한 덩어리로 들고 있다. 런타임 응답,
 * OpenAPI 문서, 계약 테스트가 전부 이 인터페이스의 구현을 읽는다.
 *
 * [status] 의 타입이 `HttpStatus` 가 아니라 `Int` 인 이유는 §4 에 있다 —
 * `org.springframework.http.HttpStatus` 는 spring-web 에 있어서, 그것을 쓰는 순간
 * 이 모듈의 "Spring 의존 0" 이 깨진다. 숫자 → HttpStatus 변환은 foundation-web 이 한다.
 */
interface ErrorCode {
    /** 클라이언트가 분기에 쓰는 유일한 값. `<DOMAIN>_<REASON>` 대문자 스네이크. */
    val code: String

    /** HTTP 상태 코드. 실제 응답의 상태 줄에 그대로 쓰인다. */
    val status: Int

    /** 메시지 번들의 키. 사용자에게 보일 문구는 여기서 온다. */
    val messageKey: String
}
