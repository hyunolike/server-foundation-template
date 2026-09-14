package com.hyunolike.foundation.web.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.hyunolike.foundation.observability.logging.MaskingRules
import com.hyunolike.foundation.observability.logging.StructuredLogWriter
import com.hyunolike.foundation.web.advice.FoundationErrorController
import com.hyunolike.foundation.web.advice.GlobalExceptionHandler
import com.hyunolike.foundation.web.advice.ResponseEnvelopeAdvice
import com.hyunolike.foundation.web.filter.RequestLoggingFilter
import com.hyunolike.foundation.web.filter.TraceIdFilter
import com.hyunolike.foundation.web.resolver.PageQueryArgumentResolver
import com.hyunolike.foundation.web.support.ErrorMessageResolver
import com.hyunolike.foundation.web.support.ResponseMetaFactory
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.MessageSource
import org.springframework.context.annotation.Bean
import org.springframework.context.support.ReloadableResourceBundleMessageSource
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * 애플리케이션이 의존성만 추가하면 표준이 켜지도록 한다. (설계 문서 §8)
 *
 * 등록 지점은 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
 * 다. Boot 3 는 `spring.factories` 를 더 이상 읽지 않는다.
 *
 * `ErrorMvcAutoConfiguration` 보다 먼저 등록되어야 한다 — Boot 의 `BasicErrorController` 는
 * `ErrorController` 빈이 없을 때만 만들어지므로, 순서가 뒤집히면 `/error` 매핑이 둘이 되어
 * 컨텍스트가 아예 기동하지 못한다.
 */
@AutoConfiguration(before = [ErrorMvcAutoConfiguration::class])
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(FoundationWebProperties::class)
class WebAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    fun responseMetaFactory(): ResponseMetaFactory = ResponseMetaFactory()

    @Bean
    @ConditionalOnMissingBean(name = ["foundationMessageSource"])
    fun foundationMessageSource(): MessageSource =
        ReloadableResourceBundleMessageSource().apply {
            // 애플리케이션의 messages 가 먼저, 파운데이션 기본 문구가 뒤.
            setBasenames("classpath:messages", "classpath:foundation-messages")
            setDefaultEncoding("UTF-8")
            setFallbackToSystemLocale(false)
        }

    @Bean
    @ConditionalOnMissingBean
    fun errorMessageResolver(foundationMessageSource: MessageSource): ErrorMessageResolver = ErrorMessageResolver(foundationMessageSource)

    @Bean
    @ConditionalOnMissingBean
    fun maskingRules(): MaskingRules = MaskingRules()

    @Bean
    @ConditionalOnMissingBean
    fun structuredLogWriter(
        objectMapper: ObjectMapper,
        maskingRules: MaskingRules,
    ): StructuredLogWriter = StructuredLogWriter(objectMapper, maskingRules)

    @Bean
    fun responseEnvelopeAdvice(
        properties: FoundationWebProperties,
        responseMetaFactory: ResponseMetaFactory,
    ): ResponseEnvelopeAdvice = ResponseEnvelopeAdvice(properties, responseMetaFactory)

    @Bean
    fun globalExceptionHandler(
        responseMetaFactory: ResponseMetaFactory,
        errorMessageResolver: ErrorMessageResolver,
    ): GlobalExceptionHandler = GlobalExceptionHandler(responseMetaFactory, errorMessageResolver)

    @Bean
    fun foundationErrorController(
        responseMetaFactory: ResponseMetaFactory,
        errorMessageResolver: ErrorMessageResolver,
    ): FoundationErrorController = FoundationErrorController(responseMetaFactory, errorMessageResolver)

    @Bean
    fun traceIdFilterRegistration(properties: FoundationWebProperties): FilterRegistrationBean<TraceIdFilter> =
        FilterRegistrationBean(TraceIdFilter(properties)).apply { order = TraceIdFilter.ORDER }

    @Bean
    fun requestLoggingFilterRegistration(
        properties: FoundationWebProperties,
        structuredLogWriter: StructuredLogWriter,
    ): FilterRegistrationBean<RequestLoggingFilter> =
        FilterRegistrationBean(RequestLoggingFilter(properties, structuredLogWriter))
            .apply { order = RequestLoggingFilter.ORDER }

    @Bean
    fun foundationWebMvcConfigurer(properties: FoundationWebProperties): WebMvcConfigurer =
        object : WebMvcConfigurer {
            override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
                resolvers.add(PageQueryArgumentResolver(properties.paging))
            }
        }
}
