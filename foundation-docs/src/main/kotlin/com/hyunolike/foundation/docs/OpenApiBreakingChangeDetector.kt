package com.hyunolike.foundation.docs

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File
import kotlin.system.exitProcess

/**
 * 두 OpenAPI 문서를 비교해 클라이언트를 깨뜨리는 변경을 찾는다. (설계 문서 §7.5)
 *
 * 전체 diff 가 목적이 아니다 — 추가는 안전하고 제거는 위험하다는 한 가지 원칙만 본다.
 */
object OpenApiBreakingChangeDetector {
    private val objectMapper = ObjectMapper()

    fun detect(
        baseSpec: String,
        headSpec: String,
    ): List<String> {
        val base = objectMapper.readTree(baseSpec)
        val head = objectMapper.readTree(headSpec)
        val findings = mutableListOf<String>()

        findings += removedPathsAndOperations(base, head)
        findings += removedResponses(base, head)
        findings += removedErrorCodes(base, head)
        findings += schemaRegressions(base, head)

        return findings
    }

    private fun removedPathsAndOperations(
        base: JsonNode,
        head: JsonNode,
    ): List<String> {
        val findings = mutableListOf<String>()
        val basePaths = base.path("paths")
        val headPaths = head.path("paths")

        basePaths.fieldNames().forEach { path ->
            val headPath = headPaths.path(path)
            if (headPath.isMissingNode) {
                findings += "경로가 사라졌습니다: $path"
                return@forEach
            }
            basePaths.path(path).fieldNames().forEach { method ->
                if (headPath.path(method).isMissingNode) {
                    findings += "오퍼레이션이 사라졌습니다: ${method.uppercase()} $path"
                }
            }
        }
        return findings
    }

    private fun removedResponses(
        base: JsonNode,
        head: JsonNode,
    ): List<String> {
        val findings = mutableListOf<String>()
        forEachOperation(base) { path, method, operation ->
            val headResponses =
                head
                    .path("paths")
                    .path(path)
                    .path(method)
                    .path("responses")
            operation.path("responses").fieldNames().forEach { status ->
                if (headResponses.path(status).isMissingNode) {
                    findings += "응답 상태가 사라졌습니다: ${method.uppercase()} $path -> $status"
                }
            }
        }
        return findings
    }

    private fun removedErrorCodes(
        base: JsonNode,
        head: JsonNode,
    ): List<String> {
        val findings = mutableListOf<String>()
        forEachOperation(base) { path, method, operation ->
            val headOperation = head.path("paths").path(path).path(method)
            operation.path("responses").fields().forEach { (status, response) ->
                val headDescription =
                    headOperation
                        .path("responses")
                        .path(status)
                        .path("description")
                        .asText("")
                val headCodes = errorCodesIn(headDescription)
                errorCodesIn(response.path("description").asText("")).forEach { code ->
                    if (code !in headCodes) {
                        findings += "에러 코드가 사라졌습니다: ${method.uppercase()} $path $status -> $code"
                    }
                }
            }
        }
        return findings
    }

    /** 응답 설명은 `CODE — 문구` 형태로 생성된다. (ErrorResponseCustomizer) */
    private fun errorCodesIn(description: String): Set<String> = ERROR_CODE_PATTERN.findAll(description).map { it.value }.toSet()

    private fun schemaRegressions(
        base: JsonNode,
        head: JsonNode,
    ): List<String> {
        val findings = mutableListOf<String>()
        val baseSchemas = base.path("components").path("schemas")
        val headSchemas = head.path("components").path("schemas")

        baseSchemas.fieldNames().forEach { name ->
            val headSchema = headSchemas.path(name)
            if (headSchema.isMissingNode) {
                findings += "스키마가 사라졌습니다: $name"
                return@forEach
            }

            val baseProperties = baseSchemas.path(name).path("properties")
            baseProperties.fieldNames().forEach { property ->
                if (headSchema.path("properties").path(property).isMissingNode) {
                    findings += "필드가 사라졌습니다: $name.$property"
                } else {
                    val baseType = baseProperties.path(property).path("type").asText("")
                    val headType =
                        headSchema
                            .path("properties")
                            .path(property)
                            .path("type")
                            .asText("")
                    if (baseType.isNotEmpty() && headType.isNotEmpty() && baseType != headType) {
                        findings += "필드 타입이 바뀌었습니다: $name.$property ($baseType -> $headType)"
                    }
                }
            }

            val baseRequired =
                baseSchemas
                    .path(name)
                    .path("required")
                    .map { it.asText() }
                    .toSet()
            headSchema.path("required").map { it.asText() }.forEach { required ->
                if (required !in baseRequired) {
                    findings += "필드가 필수로 바뀌었습니다: $name.$required"
                }
            }
        }
        return findings
    }

    private fun forEachOperation(
        spec: JsonNode,
        action: (path: String, method: String, operation: JsonNode) -> Unit,
    ) {
        val paths = spec.path("paths")
        paths.fieldNames().forEach { path ->
            val pathItem = paths.path(path)
            pathItem.fieldNames().forEach { method ->
                if (method in HTTP_METHODS) action(path, method, pathItem.path(method))
            }
        }
    }

    private val ERROR_CODE_PATTERN = Regex("[A-Z][A-Z0-9]*(?:_[A-Z0-9]+)+")
    private val HTTP_METHODS = setOf("get", "put", "post", "delete", "patch", "head", "options", "trace")
}

/**
 * `./gradlew :foundation-docs:diffOpenApi -PbaseSpec=... -PheadSpec=...` 로 실행된다.
 * CI 는 기준 브랜치의 스펙을 먼저 꺼내 두면 된다:
 * `git show origin/main:docs/openapi/openapi.json > build/base-openapi.json`
 */
fun main(args: Array<String>) {
    if (args.size != 2) {
        System.err.println("사용법: OpenApiBreakingChangeDetector <base-spec.json> <head-spec.json>")
        exitProcess(2)
    }

    val findings = OpenApiBreakingChangeDetector.detect(File(args[0]).readText(), File(args[1]).readText())

    if (findings.isEmpty()) {
        println("호환성이 깨지는 변경 없음.")
        return
    }

    System.err.println("호환성이 깨지는 변경 ${findings.size}건:")
    findings.forEach { System.err.println("  - $it") }
    exitProcess(1)
}
