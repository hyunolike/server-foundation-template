package com.hyunolike.foundation.web.filter

import com.hyunolike.foundation.observability.logging.MdcKeys
import com.hyunolike.foundation.observability.logging.RequestLogEntry
import com.hyunolike.foundation.observability.logging.StructuredLogWriter
import com.hyunolike.foundation.web.config.FoundationWebProperties
import com.hyunolike.foundation.web.support.PathExclusions
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.ContentCachingRequestWrapper

/**
 * 요청 한 건을 한 줄 JSON 으로 남긴다. 마스킹은 [StructuredLogWriter] 가 들고 있는
 * 규칙 하나만 거친다. (설계 문서 §5.1)
 */
class RequestLoggingFilter(
    private val properties: FoundationWebProperties,
    private val logWriter: StructuredLogWriter,
) : OncePerRequestFilter() {
    private val log = LoggerFactory.getLogger(RequestLoggingFilter::class.java)
    private val exclusions = PathExclusions(properties.logging.excludedPathPatterns)

    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        !properties.logging.enabled || exclusions.matches(request.requestURI)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val cached = if (properties.logging.includeRequestBody) ContentCachingRequestWrapper(request) else null
        val effectiveRequest = cached ?: request
        val startedAt = System.nanoTime()
        try {
            filterChain.doFilter(effectiveRequest, response)
        } finally {
            val entry =
                RequestLogEntry(
                    traceId = MDC.get(MdcKeys.TRACE_ID) ?: "unknown",
                    method = request.method,
                    path = request.requestURI,
                    status = response.status,
                    durationMs = (System.nanoTime() - startedAt) / 1_000_000,
                    query = request.queryString,
                    clientIp = request.remoteAddr,
                    headers = headersOf(request),
                    requestBody = cached?.let { bodyOf(it) },
                )
            log.info(logWriter.write(entry))
        }
    }

    private fun headersOf(request: HttpServletRequest): Map<String, String> =
        request.headerNames
            .asSequence()
            .associateWith { request.getHeader(it) ?: "" }

    private fun bodyOf(request: ContentCachingRequestWrapper): String? =
        request.contentAsByteArray
            .takeIf { it.isNotEmpty() }
            ?.let { String(it, Charsets.UTF_8) }
            ?.take(properties.logging.maxBodyLength)

    companion object {
        /** TraceIdFilter 다음, Spring Security(-100) 앞. */
        const val ORDER = -105
    }
}
