plugins {
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
}

dependencies {
    implementation(project(":foundation-docs"))
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    testImplementation(project(":foundation-test"))
}

// ---- OpenAPI 스냅샷 (설계 문서 §7.5) ----

val openApiSnapshot = rootProject.layout.projectDirectory.file("docs/openapi/openapi.json")

tasks.withType<Test>().configureEach {
    systemProperty("openapi.snapshot", openApiSnapshot.asFile.absolutePath)
}

fun Test.onlySnapshotTest() {
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    filter { includeTestsMatching("*OpenApiSnapshotTest*") }
    outputs.upToDateWhen { false }
}

val updateOpenApi by tasks.registering(Test::class) {
    group = "documentation"
    description = "생성된 OpenAPI 스펙을 docs/openapi/openapi.json 에 갱신한다."
    onlySnapshotTest()
    systemProperty("openapi.update", "true")
}

val verifyOpenApi by tasks.registering(Test::class) {
    group = "verification"
    description = "커밋된 openapi.json 이 현재 코드와 같은지 확인한다. 다르면 실패한다."
    onlySnapshotTest()
}
