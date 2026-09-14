package com.hyunolike.foundation.web.resolver

/**
 * 페이징 요청의 표준 타입.
 *
 * 이름이 `PageRequest` 가 아닌 이유: `org.springframework.data.domain.PageRequest` 가
 * 이미 그 이름을 쓰고 있고, Spring Data Web 이 클래스패스에 있으면
 * `PageableHandlerMethodArgumentResolver` 가 같은 인자를 두고 경쟁한다. (설계 문서 §6.4)
 */
data class PageQuery(
    val page: Int,
    val size: Int,
    val sort: String?,
) {
    /**
     * 프로퍼티가 아니라 함수인 것은 의도다 — springdoc 은 게터를 쿼리 파라미터로 읽으므로,
     * 프로퍼티로 두면 `offset` 이 필수 파라미터로 문서에 실린다.
     */
    fun offset(): Long = page.toLong() * size
}
