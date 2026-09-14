package com.hyunolike.foundation.observability.logging

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode

/**
 * 요청 한 건을 한 줄 JSON 으로 남긴다.
 *
 * 로그 수집기가 파싱할 수 있도록 구조를 고정하고, 값은 [MaskingRules] 를 거쳐 나간다.
 */
class StructuredLogWriter(
    private val objectMapper: ObjectMapper = ObjectMapper(),
    private val maskingRules: MaskingRules = MaskingRules(),
) {
    fun write(entry: RequestLogEntry): String {
        val node: ObjectNode = objectMapper.createObjectNode()
        node.put("traceId", entry.traceId)
        node.put("method", entry.method)
        node.put("path", entry.path)
        node.put("status", entry.status)
        node.put("durationMs", entry.durationMs)
        entry.query?.let { node.put("query", it) }
        entry.clientIp?.let { node.put("clientIp", it) }
        entry.errorCode?.let { node.put("errorCode", it) }

        if (entry.headers.isNotEmpty()) {
            val headers = node.putObject("headers")
            entry.headers.forEach { (name, value) -> headers.put(name, maskingRules.maskHeader(name, value)) }
        }
        entry.requestBody?.takeIf { it.isNotBlank() }?.let { node.put("requestBody", maskingRules.maskBody(it)) }

        return objectMapper.writeValueAsString(node)
    }
}

data class RequestLogEntry(
    val traceId: String,
    val method: String,
    val path: String,
    val status: Int,
    val durationMs: Long,
    val query: String? = null,
    val clientIp: String? = null,
    val errorCode: String? = null,
    val headers: Map<String, String> = emptyMap(),
    val requestBody: String? = null,
)
