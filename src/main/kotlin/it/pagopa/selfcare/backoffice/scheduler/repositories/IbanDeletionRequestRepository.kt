package it.pagopa.selfcare.backoffice.scheduler.repositories

import it.pagopa.selfcare.backoffice.scheduler.documents.IbanDeletionRequest
import it.pagopa.selfcare.backoffice.scheduler.documents.RequestStatus
import java.time.Instant
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Flux

interface IbanDeletionRequestRepository : ReactiveMongoRepository<IbanDeletionRequest, String> {

    fun findAllByStatusAndScheduledDeletionDateBeforeAndCancellationRequestedIsFalse(
        status: RequestStatus,
        now: Instant,
    ): Flux<IbanDeletionRequest>
}
