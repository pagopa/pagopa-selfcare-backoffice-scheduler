package it.pagopa.selfcare.backoffice.scheduler.documents

import java.time.Instant
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

enum class RequestStatus {
    PENDING,
    CANCELED,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
}

@Document(collection = "ibanDeletionRequests")
data class IbanDeletionRequest(
    @Id val id: String? = null,
    val userId: String,
    val iban: String,
    val requestedAt: Instant = Instant.now(),
    val scheduledDeletionDate: Instant,
    var status: RequestStatus = RequestStatus.PENDING,
    var cancellationRequested: Boolean = false,
    var cancellationRequestedAt: Instant? = null,
    var processedAt: Instant? = null,
    var deletedBySchedulerAt: Instant? = null,
)
