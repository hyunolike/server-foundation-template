plugins {
    `java-library`
}

// 테스트에서만 쓰이는 모듈이지만, 소비 쪽에서 testImplementation(project(":foundation-test")) 으로
// 붙이므로 일반 소스셋에 둔다. (설계 문서 §4)
dependencies {
    api(project(":foundation-core"))
    api("org.springframework.boot:spring-boot-starter-test")
    api("org.springframework.restdocs:spring-restdocs-mockmvc")
    api(libs.swagger.request.validator.mockmvc)
}
