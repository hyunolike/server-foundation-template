package com.hyunolike.foundation.test

import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse
import org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint
import org.springframework.restdocs.payload.FieldDescriptor
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.payload.ResponseFieldsSnippet
import org.springframework.restdocs.snippet.Snippet
import org.springframework.test.web.servlet.ResultHandler

/**
 * 검증의 첫 번째 겹. (설계 문서 §7.4)
 *
 * REST Docs 가 보장하는 것은 하나뿐이다 — 문서에 실린 예시와 필드 설명이 실제로 테스트가
 * 받아 낸 응답에서 나왔다는 것. 응답에 없는 필드를 문서에 적거나, 응답에 있는 필드를
 * 문서에서 빠뜨리면 테스트가 깨진다.
 *
 * 스펙 전체가 맞는지는 보지 않는다 — 그쪽은 [OpenApiResponseValidator] 의 몫이다.
 */
object RestDocsSupport {
    /** 성공 응답의 봉투 필드. `data` 하위는 각 엔드포인트가 채운다. */
    fun successEnvelopeFields(): List<FieldDescriptor> =
        listOf(
            fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("2xx 면 true. HTTP 상태와 항상 일치한다."),
            fieldWithPath("error").type(JsonFieldType.NULL).description("성공 응답에서는 항상 null."),
        ) + metaFields()

    /** 실패 응답의 봉투 필드. 모든 실패가 이 모양을 공유한다. */
    fun errorEnvelopeFields(): List<FieldDescriptor> =
        listOf(
            fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("실패 응답에서는 항상 false."),
            fieldWithPath("data").type(JsonFieldType.NULL).description("실패 응답에서는 항상 null."),
            fieldWithPath("error.code").type(JsonFieldType.STRING).description("클라이언트가 분기에 쓰는 유일한 값."),
            fieldWithPath("error.message").type(JsonFieldType.STRING).description("사용자에게 그대로 보여줄 문장."),
            fieldWithPath("error.details")
                .type(JsonFieldType.ARRAY)
                .optional()
                .description("필드 단위 사유. 검증 실패에서만 채운다."),
            fieldWithPath("error.details[].field").type(JsonFieldType.STRING).optional().description("실패한 필드."),
            fieldWithPath("error.details[].reason").type(JsonFieldType.STRING).optional().description("실패 사유."),
        ) + metaFields()

    private fun metaFields(): List<FieldDescriptor> =
        listOf(
            fieldWithPath("meta.traceId")
                .type(JsonFieldType.STRING)
                .description("응답 헤더 X-Trace-Id, 로그의 traceId 와 같은 값."),
            fieldWithPath("meta.timestamp").type(JsonFieldType.STRING).description("응답 생성 시각 (UTC, 밀리초)."),
            fieldWithPath("meta.path").type(JsonFieldType.STRING).description("요청 경로."),
        )

    /** 봉투 필드를 앞에 두고, 엔드포인트가 채우는 `data` 하위 필드를 잇는다. */
    fun successResponseFields(vararg dataFields: FieldDescriptor): ResponseFieldsSnippet =
        responseFields(successEnvelopeFields() + dataFields.toList())

    fun errorResponseFields(): ResponseFieldsSnippet = responseFields(errorEnvelopeFields())

    /** 요청·응답을 보기 좋게 정리해 스니펫으로 남긴다. */
    fun documentWith(
        identifier: String,
        vararg snippets: Snippet,
    ): ResultHandler =
        document(
            identifier,
            preprocessRequest(prettyPrint()),
            preprocessResponse(prettyPrint()),
            *snippets,
        )
}
