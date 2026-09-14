package com.hyunolike.foundation.web.filter

import com.hyunolike.foundation.observability.logging.MdcKeys
import com.hyunolike.foundation.web.config.FoundationWebProperties
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.web.filter.OncePerRequestFilter
import java.security.SecureRandom
import java.util.HexFormat

/**
 * 요청 맨 앞에서 traceId 를 확정한다.
 *
 * 순서가 [ORDER] 인 이유: Spring Security 의 필터 체인은 -100 에 등록된다. 그보다 앞서지
 * 않으면 인증 실패(401) 응답에 traceId 가 없다. (설계 문서 §5.1)
 */
class TraceIdFilter(
    private val properties: FoundationWebProperties,
) : OncePerRequestFilter() {
    private val random = SecureRandom()

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val traceId = resolveTraceId(request)
        MDC.put(MdcKeys.TRACE_ID, traceId)
        MDC.put(MdcKeys.HTTP_METHOD, request.method)
        MDC.put(MdcKeys.HTTP_PATH, request.requestURI)
        request.setAttribute(TRACE_ID_ATTRIBUTE, traceId)
        response.setHeader(properties.traceIdHeader, traceId)
        try {
            filterChain.doFilter(request, response)
        } finally {
            MDC.remove(MdcKeys.TRACE_ID)
            MDC.remove(MdcKeys.HTTP_METHOD)
            MDC.remove(MdcKeys.HTTP_PATH)
        }
    }

    private fun resolveTraceId(request: HttpServletRequest): String =
        traceParentTraceId(request.getHeader(TRACEPARENT_HEADER))
            ?: request.getHeader(properties.requestIdHeader)?.takeIf { it.isNotBlank() }
            ?: generateTraceId()

    /** W3C traceparent: `00-<32 hex trace-id>-<16 hex parent-id>-<flags>` */
    private fun traceParentTraceId(header: String?): String? {
        val parts = header?.split('-') ?: return null
        val traceId = parts.getOrNull(1) ?: return null
        return traceId.takeIf { it.length == TRACE_ID_LENGTH && it.all(Char::isLetterOrDigit) && it != ZERO_TRACE_ID }
    }

    private fun generateTraceId(): String {
        val bytes = ByteArray(TRACE_ID_LENGTH / 2)
        random.nextBytes(bytes)
        return HexFormat.of().formatHex(bytes)
    }

    companion object {
        /** Spring Security 의 -100 보다 앞. */
        const val ORDER = -110
        const val TRACE_ID_ATTRIBUTE = "com.hyunolike.foundation.traceId"
        private const val TRACEPARENT_HEADER = "traceparent"
        private const val TRACE_ID_LENGTH = 32
        private const val ZERO_TRACE_ID = "00000000000000000000000000000000"
    }
}
