plugins {
    `java-library`
    alias(libs.plugins.kotlin.spring)
}

dependencies {
    api(project(":foundation-core"))
    api(project(":foundation-observability"))
    api("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    testImplementation(project(":foundation-test"))
}
