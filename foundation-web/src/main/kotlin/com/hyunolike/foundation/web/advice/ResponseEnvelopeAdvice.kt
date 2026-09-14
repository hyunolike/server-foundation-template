package com.hyunolike.foundation.web.advice

import com.hyunolike.foundation.core.response.ApiResponse
import com.hyunolike.foundation.web.config.FoundationWebProperties
import com.hyunolike.foundation.web.support.PathExclusions
import com.hyunolike.foundation.web.support.ResponseMetaFactory
import org.springframework.core.MethodParameter
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice

/**
 * 정상 경로에서 봉투를 씌우는 유일한 지점. (설계 문서 §5.2)
 *
 * [supports] 가 Jackson 컨버터만 받는 것이 중요하다. `String` 을 반환하는 핸들러는
 * `StringHttpMessageConverter` 가 선택되므로, 여기서 봉투 객체를 돌려주면
 * `ClassCastException` 이 난다. 컨버터 타입으로 거르면 그 경우가 애초에 오지 않는다.
 */
@RestControllerAdvice
class ResponseEnvelopeAdvice(
    properties: FoundationWebProperties,
    private val metaFactory: ResponseMetaFactory,
) : ResponseBodyAdvice<Any> {
    private val exclusions = PathExclusions(properties.excludedPathPatterns)

    override fun supports(
        returnType: MethodParameter,
        converterType: Class<out HttpMessageConverter<*>>,
    ): Boolean = AbstractJackson2HttpMessageConverter::class.java.isAssignableFrom(converterType)

    override fun beforeBodyWrite(
        body: Any?,
        returnType: MethodParameter,
        selectedContentType: MediaType,
        selectedConverterType: Class<out HttpMessageConverter<*>>,
        request: ServerHttpRequest,
        response: ServerHttpResponse,
    ): Any? {
        // 예외 핸들러와 /error 컨트롤러가 이미 만든 봉투는 두 번 감싸지 않는다.
        if (body is ApiResponse<*>) return body

        val path = request.uri.path
        if (exclusions.matches(path)) return body

        return ApiResponse.success(body, metaFactory.create(path))
    }
}
