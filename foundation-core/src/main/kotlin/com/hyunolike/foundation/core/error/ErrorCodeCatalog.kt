package com.hyunolike.foundation.core.error

/**
 * 애플리케이션이 쓰는 모든 [ErrorCode] 를 한 자리에 모은 목록.
 *
 * 문서 생성기는 `@ApiErrorCodes("USER_NOT_FOUND")` 의 문자열을 여기서 찾아
 * 상태 코드와 메시지를 얻는다. 이름으로 찾지 못하면 문서가 아니라 빌드가 깨져야 한다.
 */
class ErrorCodeCatalog(
    codes: List<ErrorCode>,
) {
    private val byCode: Map<String, ErrorCode> = codes.associateBy { it.code }

    val all: List<ErrorCode> = codes.toList()

    fun find(code: String): ErrorCode? = byCode[code]

    fun require(code: String): ErrorCode =
        byCode[code]
            ?: error("등록되지 않은 에러 코드입니다: $code. ErrorCodeCatalog 빈에 추가하세요.")
}
