package com.hyunolike.foundation.docs

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OpenApiBreakingChangeDetectorTest {
    private val base =
        """
        {
          "paths": {
            "/api/v1/users/{userId}": {
              "get": {
                "responses": {
                  "200": { "description": "OK" },
                  "404": { "description": "USER_NOT_FOUND — 사용자를 찾을 수 없습니다." }
                }
              },
              "delete": { "responses": { "204": { "description": "No Content" } } }
            }
          },
          "components": {
            "schemas": {
              "UserResponse": {
                "properties": { "userId": { "type": "integer" }, "email": { "type": "string" } },
                "required": ["userId"]
              }
            }
          }
        }
        """.trimIndent()

    @Test
    fun `추가만 있으면 아무것도 보고하지 않는다`() {
        val head = base.replace(""""email": { "type": "string" }""", """"email": { "type": "string" }, "nickname": { "type": "string" }""")

        assertEquals(emptyList(), OpenApiBreakingChangeDetector.detect(base, head))
    }

    @Test
    fun `필드 제거와 타입 변경을 잡는다`() {
        val head = base.replace(""""email": { "type": "string" }""", """"phone": { "type": "integer" }""")

        val findings = OpenApiBreakingChangeDetector.detect(base, head)

        assertTrue(findings.any { it.contains("UserResponse.email") }, findings.toString())
    }

    @Test
    fun `오퍼레이션과 응답 제거를 잡는다`() {
        val head =
            base.replace(
                """"delete": { "responses": { "204": { "description": "No Content" } } }""",
                """"put": { "responses": {} }""",
            )

        val findings = OpenApiBreakingChangeDetector.detect(base, head)

        assertTrue(findings.any { it.contains("DELETE /api/v1/users/{userId}") }, findings.toString())
    }

    @Test
    fun `에러 코드가 사라지면 잡는다`() {
        val head = base.replace("USER_NOT_FOUND — 사용자를 찾을 수 없습니다.", "COMMON_NOT_FOUND — 없습니다.")

        val findings = OpenApiBreakingChangeDetector.detect(base, head)

        assertTrue(findings.any { it.contains("USER_NOT_FOUND") }, findings.toString())
    }

    @Test
    fun `필드가 필수로 바뀌면 잡는다`() {
        val head = base.replace(""""required": ["userId"]""", """"required": ["userId", "email"]""")

        val findings = OpenApiBreakingChangeDetector.detect(base, head)

        assertTrue(findings.any { it.contains("필수로 바뀌었습니다") }, findings.toString())
    }
}
