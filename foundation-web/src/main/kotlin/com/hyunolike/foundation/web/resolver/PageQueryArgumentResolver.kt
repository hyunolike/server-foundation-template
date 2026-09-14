package com.hyunolike.foundation.web.resolver

import com.hyunolike.foundation.core.error.BusinessException
import com.hyunolike.foundation.core.error.CommonErrorCode
import com.hyunolike.foundation.core.response.ApiErrorDetail
import com.hyunolike.foundation.web.config.FoundationWebProperties
import org.springframework.core.MethodParameter
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

/**
 * 모든 목록 API 가 같은 쿼리 파라미터를 쓰도록 강제한다.
 *
 * 잘못된 값은 여기서 [BusinessException] 으로 바꿔 던진다 — 검증 실패의 응답 모양이
 * 본문 검증 실패와 같아지도록.
 */
class PageQueryArgumentResolver(
    private val paging: FoundationWebProperties.Paging,
) : HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter): Boolean = PageQuery::class.java == parameter.parameterType

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): PageQuery {
        val details = mutableListOf<ApiErrorDetail>()

        val page = intParameter(webRequest, paging.pageParameter, default = 0, details = details)
        val size = intParameter(webRequest, paging.sizeParameter, default = paging.defaultSize, details = details)

        if (page < 0) {
            details += ApiErrorDetail(paging.pageParameter, "0 이상이어야 합니다.")
        }
        if (size !in 1..paging.maxSize) {
            details += ApiErrorDetail(paging.sizeParameter, "1 이상 ${paging.maxSize} 이하여야 합니다.")
        }
        if (details.isNotEmpty()) {
            throw BusinessException(CommonErrorCode.COMMON_INVALID_PARAMETER, details)
        }

        return PageQuery(page = page, size = size, sort = webRequest.getParameter(paging.sortParameter))
    }

    private fun intParameter(
        webRequest: NativeWebRequest,
        name: String,
        default: Int,
        details: MutableList<ApiErrorDetail>,
    ): Int {
        val raw = webRequest.getParameter(name) ?: return default
        return raw.toIntOrNull() ?: run {
            details += ApiErrorDetail(name, "정수여야 합니다.")
            default
        }
    }
}
