package com.hyunolike.foundation.docs

import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.BooleanSchema
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema

/**
 * 문서에 넣을 봉투 스키마를 만든다.
 *
 * springdoc 은 컨트롤러 시그니처만 보므로 봉투를 모른다. 여기서 만든 스키마를 되돌려
 * 넣는 것이 이 모듈의 존재 이유다. (설계 문서 §3.3)
 */
object EnvelopeSchemas {
    const val API_ERROR = "ApiError"
    const val API_ERROR_DETAIL = "ApiErrorDetail"
    const val RESPONSE_META = "ResponseMeta"
    const val ERROR_ENVELOPE = "ApiResponseVoid"

    fun components(): Map<String, Schema<*>> =
        mapOf(
            API_ERROR_DETAIL to
                ObjectSchema().apply {
                    addProperty("field", StringSchema().description("실패한 필드"))
                    addProperty("reason", StringSchema().description("실패 사유"))
                    required = listOf("field", "reason")
                },
            API_ERROR to
                ObjectSchema().apply {
                    addProperty("code", StringSchema().description("클라이언트가 분기에 쓰는 값"))
                    addProperty("message", StringSchema().description("사용자에게 보여줄 문장"))
                    addProperty(
                        "details",
                        ArraySchema()
                            .items(Schema<Any>().`$ref`("#/components/schemas/$API_ERROR_DETAIL"))
                            .nullable(true)
                            .description("필드 단위 사유. 검증 실패에서만 채운다."),
                    )
                    required = listOf("code", "message")
                },
            RESPONSE_META to
                ObjectSchema().apply {
                    addProperty("traceId", StringSchema().description("응답 헤더 X-Trace-Id, 로그의 traceId 와 같은 값"))
                    addProperty("timestamp", StringSchema().description("응답 생성 시각 (UTC, 밀리초)"))
                    addProperty("path", StringSchema().description("요청 경로"))
                    required = listOf("traceId", "timestamp", "path")
                },
            ERROR_ENVELOPE to envelope(dataSchema = Schema<Any>().nullable(true).description("실패 응답의 data 는 항상 null")),
        )

    /** 주어진 payload 스키마를 봉투로 감싼다. 네 개의 최상위 키는 항상 존재한다. */
    fun envelope(dataSchema: Schema<*>): Schema<*> =
        ObjectSchema().apply {
            addProperty("success", BooleanSchema().description("2xx 면 true. HTTP 상태와 항상 일치한다."))
            addProperty("data", dataSchema)
            addProperty("error", Schema<Any>().`$ref`("#/components/schemas/$API_ERROR").nullable(true))
            addProperty("meta", Schema<Any>().`$ref`("#/components/schemas/$RESPONSE_META"))
            required = listOf("success", "data", "error", "meta")
        }
}
