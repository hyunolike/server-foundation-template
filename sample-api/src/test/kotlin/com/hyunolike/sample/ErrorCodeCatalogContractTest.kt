package com.hyunolike.sample

import com.hyunolike.foundation.core.error.ErrorCodeCatalog
import com.hyunolike.foundation.test.ErrorCodeContract
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.assertTrue

/**
 * 애플리케이션이 실제로 쓰는 모든 에러 코드를 검사한다. (설계 문서 §6.3)
 *
 * `foundation-core` 의 테스트는 공통 코드만 본다. 도메인 코드는 도메인이 검사해야 하고,
 * 그 출발점은 문서 생성기가 읽는 것과 같은 `ErrorCodeCatalog` 다 — 카탈로그에 없는 코드는
 * 어차피 문서에도 실리지 않는다.
 */
@SpringBootTest
class ErrorCodeCatalogContractTest {
    @Autowired
    private lateinit var errorCodeCatalog: ErrorCodeCatalog

    @Test
    fun `카탈로그의 모든 에러 코드가 계약을 만족한다`() {
        val knownMessageKeys =
            ErrorCodeContract.messageKeysFrom("foundation-messages.properties") +
                ErrorCodeContract.messageKeysFrom("messages.properties")

        ErrorCodeContract.verify(errorCodeCatalog.all, knownMessageKeys)
    }

    @Test
    fun `도메인 코드가 카탈로그에 실제로 들어 있다`() {
        assertTrue(
            errorCodeCatalog.all.any { it.code.startsWith("USER_") },
            "도메인 에러 코드가 카탈로그에 없습니다. SampleApiDocsConfiguration 을 확인하세요.",
        )
    }
}
