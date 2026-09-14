package com.hyunolike.foundation.observability.logging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MaskingRulesTest {
    private val rules = MaskingRules()

    @Test
    fun `본문의 비밀번호는 마스킹된다`() {
        val masked = rules.maskBody("""{"email":"a@b.com","password":"hunter2"}""")

        assertTrue(masked.contains(""""password": "***""""), masked)
        assertTrue(masked.contains("a@b.com"), masked)
    }

    @Test
    fun `토큰은 문자열이 아니어도 마스킹된다`() {
        val masked = rules.maskBody("""{"accessToken":null,"cardNumber":1234567812345678}""")

        assertTrue(masked.contains(""""accessToken": "***""""), masked)
        assertTrue(!masked.contains("1234567812345678"), masked)
    }

    @Test
    fun `민감 헤더만 가린다`() {
        assertEquals(MaskingRules.MASK, rules.maskHeader("Authorization", "Bearer abc"))
        assertEquals("application/json", rules.maskHeader("Content-Type", "application/json"))
    }
}
