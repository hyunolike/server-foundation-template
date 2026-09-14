package com.hyunolike.sample

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.hyunolike.foundation.core.error.ErrorCodeCatalog
import com.hyunolike.foundation.docs.ApiErrorCodes
import com.hyunolike.foundation.test.OpenApiResponseValidator
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * M3·M4 완료 기준. (설계 문서 §9)
 *
 * 첫째, 컨트롤러가 쓰지 않은 봉투가 문서에는 들어가 있어야 한다.
 * 둘째, 실제 응답이 그 문서를 만족해야 한다 — 이 둘은 다른 검사다. (설계 문서 §7.4)
 */
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiContractTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var errorCodeCatalog: ErrorCodeCatalog

    @Autowired
    private lateinit var handlerMapping: RequestMappingHandlerMapping

    private val objectMapper = ObjectMapper()

    private fun spec(): JsonNode =
        objectMapper.readTree(
            mockMvc
                .perform(get("/v3/api-docs"))
                .andReturn()
                .response.contentAsString,
        )

    @Test
    fun `문서 경로는 스펙에서 감춘다`() {
        assertTrue(spec().path("paths").path("/error").isMissingNode, "/error 가 문서에 실렸습니다")
    }

    @Test
    fun `문서 경로 자체는 봉투에 싸이지 않는다`() {
        val raw =
            mockMvc
                .perform(get("/v3/api-docs"))
                .andReturn()
                .response.contentAsString

        assertTrue(raw.startsWith("{\"openapi\""), raw.take(80))
    }

    @Test
    fun `200 응답 스키마는 봉투다`() {
        val schema =
            spec()
                .path("paths")
                .path("/api/v1/users/{userId}")
                .path("get")
                .path("responses")
                .path("200")
                .path("content")
                .path("application/json")
                .path("schema")

        val properties = schema.path("properties")
        assertTrue(properties.has("success"), "봉투의 success 가 없습니다: $schema")
        assertTrue(properties.has("data"), "봉투의 data 가 없습니다")
        assertTrue(properties.has("error"), "봉투의 error 가 없습니다")
        assertTrue(properties.has("meta"), "봉투의 meta 가 없습니다")
        assertEquals(
            "#/components/schemas/UserResponse",
            properties.path("data").path("\$ref").asText(),
            "data 안에 원래 스키마가 그대로 있어야 합니다",
        )
    }

    @Test
    fun `공통 응답 헤더가 문서에 있다`() {
        val header =
            spec()
                .path("paths")
                .path("/api/v1/users/{userId}")
                .path("get")
                .path("responses")
                .path("200")
                .path("headers")
                .path("X-Trace-Id")

        assertTrue(!header.isMissingNode, "X-Trace-Id 헤더가 문서에 없습니다")
    }

    @Test
    fun `선언한 에러 코드가 상태별 응답으로 들어간다`() {
        val response =
            spec()
                .path("paths")
                .path("/api/v1/users/{userId}")
                .path("get")
                .path("responses")
                .path("404")

        assertTrue(!response.isMissingNode, "404 응답이 문서에 없습니다")
        assertTrue(
            response.path("description").asText().contains("USER_NOT_FOUND"),
            "404 설명에 에러 코드가 없습니다: ${response.path("description").asText()}",
        )
        assertEquals(
            "#/components/schemas/ApiResponseVoid",
            response
                .path("content")
                .path("application/json")
                .path("schema")
                .path("\$ref")
                .asText(),
        )
    }

    @Test
    fun `전역 기본 응답이 모든 엔드포인트에 붙는다`() {
        val responses =
            spec()
                .path("paths")
                .path("/api/v1/users")
                .path("get")
                .path("responses")

        listOf("400", "401", "403", "500").forEach { status ->
            assertTrue(!responses.path(status).isMissingNode, "$status 기본 응답이 없습니다")
        }
    }

    @Test
    fun `ApiErrorCodes 에 적은 코드는 모두 레지스트리에 있다`() {
        val unknown =
            handlerMapping.handlerMethods.values
                .flatMap { handler ->
                    val onMethod =
                        AnnotatedElementUtils
                            .findMergedAnnotation(handler.method, ApiErrorCodes::class.java)
                            ?.value
                            ?.toList()
                            .orEmpty()
                    val onType =
                        AnnotatedElementUtils
                            .findMergedAnnotation(handler.beanType, ApiErrorCodes::class.java)
                            ?.value
                            ?.toList()
                            .orEmpty()
                    onMethod + onType
                }.distinct()
                .filter { errorCodeCatalog.find(it) == null }

        assertTrue(unknown.isEmpty(), "ErrorCodeCatalog 에 없는 코드를 문서에 선언했습니다: $unknown")
    }

    @Test
    fun `실제 응답이 생성된 스펙을 만족한다`() {
        val validator = OpenApiResponseValidator.fromSpec(spec().toString())

        mockMvc.perform(get("/api/v1/users/1001")).andExpect(validator.matcher())
        mockMvc.perform(get("/api/v1/users?page=0&size=20")).andExpect(validator.matcher())
    }

    @Test
    fun `스펙에 봉투 공통 스키마가 등록돼 있다`() {
        val schemas = spec().path("components").path("schemas")

        listOf("ApiError", "ApiErrorDetail", "ResponseMeta", "ApiResponseVoid").forEach {
            assertNotNull(schemas.get(it), "$it 스키마가 없습니다")
        }
    }
}
