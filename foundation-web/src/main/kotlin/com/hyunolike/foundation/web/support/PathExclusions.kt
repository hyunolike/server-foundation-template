package com.hyunolike.foundation.web.support

import org.springframework.util.AntPathMatcher

/** Ant 패턴 목록으로 경로를 걸러 낸다. 제외 규칙이 여러 군데 흩어지지 않도록 한 곳에 둔다. */
class PathExclusions(
    private val patterns: List<String>,
) {
    private val matcher = AntPathMatcher()

    fun matches(path: String): Boolean = patterns.any { matcher.match(it, path) }
}
