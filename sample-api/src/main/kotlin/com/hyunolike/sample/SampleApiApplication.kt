package com.hyunolike.sample

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * 파운데이션을 쓰는 얇은 참조 구현.
 *
 * 여기에는 `@Configuration` 이 하나도 없다 — 표준은 의존성만 추가하면 켜진다.
 */
@SpringBootApplication
class SampleApiApplication

fun main(args: Array<String>) {
    runApplication<SampleApiApplication>(*args)
}
