package it.pagopa.selfcare.backoffice.scheduler.repositories

import it.pagopa.selfcare.backoffice.scheduler.documents.ScheduledTask
import org.springframework.data.mongodb.repository.Query
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Flux

interface ScheduledTaskRepository : ReactiveMongoRepository<ScheduledTask, String> {

    @Query("{ 'status' : ?0, 'scheduledExecutionDate' : { \$lte: ?1 } }")
    fun findExecutableTasks(status: String, scheduledExecutionDate: String): Flux<ScheduledTask>
}
