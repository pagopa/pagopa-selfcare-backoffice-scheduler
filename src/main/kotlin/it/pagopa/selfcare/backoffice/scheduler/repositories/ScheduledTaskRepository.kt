package it.pagopa.selfcare.backoffice.scheduler.repositories

import it.pagopa.selfcare.backoffice.scheduler.documents.IbanDeletionRequest
import org.springframework.data.mongodb.repository.Query
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Flux

interface IbanDeletionRequestRepository : ReactiveMongoRepository<IbanDeletionRequest, String> {

    @Query("{ 'status' : ?0, 'scheduledExecutionDate' : { \$lte: ?1 } }")
    fun findExecutableTasks(
        status: String,
        scheduledExecutionDate: String,
    ): Flux<IbanDeletionRequest>
}
