package it.pagopa.selfcare.backoffice.scheduler.services

import it.pagopa.selfcare.backoffice.scheduler.clients.ApiConfigClient
import it.pagopa.selfcare.backoffice.scheduler.documents.ScheduledTask
import it.pagopa.selfcare.backoffice.scheduler.documents.TaskStatus
import it.pagopa.selfcare.backoffice.scheduler.repositories.ScheduledTaskRepository
import java.time.Instant
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

/**
 * Service responsible for processing IBAN deletion tasks. Handles the complete lifecycle of IBAN
 * deletion including API calls and status updates.
 */
@Service
class IbanDeletionService(
    private val apiConfigClient: ApiConfigClient,
    private val repository: ScheduledTaskRepository,
) {

    companion object {
        private val logger = LoggerFactory.getLogger(IbanDeletionService::class.java)

        // Keys for data fields in the task's 'data' map
        private const val CREDITOR_INSTITUTION_CODE_KEY = "creditorInstitutionCode"
        private const val IBAN_VALUE_KEY = "ibanValue"
    }

    /**
     * Processes a single IBAN deletion task.
     *
     * @param task The scheduled task to process
     * @return A Mono emitting the updated task after processing
     */
    fun processTask(task: ScheduledTask): Mono<ScheduledTask> {
        logger.info("Starting IBAN deletion task processing: taskId=${task.id}")

        if (TaskStatus.CANCELED.equals(task.status)) {
            logger.info("Task ${task.id} cancellation requested, skipping execution")
            return markTaskAsCanceled(task)
        }

        return markTaskAsInProgress(task)
            .flatMap { updatedTask -> deleteIbanViaApiConfig(updatedTask) }
            .flatMap { successTask -> markTaskAsCompleted(successTask) }
            .doOnSuccess { completedTask ->
                logger.info("IBAN deletion completed successfully: taskId=${completedTask.id}")
            }
            .onErrorResume { error ->
                logger.error("Error processing IBAN deletion for task ${task.id}", error)
                markTaskAsFailed(task, error)
            }
    }

    /** Updates task status to IN_PROGRESS and saves it to the repository. */
    private fun markTaskAsInProgress(task: ScheduledTask): Mono<ScheduledTask> {
        logger.debug("Marking task ${task.id} as IN_PROGRESS")

        task.status = TaskStatus.IN_PROGRESS
        task.processedAt = Instant.now().toString()

        return repository.save(task)
    }

    /** Calls ApiConfig to delete the IBAN associated with the task. */
    private fun deleteIbanViaApiConfig(task: ScheduledTask): Mono<ScheduledTask> {
        val creditorInstitutionCode = extractRequiredData(task, CREDITOR_INSTITUTION_CODE_KEY)
        val ibanValue = extractRequiredData(task, IBAN_VALUE_KEY)

        logger.info(
            "Deleting IBAN via ApiConfig: taskId=${task.id}, " +
                "creditorInstitutionCode=$creditorInstitutionCode, ibanValue=****"
        )

        return apiConfigClient
            .deleteCreditorInstitutionsIban(
                creditorInstitutionCode = creditorInstitutionCode,
                ibanValue = ibanValue,
            )
            .doOnSuccess { result ->
                logger.debug("ApiConfig deletion successful: taskId=${task.id}, result=$result")
            }
            .map { task }
    }

    /** Updates task status to COMPLETED and saves it to the repository. */
    private fun markTaskAsCompleted(task: ScheduledTask): Mono<ScheduledTask> {
        logger.debug("Marking task ${task.id} as COMPLETED")

        task.status = TaskStatus.COMPLETED
        task.completedAt = Instant.now().toString()

        return repository.save(task)
    }

    /** Updates task status to CANCELED and saves it to the repository. */
    private fun markTaskAsCanceled(task: ScheduledTask): Mono<ScheduledTask> {
        logger.debug("Marking task ${task.id} as CANCELED")

        task.status = TaskStatus.CANCELED
        task.completedAt = Instant.now().toString()

        return repository.save(task)
    }

    /** Updates task status to FAILED and saves it to the repository. */
    private fun markTaskAsFailed(task: ScheduledTask, error: Throwable): Mono<ScheduledTask> {
        logger.debug("Marking task ${task.id} as FAILED")

        task.status = TaskStatus.FAILED
        task.completedAt = Instant.now().toString()

        return repository.save(task)
    }

    /**
     * Extracts a required data field from the task's data map.
     *
     * @param task The scheduled task
     * @param key The key of the data field to extract
     * @return The extracted value as a String
     * @throws IllegalArgumentException if the required field is missing
     */
    private fun extractRequiredData(task: ScheduledTask, key: String): String {
        return task.data[key]?.toString()
            ?: throw IllegalArgumentException(
                "Missing required data field '$key' in task ${task.id}"
            )
    }
}
