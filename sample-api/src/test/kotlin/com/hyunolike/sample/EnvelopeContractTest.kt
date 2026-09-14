package com.hyunolike.sample

import com.hyunolike.foundation.test.ApiResponseAssertions.hasFieldError
import com.hyunolike.foundation.test.ApiResponseAssertions.isEnvelope
import com.hyunolike.foundation.test.ApiResponseAssertions.isFailure
import com.hyunolike.foundation.test.ApiResponseAssertions.isSuccess
import com.hyunolike.sampletest.ExplodingController
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * M2 완료 기준: 성공 · 검증 실패 · 미처리 예외 · 매핑 없는 URL 네 응답이
 * 모두 같은 봉투로 나온다. (설계 문서 §9)
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(ExplodingController::class)
class EnvelopeContractTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `성공 응답은 봉투로 나간다`() {
        mockMvc
            .perform(get("/api/v1/users/1001"))
            .andExpect(status().isOk)
            .andExpect(isSuccess())
            .andExpect(jsonPath("$.data.email").value("hyunho@example.com"))
    }

    @Test
    fun `도메인 실패는 에러 코드의 상태를 그대로 쓴다`() {
        mockMvc
            .perform(get("/api/v1/users/999999"))
            .andExpect(status().isNotFound)
            .andExpect(isFailure("USER_NOT_FOUND"))
            .andExpect(jsonPath("$.error.message").value("사용자를 찾을 수 없습니다."))
    }

    @Test
    fun `검증 실패는 필드별 사유를 담는다`() {
        mockMvc
            .perform(
                post("/api/v1/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"email":"not-an-email","password":"short"}"""),
            ).andExpect(status().isBadRequest)
            .andExpect(isFailure("COMMON_INVALID_PARAMETER"))
            .andExpect(hasFieldError("email"))
            .andExpect(hasFieldError("password"))
    }

    @Test
    fun `미처리 예외는 내부 메시지를 흘리지 않는다`() {
        val body =
            mockMvc
                .perform(get("/api/v1/boom"))
                .andExpect(status().isInternalServerError)
                .andExpect(isFailure("COMMON_INTERNAL_ERROR"))
                .andReturn()
                .response
                .contentAsString

        check(!body.contains(ExplodingController.SECRET)) { "내부 메시지가 응답에 노출됐습니다: $body" }
    }

    @Test
    fun `매핑 없는 URL 도 봉투로 나간다`() {
        mockMvc
            .perform(get("/api/v1/definitely-not-here"))
            .andExpect(status().isNotFound)
            .andExpect(isEnvelope())
            .andExpect(jsonPath("$.success").value(false))
    }

    @Test
    fun `지원하지 않는 메서드도 봉투로 나간다`() {
        mockMvc
            .perform(patch("/api/v1/users/1001"))
            .andExpect(status().isMethodNotAllowed)
            .andExpect(isFailure("COMMON_METHOD_NOT_ALLOWED"))
    }

    /** 봉투 규칙의 유일한 예외를 문서와 같은 모양으로 고정한다. (설계 문서 §6.5) */
    @Test
    fun `본문 없는 성공은 204 로 나가고 봉투를 쓰지 않는다`() {
        mockMvc
            .perform(
                post(
                    "/api/v1/users",
                ).contentType(MediaType.APPLICATION_JSON).content("""{"email":"gone@example.com","password":"correct-horse"}"""),
            ).andExpect(status().isCreated)

        val created =
            mockMvc
                .perform(get("/api/v1/users?size=200"))
                .andReturn()
                .response
                .contentAsString

        val userId = Regex(""""userId":(\d+),"email":"gone@example\.com"""").find(created)!!.groupValues[1]

        val response =
            mockMvc
                .perform(delete("/api/v1/users/$userId"))
                .andExpect(status().isNoContent)
                .andReturn()
                .response

        check(response.contentAsString.isEmpty()) { "204 응답에 본문이 있습니다: ${response.contentAsString}" }
    }

    @Test
    fun `traceId 는 응답 헤더와 meta 에서 같은 값이다`() {
        val result =
            mockMvc
                .perform(get("/api/v1/users/1001"))
                .andExpect(header().exists("X-Trace-Id"))
                .andReturn()

        val headerValue = result.response.getHeader("X-Trace-Id")!!
        mockMvc
            .perform(get("/api/v1/users/1001").header("traceparent", "00-$headerValue-0000000000000001-01"))
            .andExpect(jsonPath("$.meta.traceId").value(headerValue))
    }

    @Test
    fun `페이징 파라미터가 잘못되면 검증 실패와 같은 모양이다`() {
        mockMvc
            .perform(get("/api/v1/users?size=0"))
            .andExpect(status().isBadRequest)
            .andExpect(isFailure("COMMON_INVALID_PARAMETER"))
            .andExpect(hasFieldError("size"))
    }

    private fun get(path: String) =
        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
            .get(path)

    private fun post(path: String) =
        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
            .post(path)

    private fun patch(path: String) =
        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
            .patch(path)

    private fun delete(path: String) =
        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
            .delete(path)
}
