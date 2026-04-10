package com.qualidoc

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class QualiDocApplication

fun main(args: Array<String>) {
    runApplication<QualiDocApplication>(*args)
}
