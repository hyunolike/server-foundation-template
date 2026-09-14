package com.hyunolike.foundation.web.support

import com.hyunolike.foundation.core.error.CommonErrorCode
import com.hyunolike.foundation.core.error.ErrorCode

/**
 * Spring 이 상태 코드만 들고 오는 경로(프레임워크 예외, `/error` 포워딩)에서
 * 상태를 공통 에러 코드로 되돌린다.
 *
 * 에러 코드가 단일 레지스트리에서 나온다는 원칙을 지키려면, 상태만 남은 자리에도
 * 반드시 코드가 붙어야 한다. (설계 문서 §3.2)
 */
object HttpStatusMapper {
    private val BY_STATUS: Map<Int, ErrorCode> =
        CommonErrorCode.entries.associateBy { it.status }

    fun toErrorCode(status: Int): ErrorCode =
        BY_STATUS[status]
            ?: when {
                status >= 500 -> CommonErrorCode.COMMON_INTERNAL_ERROR
                status >= 400 -> CommonErrorCode.COMMON_INVALID_PARAMETER
                else -> CommonErrorCode.COMMON_INTERNAL_ERROR
            }
}
