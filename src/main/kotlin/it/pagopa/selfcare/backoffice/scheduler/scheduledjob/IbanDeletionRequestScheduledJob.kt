package it.pagopa.selfcare.backoffice.scheduler.scheduledjob

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class IbanDeletionRequestScheduledJob {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "\${iban-deletion-request-scheduled.execution.cron}")
    fun IbanDeletionRequestProcessor() {
        logger.info("RUN IbanDeletionRequestProcessor()")
    }
}
