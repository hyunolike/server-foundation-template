package com.hyunolike.foundation.docs

/**
 * 이 엔드포인트가 낼 수 있는 도메인 실패를 선언한다.
 *
 * 값은 [com.hyunolike.foundation.core.error.ErrorCode.code] 문자열이다. 문자열인 이유는
 * 어노테이션 인자가 특정 enum 타입으로 고정되기 때문이다 — 도메인마다 enum 이 다르므로
 * 타입으로는 묶을 수 없다. 대신 [com.hyunolike.foundation.core.error.ErrorCodeCatalog] 에서
 * 찾지 못하는 코드는 계약 테스트가 잡는다.
 *
 * ```
 * @ApiErrorCodes("USER_NOT_FOUND")
 * @GetMapping("/{userId}")
 * fun get(@PathVariable userId: Long): UserResponse = ...
 * ```
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class ApiErrorCodes(
    vararg val value: String,
)
