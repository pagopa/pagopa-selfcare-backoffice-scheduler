package it.pagopa.selfcare.backoffice.scheduler.repositories

import it.pagopa.selfcare.backoffice.scheduler.documents.ScheduledTask
import it.pagopa.selfcare.backoffice.scheduler.documents.TaskStatus
import java.time.Instant
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Flux

interface ScheduledTaskRepository : ReactiveMongoRepository<ScheduledTask, String> {

    /**
     * Key method for the Scheduler: finds all PENDING and UNCANCELED tasks whose scheduled
     * execution date is less than or equal to the current timestamp.
     */
    fun findAllByStatusAndScheduledExecutionDateBeforeAndCancellationRequestedIsFalse(
        status: TaskStatus,
        now: Instant,
    ): Flux<ScheduledTask>
}
