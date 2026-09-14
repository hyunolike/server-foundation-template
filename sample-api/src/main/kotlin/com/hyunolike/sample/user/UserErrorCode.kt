package com.hyunolike.sample.user

import com.hyunolike.foundation.core.error.ErrorCode

/** 도메인 고유의 실패. 접두사 `USER_` 는 필수다. (설계 문서 §6.3) */
enum class UserErrorCode(
    override val status: Int,
    override val messageKey: String,
) : ErrorCode {
    USER_NOT_FOUND(404, "error.user.notFound"),
    USER_EMAIL_DUPLICATED(409, "error.user.emailDuplicated"),
    ;

    override val code: String get() = name
}
