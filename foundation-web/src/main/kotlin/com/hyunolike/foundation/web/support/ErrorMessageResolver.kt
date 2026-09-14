package com.hyunolike.foundation.web.support

import com.hyunolike.foundation.core.error.ErrorCode
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder

/**
 * 에러 코드의 messageKey 를 사용자에게 보일 문장으로 바꾼다.
 *
 * 번들에 키가 없으면 코드 문자열을 그대로 돌려준다 — 문서에 없는 문구를 지어내는 것보다
 * 코드가 그대로 보이는 편이 디버깅에 낫다.
 */
class ErrorMessageResolver(
    private val messageSource: MessageSource,
) {
    fun resolve(
        errorCode: ErrorCode,
        arguments: List<Any> = emptyList(),
    ): String = resolve(errorCode, arguments, LocaleContextHolder.getLocale())

    fun resolve(
        errorCode: ErrorCode,
        arguments: List<Any>,
        locale: java.util.Locale,
    ): String =
        messageSource.getMessage(
            errorCode.messageKey,
            arguments.toTypedArray(),
            errorCode.code,
            locale,
        ) ?: errorCode.code
}
