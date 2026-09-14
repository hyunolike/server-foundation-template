package com.hyunolike.foundation.web.support

import com.hyunolike.foundation.core.response.ResponseMeta
import com.hyunolike.foundation.observability.logging.MdcKeys
import org.slf4j.MDC
import java.time.Clock
import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * 성공이든 실패든 같은 meta 를 만든다.
 *
 * traceId 는 MDC 에서 가져온다 — 필터가 요청 맨 앞에서 넣어 두므로, 로그에 찍힌 값과
 * 응답에 실린 값이 반드시 같다.
 */
class ResponseMetaFactory(
    private val clock: Clock = Clock.systemUTC(),
) {
    fun create(path: String): ResponseMeta =
        ResponseMeta(
            traceId = MDC.get(MdcKeys.TRACE_ID) ?: UNKNOWN_TRACE_ID,
            timestamp = TIMESTAMP_FORMAT.format(Instant.now(clock)),
            path = path,
        )

    companion object {
        const val UNKNOWN_TRACE_ID = "unknown"
        private val TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(java.time.ZoneOffset.UTC)
    }
}
