package com.template.webserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Our Spring Boot application.
 */
@SpringBootApplication
private open class CordaServices

/**
 * Starts our Spring Boot application.
 */
fun main(args: Array<String>) {
    runApplication<CordaServices>(*args)
}
