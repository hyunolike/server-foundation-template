package com.hyunolike.foundation.core.error

/**
 * 여러 도메인이 공유하는 실패. 도메인 고유의 실패는 각 도메인이 자기 enum 을 만든다.
 *
 * 접두사 `COMMON_` 은 이 목록에만 쓴다. (설계 문서 §6.3)
 */
enum class CommonErrorCode(
    override val status: Int,
    override val messageKey: String,
) : ErrorCode {
    COMMON_INVALID_PARAMETER(400, "error.common.invalidParameter"),
    COMMON_UNAUTHORIZED(401, "error.common.unauthorized"),
    COMMON_FORBIDDEN(403, "error.common.forbidden"),
    COMMON_NOT_FOUND(404, "error.common.notFound"),
    COMMON_METHOD_NOT_ALLOWED(405, "error.common.methodNotAllowed"),
    COMMON_CONFLICT(409, "error.common.conflict"),
    COMMON_TOO_MANY_REQUESTS(429, "error.common.tooManyRequests"),
    COMMON_INTERNAL_ERROR(500, "error.common.internalError"),
    ;

    override val code: String get() = name
}
