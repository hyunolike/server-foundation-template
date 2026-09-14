package com.hyunolike.sampletest

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 미처리 예외 경로를 확인하기 위한 테스트 전용 엔드포인트.
 *
 * 패키지가 `com.hyunolike.sampletest` 인 것은 의도다 — 애플리케이션의 컴포넌트 스캔은
 * `com.hyunolike.sample` 아래만 훑으므로, 이 컨트롤러는 명시적으로 @Import 한 테스트에서만
 * 살아난다. 같은 패키지에 두면 생성된 OpenAPI 스냅샷에 `/api/v1/boom` 이 섞인다.
 */
@RestController
class ExplodingController {
    @GetMapping("/api/v1/boom")
    fun boom(): Nothing = throw IllegalStateException(SECRET)

    companion object {
        const val SECRET = "내부 커넥션 문자열 jdbc:postgresql://secret-host/db"
    }
}
