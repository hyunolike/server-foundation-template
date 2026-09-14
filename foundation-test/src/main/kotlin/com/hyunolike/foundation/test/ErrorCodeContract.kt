package com.hyunolike.foundation.test

import com.hyunolike.foundation.core.error.ErrorCode

/**
 * 에러 코드 레지스트리가 지켜야 할 규칙. 각 모듈의 테스트가 이 검사기를 호출한다.
 *
 * "코드는 있는데 상태가 안 맞는" 응답을 빌드에서 막는 것이 목적이다. (설계 문서 §6.3)
 */
object ErrorCodeContract {
    private val NAME_PATTERN = Regex("^[A-Z][A-Z0-9]*(_[A-Z0-9]+)+$")

    private val ALLOWED_STATUSES = (400..431) + (500..511)

    /**
     * @param codes 검사할 에러 코드 목록
     * @param knownMessageKeys 메시지 번들에 실재하는 키. 비워 두면 키 존재 검사를 건너뛴다.
     */
    fun verify(
        codes: List<ErrorCode>,
        knownMessageKeys: Set<String> = emptySet(),
    ) {
        check(codes.isNotEmpty()) { "검사할 에러 코드가 없습니다." }

        val violations = mutableListOf<String>()

        codes
            .groupBy { it.code }
            .filterValues { it.size > 1 }
            .forEach { (code, duplicates) ->
                violations += "코드 문자열이 중복됩니다: $code (${duplicates.size}건)"
            }

        codes.forEach { errorCode ->
            if (!NAME_PATTERN.matches(errorCode.code)) {
                violations += "${errorCode.code} — 코드는 <DOMAIN>_<REASON> 대문자 스네이크여야 합니다."
            }
            if (errorCode.status !in ALLOWED_STATUSES) {
                violations += "${errorCode.code} — status ${errorCode.status} 는 유효한 실패 HTTP 상태가 아닙니다."
            }
            if (errorCode.messageKey.isBlank()) {
                violations += "${errorCode.code} — messageKey 가 비어 있습니다."
            }
            if (knownMessageKeys.isNotEmpty() && errorCode.messageKey !in knownMessageKeys) {
                violations += "${errorCode.code} — messageKey '${errorCode.messageKey}' 가 메시지 번들에 없습니다."
            }
            val enumName = (errorCode as? Enum<*>)?.name
            if (enumName != null && enumName != errorCode.code) {
                violations += "$enumName — enum 이름과 code('${errorCode.code}') 가 다릅니다."
            }
        }

        check(violations.isEmpty()) {
            "에러 코드 계약 위반 ${violations.size}건:\n" + violations.joinToString("\n") { "  - $it" }
        }
    }

    /** 프로퍼티 파일에서 메시지 키를 읽어 온다. 클래스패스 기준 경로를 넘긴다. */
    fun messageKeysFrom(resourcePath: String): Set<String> {
        val stream =
            ErrorCodeContract::class.java.classLoader.getResourceAsStream(resourcePath)
                ?: error("메시지 번들을 찾을 수 없습니다: $resourcePath")
        return stream.use { input ->
            java.util
                .Properties()
                .apply { load(input.reader(Charsets.UTF_8)) }
                .stringPropertyNames()
                .toSet()
        }
    }
}
