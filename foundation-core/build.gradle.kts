// 계약 모듈. Spring 을 포함한 어떤 프레임워크에도 의존하지 않는다. (설계 문서 §4)
dependencies {
    testImplementation(project(":foundation-test"))
}

// M1 완료 기준: 의존성 그래프에 Spring 이 없다.
val verifyNoSpringDependency by tasks.registering {
    group = "verification"
    description = "foundation-core 의 컴파일 클래스패스에 Spring 이 섞이지 않았는지 확인한다."
    val classpath = configurations.named("compileClasspath")
    doLast {
        val offenders =
            classpath
                .get()
                .resolvedConfiguration
                .resolvedArtifacts
                .map { it.moduleVersion.id }
                .filter { it.group.startsWith("org.springframework") }
                .map { "${it.group}:${it.name}:${it.version}" }
                .distinct()
        check(offenders.isEmpty()) {
            "foundation-core 는 Spring 에 의존할 수 없다. 발견된 의존성: ${offenders.joinToString()}"
        }
    }
}

tasks.named("check") { dependsOn(verifyNoSpringDependency) }
