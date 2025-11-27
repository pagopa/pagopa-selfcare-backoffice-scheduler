package it.pagopa.selfcare.backoffice.scheduler.scheduledjob

import it.pagopa.selfcare.backoffice.scheduler.documents.TaskStatus
import it.pagopa.selfcare.backoffice.scheduler.documents.TaskType
import it.pagopa.selfcare.backoffice.scheduler.repositories.ScheduledTaskRepository
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
    private val repository: ScheduledTaskRepository,
    private val ibanDeletionService: IbanDeletionService,
) {

    companion object {
        private val logger = LoggerFactory.getLogger(ProcessingRequestScheduler::class.java)
        private const val MAX_CONCURRENCY = 8
    }

    @Scheduled(cron = "\${iban-deletion-request-scheduled.execution.cron}")
    fun executeScheduledIbanDeletionJobs() {
        logger.info(
            "Scheduler Iban deletion job started: Fetching ${TaskStatus.PENDING} tasks to execute related type ${TaskType.IBAN_DELETION}"
        )

        repository
            .findExecutableTasks(
                status = TaskStatus.PENDING.toString(),
                scheduledExecutionDate = Instant.now().toString(),
            )
            .flatMap(
                { task ->
                    logger.info("Processing task ${task.id} of type ${task.type}")
                    // Dispatch to the appropriate service based on task type
                    when (task.type) {
                        TaskType.IBAN_DELETION ->
                            ibanDeletionService.processTask(task).onErrorResume { error ->
                                logger.error("Error processing task ${task.id}: ${error.message}")
                                Mono.empty()
                            }
                    }
                },
                MAX_CONCURRENCY,
            )
            .subscribe(
                { completedTask ->
                    logger.info(
                        "Processing task completed: ${completedTask.id} (${completedTask.type})"
                    )
                },
                { error ->
                    logger.error(
                        "Critical error during scheduler processing: ${error.message}",
                        error,
                    )
                },
            )
    }
}
