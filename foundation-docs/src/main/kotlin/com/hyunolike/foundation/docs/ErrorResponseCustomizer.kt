package com.hyunolike.foundation.docs

import com.hyunolike.foundation.core.error.CommonErrorCode
import com.hyunolike.foundation.core.error.ErrorCode
import com.hyunolike.foundation.core.error.ErrorCodeCatalog
import com.hyunolike.foundation.web.support.ErrorMessageResolver
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import org.springdoc.core.customizers.OperationCustomizer
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.web.method.HandlerMethod
import io.swagger.v3.oas.models.responses.ApiResponse as SwaggerApiResponse

/**
 * `@ApiErrorCodes` 와 전역 기본 응답을 문서의 실패 응답으로 옮긴다.
 *
 * 클래스 이름 충돌에 주의 — swagger 의 `ApiResponse` 와 우리 봉투의 `ApiResponse` 는
 * 다른 타입이다. 여기서는 swagger 쪽을 별칭으로 가져온다. (설계 문서 §6.3)
 */
class ErrorResponseCustomizer(
    private val catalog: ErrorCodeCatalog,
    private val messages: ErrorMessageResolver,
    /** 스펙이 요청자의 Accept-Language 에 따라 달라지면 커밋된 스냅샷과 비교할 수 없다. */
    private val documentationLocale: java.util.Locale = java.util.Locale.KOREAN,
    private val globalErrorCodes: List<ErrorCode> =
        listOf(
            CommonErrorCode.COMMON_INVALID_PARAMETER,
            CommonErrorCode.COMMON_UNAUTHORIZED,
            CommonErrorCode.COMMON_FORBIDDEN,
            CommonErrorCode.COMMON_INTERNAL_ERROR,
        ),
) : OperationCustomizer {
    override fun customize(
        operation: Operation,
        handlerMethod: HandlerMethod,
    ): Operation {
        val declared =
            (
                AnnotatedElementUtils.findMergedAnnotation(handlerMethod.method, ApiErrorCodes::class.java)?.value
                    ?: emptyArray()
            ).toList() +
                (
                    AnnotatedElementUtils.findMergedAnnotation(handlerMethod.beanType, ApiErrorCodes::class.java)?.value
                        ?: emptyArray()
                ).toList()

        val declaredCodes = declared.distinct().map(catalog::require)

        (declaredCodes + globalErrorCodes)
            .distinctBy { it.code }
            .groupBy { it.status }
            .forEach { (status, codes) -> putResponse(operation, status, codes) }

        return operation
    }

    private fun putResponse(
        operation: Operation,
        status: Int,
        codes: List<ErrorCode>,
    ) {
        val responses = operation.responses ?: return
        val description =
            codes.joinToString(" · ") { "${it.code} — ${messages.resolve(it, emptyList(), documentationLocale)}" }

        responses
            .computeIfAbsent(status.toString()) {
                SwaggerApiResponse()
                    .content(
                        Content().addMediaType(
                            org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
                            MediaType().schema(
                                Schema<Any>().`$ref`("#/components/schemas/${EnvelopeSchemas.ERROR_ENVELOPE}"),
                            ),
                        ),
                    )
            }.description = description
    }
}
