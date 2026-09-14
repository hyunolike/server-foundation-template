plugins {
    `java-library`
}

dependencies {
    api(project(":foundation-core"))
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("org.slf4j:slf4j-api")
}
