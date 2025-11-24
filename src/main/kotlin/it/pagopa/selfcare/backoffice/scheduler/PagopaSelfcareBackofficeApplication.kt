package it.pagopa.selfcare.backoffice.scheduler

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication @EnableScheduling class PagopaSelfcareBackofficeApplication

fun main(args: Array<String>) {
    runApplication<PagopaSelfcareBackofficeApplication>(*args)
}
