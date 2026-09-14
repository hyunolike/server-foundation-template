package com.hyunolike.foundation.docs

import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.headers.Header
import io.swagger.v3.oas.models.media.StringSchema
import org.springdoc.core.customizers.OperationCustomizer
import org.springframework.web.method.HandlerMethod

/**
 * 성공 응답의 스키마를 봉투로 바꾸고, 공통 응답 헤더를 붙인다.
 *
 * 컨트롤러가 `UserResponse` 를 반환해도 실제로 나가는 것은 `ApiResponse<UserResponse>` 다.
 * 그 간극을 문서 쪽에서 메운다. (설계 문서 §7)
 */
class EnvelopeSchemaCustomizer : OperationCustomizer {
    override fun customize(
        operation: Operation,
        handlerMethod: HandlerMethod,
    ): Operation {
        operation.responses?.forEach { (status, response) ->
            if (!status.startsWith("2")) return@forEach

            response.content?.let { content ->
                // 핸들러가 produces 를 적지 않으면 springdoc 은 `*/*` 로 적는다.
                // 봉투는 JSON 에서만 씌워지므로 문서의 미디어 타입도 그에 맞춘다.
                content.remove(ANY_MEDIA_TYPE)?.let { any -> content.putIfAbsent(JSON_MEDIA_TYPE, any) }
                content.values.forEach { mediaType ->
                    mediaType.schema?.let { original ->
                        mediaType.schema = EnvelopeSchemas.envelope(original)
                    }
                }
            }

            val headers = response.headers ?: linkedMapOf<String, Header>().also { response.headers = it }
            headers.putIfAbsent(
                TRACE_ID_HEADER,
                Header()
                    .description("이 요청의 traceId. meta.traceId, 로그의 traceId 와 같은 값이다.")
                    .schema(StringSchema()),
            )
        }
        return operation
    }

    companion object {
        const val TRACE_ID_HEADER = "X-Trace-Id"
        private const val ANY_MEDIA_TYPE = "*/*"
        private const val JSON_MEDIA_TYPE = "application/json"
    }
}
