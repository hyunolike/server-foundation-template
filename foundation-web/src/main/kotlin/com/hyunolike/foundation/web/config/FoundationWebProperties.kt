package com.hyunolike.foundation.web.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 파운데이션의 동작을 여는 유일한 손잡이.
 *
 * 기본값은 전부 코드에 박혀 있다 — 프로퍼티를 건드리지 않아도 표준이 서야 하기 때문이다.
 */
@ConfigurationProperties(prefix = "foundation.web")
data class FoundationWebProperties(
    /**
     * 표준 봉투를 씌우지 않을 경로.
     *
     * 이 목록이 길어지면 표준이 무너진 신호다. (설계 문서 §5.4)
     */
    val excludedPathPatterns: List<String> =
        listOf(
            "/actuator/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
        ),
    val traceIdHeader: String = "X-Trace-Id",
    val requestIdHeader: String = "X-Request-Id",
    val logging: Logging = Logging(),
    val paging: Paging = Paging(),
) {
    data class Logging(
        val enabled: Boolean = true,
        val includeRequestBody: Boolean = true,
        val maxBodyLength: Int = 2_000,
        val excludedPathPatterns: List<String> = listOf("/actuator/**"),
    )

    data class Paging(
        val defaultSize: Int = 20,
        val maxSize: Int = 200,
        val pageParameter: String = "page",
        val sizeParameter: String = "size",
        val sortParameter: String = "sort",
    )
}
