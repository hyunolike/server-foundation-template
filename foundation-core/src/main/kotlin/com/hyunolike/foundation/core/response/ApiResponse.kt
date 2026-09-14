package com.hyunolike.foundation.core.response

/**
 * 모든 JSON 응답이 공유하는 표준 봉투. (설계 문서 §6.1)
 *
 * 네 개의 최상위 키는 항상 존재한다 — 키가 사라졌다 나타나면 클라이언트 파싱 코드가
 * 두 갈래가 되기 때문이다. 그래서 이 타입을 직렬화하는 ObjectMapper 는
 * `JsonInclude.Include.NON_NULL` 로 설정하지 않는다.
 *
 * 컨트롤러는 이 타입을 반환하지 않는다. 봉투를 만드는 곳은 foundation-web 의 세 군데뿐이다.
 */
data class ApiResponse<out T>(
    val success: Boolean,
    val data: T?,
    val error: ApiError?,
    val meta: ResponseMeta,
) {
    companion object {
        fun <T> success(
            data: T?,
            meta: ResponseMeta,
        ): ApiResponse<T> = ApiResponse(success = true, data = data, error = null, meta = meta)

        fun failure(
            error: ApiError,
            meta: ResponseMeta,
        ): ApiResponse<Nothing> = ApiResponse(success = false, data = null, error = error, meta = meta)
    }
}

/**
 * 실패 응답의 본문.
 *
 * [code] 는 계약이고 [message] 는 문구다. 이 분리가 있어야 문구를 다듬어도
 * 클라이언트가 깨지지 않는다. (설계 문서 §6.6)
 */
data class ApiError(
    val code: String,
    val message: String,
    val details: List<ApiErrorDetail>?,
)

/** 필드 단위 실패 사유. 검증 실패에서만 채운다. */
data class ApiErrorDetail(
    val field: String,
    val reason: String,
)

/**
 * 성공/실패와 무관하게 항상 실리는 메타데이터.
 *
 * [traceId] 는 응답 헤더 `X-Trace-Id`, 로그의 traceId 와 같은 값이다.
 */
data class ResponseMeta(
    val traceId: String,
    val timestamp: String,
    val path: String,
)
