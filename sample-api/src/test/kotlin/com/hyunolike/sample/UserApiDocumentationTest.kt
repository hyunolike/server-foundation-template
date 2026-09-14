package com.hyunolike.sample

import com.hyunolike.foundation.test.RestDocsSupport
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.io.File
import kotlin.test.assertTrue

/**
 * 검증의 첫 번째 겹. (설계 문서 §7.4)
 *
 * 여기서 적은 필드 설명이 실제 응답과 하나라도 어긋나면 테스트가 깨진다 — 응답에 없는
 * 필드를 문서에 적어도, 응답에 있는 필드를 빠뜨려도 마찬가지다. 그래서 스니펫이 나왔다는
 * 사실 자체가 "문서의 예시가 실재한다"는 보증이 된다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureRestDocs
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class UserApiDocumentationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    @Order(1)
    fun `사용자 단건 조회를 문서화한다`() {
        mockMvc
            .perform(get("/api/v1/users/1001").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andDo(
                RestDocsSupport.documentWith(
                    "user-get",
                    RestDocsSupport.successResponseFields(
                        fieldWithPath("data.userId").type(JsonFieldType.NUMBER).description("사용자 식별자"),
                        fieldWithPath("data.email").type(JsonFieldType.STRING).description("이메일"),
                        fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("가입 시각 (UTC)"),
                    ),
                ),
            )
    }

    @Test
    @Order(2)
    fun `목록 조회를 문서화한다`() {
        mockMvc
            .perform(get("/api/v1/users?page=0&size=20").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andDo(
                RestDocsSupport.documentWith(
                    "user-list",
                    RestDocsSupport.successResponseFields(
                        fieldWithPath("data.content[]").type(JsonFieldType.ARRAY).description("사용자 목록"),
                        fieldWithPath("data.content[].userId").type(JsonFieldType.NUMBER).description("사용자 식별자"),
                        fieldWithPath("data.content[].email").type(JsonFieldType.STRING).description("이메일"),
                        fieldWithPath("data.content[].createdAt").type(JsonFieldType.STRING).description("가입 시각 (UTC)"),
                        fieldWithPath("data.page.number").type(JsonFieldType.NUMBER).description("0부터 시작하는 페이지 번호"),
                        fieldWithPath("data.page.size").type(JsonFieldType.NUMBER).description("페이지 크기"),
                        fieldWithPath("data.page.totalElements").type(JsonFieldType.NUMBER).description("전체 건수"),
                        fieldWithPath("data.page.totalPages").type(JsonFieldType.NUMBER).description("전체 페이지 수"),
                        fieldWithPath("data.page.sort").type(JsonFieldType.STRING).optional().description("정렬 기준"),
                    ),
                ),
            )
    }

    @Test
    @Order(3)
    fun `실패 응답을 문서화한다`() {
        mockMvc
            .perform(get("/api/v1/users/999999").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound)
            .andDo(RestDocsSupport.documentWith("user-get-not-found", RestDocsSupport.errorResponseFields()))
    }

    @Test
    @Order(4)
    fun `스니펫이 실제로 생성됐다`() {
        val snippets = File("build/generated-snippets")

        listOf("user-get", "user-list", "user-get-not-found").forEach { identifier ->
            val directory = File(snippets, identifier)
            assertTrue(directory.isDirectory, "스니펫이 없습니다: ${directory.absolutePath}")
            assertTrue(
                File(directory, "response-fields.adoc").isFile,
                "필드 문서가 없습니다: $identifier/response-fields.adoc",
            )
        }
    }
}
