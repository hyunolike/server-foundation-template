package com.hyunolike.sample

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 커밋된 `docs/openapi/openapi.json` 이 현재 코드와 같은지 확인한다. (설계 문서 §7.5)
 *
 * 스펙을 커밋하는 이유는 생성물이어도 리뷰 대상이기 때문이다 — PR diff 에 스펙 변경이
 * 보여야 호환성이 깨지는 변경을 사람이 알아챈다.
 *
 * 갱신: `./gradlew :sample-api:updateOpenApi`
 */
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiSnapshotTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    private val objectMapper =
        ObjectMapper().enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, SerializationFeature.INDENT_OUTPUT)

    @Test
    fun `커밋된 스펙이 현재 코드와 같다`() {
        val snapshotPath = System.getProperty(SNAPSHOT_PROPERTY)
        assertTrue(!snapshotPath.isNullOrBlank(), "$SNAPSHOT_PROPERTY 시스템 프로퍼티가 없습니다.")

        val generated =
            normalize(
                mockMvc
                    .perform(get("/v3/api-docs"))
                    .andReturn()
                    .response.contentAsString,
            )
        val snapshot = File(snapshotPath)

        if (System.getProperty(UPDATE_PROPERTY) == "true") {
            snapshot.parentFile.mkdirs()
            snapshot.writeText(generated)
            println("openapi.json 을 갱신했습니다: ${snapshot.absolutePath}")
            return
        }

        assertTrue(
            snapshot.isFile,
            "커밋된 스펙이 없습니다: ${snapshot.absolutePath}\n" +
                "./gradlew :sample-api:updateOpenApi 로 생성하세요.",
        )

        assertEquals(
            snapshot.readText(),
            generated,
            "생성된 스펙이 커밋된 openapi.json 과 다릅니다.\n" +
                "의도한 변경이라면 ./gradlew :sample-api:updateOpenApi 로 갱신하고 함께 커밋하세요.",
        )
    }

    private fun normalize(spec: String): String = objectMapper.writeValueAsString(objectMapper.readTree(spec)) + "\n"

    companion object {
        const val SNAPSHOT_PROPERTY = "openapi.snapshot"
        const val UPDATE_PROPERTY = "openapi.update"
    }
}
