package com.hyunolike.foundation.docs

import com.hyunolike.foundation.core.error.CommonErrorCode
import com.hyunolike.foundation.core.error.ErrorCodeCatalog
import com.hyunolike.foundation.web.config.FoundationWebProperties
import com.hyunolike.foundation.web.support.ErrorMessageResolver
import io.swagger.v3.oas.models.Components
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean

/**
 * 문서 자동화도 의존성만 추가하면 켜진다.
 *
 * 애플리케이션이 해야 할 일은 [ErrorCodeCatalog] 빈 하나를 등록하는 것뿐이다 —
 * 어떤 도메인 에러 코드를 쓰는지는 애플리케이션만 안다.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
class DocsAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    fun errorCodeCatalog(): ErrorCodeCatalog = ErrorCodeCatalog(CommonErrorCode.entries.toList())

    @Bean
    fun envelopeSchemaCustomizer(): EnvelopeSchemaCustomizer = EnvelopeSchemaCustomizer()

    @Bean
    fun pageQueryParameterCustomizer(properties: FoundationWebProperties): PageQueryParameterCustomizer =
        PageQueryParameterCustomizer(properties.paging)

    /**
     * `/error` 는 봉투를 내보내는 내부 경로일 뿐, API 가 아니다.
     * 문서에 실리면 클라이언트가 호출해도 되는 엔드포인트로 읽힌다.
     */
    @Bean
    fun hideErrorPathCustomizer(): OpenApiCustomizer = OpenApiCustomizer { openApi -> openApi.paths?.remove(ERROR_PATH) }

    @Bean
    fun errorResponseCustomizer(
        errorCodeCatalog: ErrorCodeCatalog,
        errorMessageResolver: ErrorMessageResolver,
    ): ErrorResponseCustomizer = ErrorResponseCustomizer(errorCodeCatalog, errorMessageResolver)

    /** 봉투가 참조하는 공통 스키마를 components 에 등록한다. */
    @Bean
    fun envelopeComponentsCustomizer(): OpenApiCustomizer =
        OpenApiCustomizer { openApi ->
            val components = openApi.components ?: Components().also { openApi.components = it }
            EnvelopeSchemas.components().forEach { (name, schema) ->
                components.addSchemas(name, schema)
            }
        }

    companion object {
        private const val ERROR_PATH = "/error"
    }
}
