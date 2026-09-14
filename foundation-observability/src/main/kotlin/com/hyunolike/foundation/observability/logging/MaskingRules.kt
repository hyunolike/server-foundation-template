package com.hyunolike.foundation.observability.logging

/**
 * 민감정보 마스킹 규칙의 단일 출처. (설계 문서 §4)
 *
 * 필터가 로그를 남기기 직전에만 쓰인다. 규칙이 여러 군데로 퍼지면 한 곳만 빠뜨려도
 * 평문이 로그에 남으므로, 추가는 항상 이 클래스에 한다.
 */
class MaskingRules(
    fieldNames: Collection<String> = DEFAULT_FIELD_NAMES,
    headerNames: Collection<String> = DEFAULT_HEADER_NAMES,
) {
    private val maskedFields: Set<String> = fieldNames.map { it.lowercase() }.toSet()
    private val maskedHeaders: Set<String> = headerNames.map { it.lowercase() }.toSet()

    private val jsonFieldPattern: Regex? =
        maskedFields.takeIf { it.isNotEmpty() }?.let { names ->
            Regex(
                """"(${names.joinToString("|") { Regex.escape(it) }})"\s*:\s*("(?:[^"\\]|\\.)*"|-?\d+(?:\.\d+)?|true|false|null)""",
                RegexOption.IGNORE_CASE,
            )
        }

    fun isMaskedHeader(name: String): Boolean = name.lowercase() in maskedHeaders

    fun maskHeader(
        name: String,
        value: String,
    ): String = if (isMaskedHeader(name)) MASK else value

    /** JSON 본문에서 마스킹 대상 필드의 값만 바꾼다. 파싱에 실패해도 원문을 흘리지 않는다. */
    fun maskBody(body: String): String {
        val pattern = jsonFieldPattern ?: return body
        return pattern.replace(body) { match -> """"${match.groupValues[1]}": "$MASK"""" }
    }

    companion object {
        const val MASK = "***"

        val DEFAULT_FIELD_NAMES =
            listOf(
                "password",
                "passwd",
                "secret",
                "token",
                "accessToken",
                "refreshToken",
                "authorization",
                "ssn",
                "residentRegistrationNumber",
                "cardNumber",
                "cvc",
                "accountNumber",
            )

        val DEFAULT_HEADER_NAMES =
            listOf(
                "authorization",
                "proxy-authorization",
                "cookie",
                "set-cookie",
                "x-api-key",
            )
    }
}
