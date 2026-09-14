package com.hyunolike.foundation.test

import org.springframework.test.web.servlet.ResultMatcher
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath

/**
 * 봉투 규격을 테스트에서 한 줄로 단언한다.
 *
 * 각 서비스가 봉투 검사를 저마다 다시 짜면, 검사 자체가 표준에서 벗어난다.
 */
object ApiResponseAssertions {
    /** 성공·실패와 무관하게 네 개의 최상위 키가 모두 존재하는지. (설계 문서 §6.2) */
    fun isEnvelope(): ResultMatcher =
        ResultMatcher.matchAll(
            jsonPath("$.success").exists(),
            jsonPath("$").value(org.hamcrest.Matchers.hasKey<String>("data")),
            jsonPath("$").value(org.hamcrest.Matchers.hasKey<String>("error")),
            jsonPath("$.meta.traceId").isNotEmpty,
            jsonPath("$.meta.timestamp").isNotEmpty,
            jsonPath("$.meta.path").isNotEmpty,
        )

    fun isSuccess(): ResultMatcher =
        ResultMatcher.matchAll(
            isEnvelope(),
            jsonPath("$.success").value(true),
            jsonPath("$.error").doesNotExist(),
        )

    fun isFailure(errorCode: String): ResultMatcher =
        ResultMatcher.matchAll(
            isEnvelope(),
            jsonPath("$.success").value(false),
            jsonPath("$.data").doesNotExist(),
            jsonPath("$.error.code").value(errorCode),
            jsonPath("$.error.message").isNotEmpty,
        )

    fun hasFieldError(field: String): ResultMatcher = jsonPath("$.error.details[?(@.field == '$field')]").exists()
}
