package com.hyunolike.sample.config

import com.hyunolike.foundation.core.error.CommonErrorCode
import com.hyunolike.foundation.core.error.ErrorCodeCatalog
import com.hyunolike.sample.user.UserErrorCode
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * 애플리케이션이 파운데이션에 알려 줘야 하는 유일한 것: 우리가 쓰는 에러 코드 목록.
 *
 * 여기 빠진 코드를 `@ApiErrorCodes` 에 적으면 문서가 아니라 테스트가 깨진다.
 */
@Configuration
class SampleApiDocsConfiguration {
    @Bean
    fun errorCodeCatalog(): ErrorCodeCatalog = ErrorCodeCatalog(CommonErrorCode.entries + UserErrorCode.entries)

    @Bean
    fun sampleApiOpenApi(): OpenAPI =
        OpenAPI().info(
            Info()
                .title("Sample API")
                .version("v1")
                .description("server-foundation-template 의 참조 구현."),
        )
}
