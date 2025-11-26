package it.pagopa.selfcare.backoffice.scheduler.scheduledJob

import it.pagopa.selfcare.backoffice.scheduler.documents.ScheduledTask
import it.pagopa.selfcare.backoffice.scheduler.documents.TaskStatus
import it.pagopa.selfcare.backoffice.scheduler.documents.TaskType
import it.pagopa.selfcare.backoffice.scheduler.repositories.ScheduledTaskRepository
import it.pagopa.selfcare.backoffice.scheduler.scheduledjob.ProcessingRequestScheduler
import java.time.Instant
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import reactor.core.publisher.Flux

@ExtendWith(MockitoExtension::class)
class ProcessingRequestSchedulerTest {

    @Mock private lateinit var repository: ScheduledTaskRepository

    @InjectMocks private lateinit var scheduler: ProcessingRequestScheduler

    private val mockTask1 =
        ScheduledTask(
            id = "1",
            type = TaskType.IBAN_DELETION,
            userId = "user1",
            data = mapOf("iban" to "IT123"),
            scheduledExecutionDate = Instant.now(),
        )
    private val mockTask2 =
        ScheduledTask(
            id = "2",
            type = TaskType.IBAN_DELETION,
            userId = "user2",
            data = mapOf("iban" to "IT456"),
            scheduledExecutionDate = Instant.now(),
        )

    @Test
    fun `should fetch pending tasks and subscribe to the processing flux`() {
        val pendingTasks = Flux.just(mockTask1, mockTask2)

        `when`(
                repository
                    .findAllByStatusAndScheduledExecutionDateBeforeAndCancellationRequestedIsFalse(
                        eq(TaskStatus.PENDING),
                        any(),
                    )
            )
            .thenReturn(pendingTasks)

        scheduler.executeScheduledJobs()

        verify(repository, times(1))
            .findAllByStatusAndScheduledExecutionDateBeforeAndCancellationRequestedIsFalse(
                eq(TaskStatus.PENDING),
                any(),
            )
    }

    @Test
    fun `should handle empty repository result gracefully`() {
        `when`(
                repository
                    .findAllByStatusAndScheduledExecutionDateBeforeAndCancellationRequestedIsFalse(
                        eq(TaskStatus.PENDING),
                        any(),
                    )
            )
            .thenReturn(Flux.empty())

        scheduler.executeScheduledJobs()

        verify(repository, times(1))
            .findAllByStatusAndScheduledExecutionDateBeforeAndCancellationRequestedIsFalse(
                eq(TaskStatus.PENDING),
                any(),
            )
    }
}
