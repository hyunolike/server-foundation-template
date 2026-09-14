package com.hyunolike.foundation.core.error

import com.hyunolike.foundation.test.ErrorCodeContract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CommonErrorCodeContractTest {
    private companion object {
        const val MESSAGE_BUNDLE = "foundation-messages.properties"
    }

    // 한글 함수 이름 안에서 익명 객체를 만들지 않는다 — 생성되는 클래스 파일 이름에
    // 한글이 섞여, UTF-8 이 아닌 로케일에서 컴파일이 깨진다.
    private object BrokenErrorCode : ErrorCode {
        override val code = "brokenCode"
        override val status = 200
        override val messageKey = ""
    }

    private object MissingMessageErrorCode : ErrorCode {
        override val code = "COMMON_NO_SUCH_MESSAGE"
        override val status = 400
        override val messageKey = "error.common.doesNotExist"
    }

    @Test
    fun `공통 에러 코드는 계약을 만족한다`() {
        ErrorCodeContract.verify(
            codes = CommonErrorCode.entries,
            knownMessageKeys = ErrorCodeContract.messageKeysFrom(MESSAGE_BUNDLE),
        )
    }

    @Test
    fun `메시지 번들에 없는 키는 검사기가 잡는다`() {
        val failure =
            assertFailsWith<IllegalStateException> {
                ErrorCodeContract.verify(
                    codes = listOf(MissingMessageErrorCode),
                    knownMessageKeys = ErrorCodeContract.messageKeysFrom(MESSAGE_BUNDLE),
                )
            }

        assertEquals(true, failure.message!!.contains("메시지 번들에 없습니다"), failure.message)
    }

    @Test
    fun `enum 이름과 code 는 같다`() {
        CommonErrorCode.entries.forEach { assertEquals(it.name, it.code) }
    }

    @Test
    fun `계약 위반은 검사기가 잡는다`() {
        val failure =
            assertFailsWith<IllegalStateException> { ErrorCodeContract.verify(listOf(BrokenErrorCode)) }

        assertEquals(true, failure.message!!.contains("3건"), failure.message)
    }
}
