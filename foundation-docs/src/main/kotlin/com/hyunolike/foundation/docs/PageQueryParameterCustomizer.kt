package com.hyunolike.foundation.docs

import com.hyunolike.foundation.web.config.FoundationWebProperties
import com.hyunolike.foundation.web.resolver.PageQuery
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.Schema
import org.springdoc.core.customizers.OperationCustomizer
import org.springframework.web.method.HandlerMethod

/**
 * 페이징 파라미터를 문서에 올바르게 싣는다.
 *
 * springdoc 은 [PageQuery] 를 그냥 펼쳐서 모든 생성자 인자를 필수 쿼리 파라미터로 적는다.
 * 실제로는 리졸버가 기본값을 채우므로 전부 선택 파라미터다. (설계 문서 §7.3)
 */
class PageQueryParameterCustomizer(
    private val paging: FoundationWebProperties.Paging,
) : OperationCustomizer {
    override fun customize(
        operation: Operation,
        handlerMethod: HandlerMethod,
    ): Operation {
        val usesPageQuery = handlerMethod.methodParameters.any { it.parameterType == PageQuery::class.java }
        if (!usesPageQuery) return operation

        operation.parameters?.forEach { parameter ->
            when (parameter.name) {
                paging.pageParameter -> {
                    parameter.required = false
                    parameter.description = "0부터 시작하는 페이지 번호"
                    parameter.schema?.setDefaultValue(0)
                }

                paging.sizeParameter -> {
                    parameter.required = false
                    parameter.description = "페이지 크기 (1 이상 ${paging.maxSize} 이하)"
                    parameter.schema?.setDefaultValue(paging.defaultSize)
                }

                paging.sortParameter -> {
                    parameter.required = false
                    parameter.description = "정렬 기준. 예: createdAt,desc"
                }
            }
        }
        return operation
    }

    @Suppress("UNCHECKED_CAST")
    private fun Schema<*>.setDefaultValue(value: Any) {
        (this as Schema<Any>).setDefault(value)
    }
}
