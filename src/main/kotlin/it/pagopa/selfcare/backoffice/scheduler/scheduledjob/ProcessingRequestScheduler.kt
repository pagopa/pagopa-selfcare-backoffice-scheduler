package it.pagopa.selfcare.backoffice.scheduler.scheduledjob

import it.pagopa.selfcare.backoffice.scheduler.documents.IbanDeletionRequestStatus
import it.pagopa.selfcare.backoffice.scheduler.repositories.IbanDeletionRequestsRepository
import it.pagopa.selfcare.backoffice.scheduler.services.IbanDeletionService
import java.time.Instant
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

/**
 * Scheduler responsible for executing all programmed processing requests. It acts as a dispatcher
 * based on the request 'type'.
 */
@Service
class ProcessingRequestScheduler(
    private val repository: IbanDeletionRequestsRepository,
    private val ibanDeletionService: IbanDeletionService,
) {

    companion object {
        private val logger = LoggerFactory.getLogger(ProcessingRequestScheduler::class.java)
        private const val MAX_CONCURRENCY = 8
    }

    @Scheduled(cron = "\${iban-deletion-request-scheduled.execution.cron}")
    fun executeScheduledIbanDeletionJobs() {
        logger.info(
            "Scheduler Iban deletion job started: Fetching ${IbanDeletionRequestStatus.PENDING} requests"
        )

        repository
            .findExecutableTasks(
                status = IbanDeletionRequestStatus.PENDING.toString(),
                scheduledExecutionDate = Instant.now().toString(),
            )
            .flatMap(
                { request ->
                    logger.info("Processing task ${request.id} ")
                    ibanDeletionService.processTask(request).onErrorResume { error ->
                        logger.error("Error processing task ${request.id}: ${error.message}")
                        Mono.empty()
                    }
                },
                MAX_CONCURRENCY,
            )
            .subscribe(
                { completedTask -> logger.info("Processing task completed: ${completedTask.id}") },
                { error ->
                    logger.error(
                        "Critical error during scheduler processing: ${error.message}",
                        error,
                    )
                },
            )
    }
}
