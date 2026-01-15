package it.pagopa.selfcare.backoffice.scheduler.services

import it.pagopa.selfcare.backoffice.scheduler.clients.ApiConfigClient
import it.pagopa.selfcare.backoffice.scheduler.documents.IbanDeletionRequest
import it.pagopa.selfcare.backoffice.scheduler.documents.IbanDeletionRequestStatus
import it.pagopa.selfcare.backoffice.scheduler.repositories.IbanDeletionRequestsRepository
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
    private val repository: IbanDeletionRequestsRepository,
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
    fun processTask(task: IbanDeletionRequest): Mono<IbanDeletionRequest> {
        logger.info("Starting IBAN deletion task processing: taskId=${task.id}")

        if (IbanDeletionRequestStatus.CANCELED.equals(task.status)) {
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
    private fun markTaskAsInProgress(request: IbanDeletionRequest): Mono<IbanDeletionRequest> {
        logger.debug("Marking task ${request.id} as IN_PROGRESS")

        request.status = IbanDeletionRequestStatus.IN_PROGRESS
        request.requestedAt = Instant.now().toString()

        return repository.save(request)
    }

    /** Calls ApiConfig to delete the IBAN associated with the task. */
    private fun deleteIbanViaApiConfig(request: IbanDeletionRequest): Mono<IbanDeletionRequest> {
        val creditorInstitutionCode = request.creditorInstitutionCode
        val ibanValue = request.ibanValue

        logger.info(
            "Deleting IBAN via ApiConfig: taskId=${request.id}, " +
                "creditorInstitutionCode=$creditorInstitutionCode, ibanValue=****"
        )

        return apiConfigClient
            .deleteCreditorInstitutionsIban(
                creditorInstitutionCode = creditorInstitutionCode,
                ibanValue = ibanValue,
            )
            .doOnSuccess { result ->
                logger.debug("ApiConfig deletion successful: taskId=${request.id}, result=$result")
            }
            .map { request }
    }

    /** Updates task status to COMPLETED and saves it to the repository. */
    private fun markTaskAsCompleted(task: IbanDeletionRequest): Mono<IbanDeletionRequest> {
        logger.debug("Marking task ${task.id} as COMPLETED")

        task.status = IbanDeletionRequestStatus.COMPLETED
        task.updatedAt = Instant.now().toString()

        return repository.save(task)
    }

    /** Updates task status to CANCELED and saves it to the repository. */
    private fun markTaskAsCanceled(task: IbanDeletionRequest): Mono<IbanDeletionRequest> {
        logger.debug("Marking task ${task.id} as CANCELED")

        task.status = IbanDeletionRequestStatus.CANCELED
        task.updatedAt = Instant.now().toString()

        return repository.save(task)
    }

    /** Updates task status to FAILED and saves it to the repository. */
    private fun markTaskAsFailed(
        task: IbanDeletionRequest,
        error: Throwable,
    ): Mono<IbanDeletionRequest> {
        logger.debug("Marking task ${task.id} as FAILED")

        task.status = IbanDeletionRequestStatus.FAILED
        task.updatedAt = Instant.now().toString()

        return repository.save(task)
    }
}
