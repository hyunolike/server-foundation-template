package com.hyunolike.foundation.core.response

/**
 * 페이징 응답의 고정 형태. (설계 문서 §6.4)
 *
 * Spring 의 `Page` 를 그대로 직렬화하지 않는다 — 내부 구조가 버전에 따라 바뀌므로
 * API 계약으로 쓸 수 없다.
 */
data class PageResponse<out T>(
    val content: List<T>,
    val page: PageMeta,
) {
    companion object {
        fun <T> of(
            content: List<T>,
            number: Int,
            size: Int,
            totalElements: Long,
            sort: String? = null,
        ): PageResponse<T> =
            PageResponse(
                content = content,
                page =
                    PageMeta(
                        number = number,
                        size = size,
                        totalElements = totalElements,
                        totalPages = if (size <= 0) 0 else ((totalElements + size - 1) / size).toInt(),
                        sort = sort,
                    ),
            )
    }
}

data class PageMeta(
    val number: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val sort: String?,
)
