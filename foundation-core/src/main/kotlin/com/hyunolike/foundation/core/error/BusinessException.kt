package com.hyunolike.foundation.core.error

import com.hyunolike.foundation.core.response.ApiErrorDetail

/**
 * 도메인이 의도한 실패. 던진 [errorCode] 의 상태와 코드가 그대로 응답이 된다.
 *
 * 예상 밖의 오류는 이 타입을 쓰지 않는다 — 그쪽은 COMMON_INTERNAL_ERROR 로 수렴하고
 * 내부 메시지를 밖으로 내보내지 않는다. (설계 문서 §5.3)
 */
open class BusinessException(
    val errorCode: ErrorCode,
    val details: List<ApiErrorDetail> = emptyList(),
    val messageArguments: List<Any> = emptyList(),
    cause: Throwable? = null,
) : RuntimeException(errorCode.code, cause)
