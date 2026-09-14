package com.hyunolike.foundation.observability.logging

/**
 * MDC 키 이름의 단일 출처.
 *
 * 응답 `meta.traceId`, 응답 헤더 `X-Trace-Id`, 로그의 traceId 가 같은 값을 가리키려면
 * 이름도 한 곳에서 와야 한다. (설계 문서 §5.1)
 */
object MdcKeys {
    const val TRACE_ID = "traceId"
    const val HTTP_METHOD = "httpMethod"
    const val HTTP_PATH = "httpPath"
}
