package it.pagopa.selfcare.backoffice.scheduler.scheduledjob

import it.pagopa.selfcare.backoffice.scheduler.documents.TaskStatus
import it.pagopa.selfcare.backoffice.scheduler.repositories.ScheduledTaskRepository
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
class ProcessingRequestScheduler(private val repository: ScheduledTaskRepository) {

    companion object {
        private val logger = LoggerFactory.getLogger(ProcessingRequestScheduler::class.java)
    }

    private val MAX_CONCURRENCY = 8

    @Scheduled(cron = "\${iban-deletion-request-scheduled.execution.cron}")
    fun executeScheduledJobs() {
        logger.info("Scheduler job started: Fetching PENDING tasks to execute.")

        repository
            .findAllByStatusAndScheduledExecutionDateBeforeAndCancellationRequestedIsFalse(
                status = TaskStatus.PENDING,
                now = Instant.now(),
            )
            .flatMap(
                { request ->
                    println("processing task ${request.id} of type ${request.type}")
                    Mono.just(request)
                },
                MAX_CONCURRENCY,
            )
            .subscribe(
                { completedRequest ->
                    println(
                        "Processing task completed: ${completedRequest.id} (${completedRequest.type})"
                    )
                },
                { error ->
                    System.err.println(
                        "Critical error during scheduler processing: ${error.message}"
                    )
                },
            )
    }
}
