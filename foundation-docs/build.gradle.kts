plugins {
    `java-library`
    alias(libs.plugins.kotlin.spring)
}

dependencies {
    api(project(":foundation-web"))
    api(libs.springdoc.webmvc.ui)

    testImplementation(project(":foundation-test"))
}

// ---- 호환성 검사 (설계 문서 §7.5) ----
//
// CI 는 기준 스펙을 먼저 꺼내 둔다:
//   git show origin/main:docs/openapi/openapi.json > build/base-openapi.json
//   ./gradlew :foundation-docs:diffOpenApi -PbaseSpec=build/base-openapi.json

val diffOpenApi by tasks.registering(JavaExec::class) {
    group = "verification"
    description = "기준 스펙과 비교해 호환성이 깨지는 변경을 찾는다."
    mainClass.set("com.hyunolike.foundation.docs.OpenApiBreakingChangeDetectorKt")
    classpath = sourceSets["main"].runtimeClasspath

    val baseSpec = providers.gradleProperty("baseSpec")
    val headSpec =
        providers
            .gradleProperty("headSpec")
            .orElse(
                rootProject.layout.projectDirectory
                    .file("docs/openapi/openapi.json")
                    .asFile.absolutePath,
            )

    doFirst {
        check(baseSpec.isPresent) {
            "기준 스펙 경로가 필요합니다: -PbaseSpec=<path>"
        }
    }
    argumentProviders.add(
        CommandLineArgumentProvider {
            if (baseSpec.isPresent) listOf(baseSpec.get(), headSpec.get()) else emptyList()
        },
    )
}
