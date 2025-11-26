package it.pagopa.selfcare.backoffice.scheduler.scheduledJob

import it.pagopa.selfcare.backoffice.scheduler.documents.ScheduledTask
import it.pagopa.selfcare.backoffice.scheduler.documents.TaskStatus
import it.pagopa.selfcare.backoffice.scheduler.documents.TaskType
import it.pagopa.selfcare.backoffice.scheduler.repositories.ScheduledTaskRepository
import it.pagopa.selfcare.backoffice.scheduler.scheduledjob.ProcessingRequestScheduler
import it.pagopa.selfcare.backoffice.scheduler.services.IbanDeletionService
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
import reactor.core.publisher.Mono

@ExtendWith(MockitoExtension::class)
class ProcessingRequestSchedulerTest {

    @Mock private lateinit var repository: ScheduledTaskRepository

    @Mock private lateinit var ibanDeletionService: IbanDeletionService

    @InjectMocks private lateinit var scheduler: ProcessingRequestScheduler

    private val mockTask1 =
        ScheduledTask(
            id = "1",
            type = TaskType.IBAN_DELETION,
            userId = "user1",
            data = mapOf("iban" to "IT123", "organizationId" to "ORG123"),
            scheduledExecutionDate = Instant.now(),
        )

    private val mockTask2 =
        ScheduledTask(
            id = "2",
            type = TaskType.IBAN_DELETION,
            userId = "user2",
            data = mapOf("iban" to "IT456", "organizationId" to "ORG456"),
            scheduledExecutionDate = Instant.now(),
        )

    @Test
    fun `should fetch pending tasks and process them`() {
        // Given
        val pendingTasks = Flux.just(mockTask1, mockTask2)

        `when`(repository.findExecutableTasks(eq(TaskStatus.PENDING.toString()), any()))
            .thenReturn(pendingTasks)

        `when`(ibanDeletionService.processTask(any())).thenReturn(Mono.just(mockTask1))

        // When
        scheduler.executeScheduledIbanDeletionJobs()

        // Give some time for async processing
        Thread.sleep(100)

        // Then
        verify(repository, times(1)).findExecutableTasks(eq(TaskStatus.PENDING.toString()), any())
        verify(ibanDeletionService, times(2)).processTask(any())
    }

    @Test
    fun `should handle empty repository result gracefully`() {
        // Given
        `when`(repository.findExecutableTasks(eq(TaskStatus.PENDING.toString()), any()))
            .thenReturn(Flux.empty())

        // When
        scheduler.executeScheduledIbanDeletionJobs()

        // Give some time for async processing
        Thread.sleep(100)

        // Then
        verify(repository, times(1)).findExecutableTasks(eq(TaskStatus.PENDING.toString()), any())
        verify(ibanDeletionService, times(0)).processTask(any())
    }

    @Test
    fun `should continue processing remaining tasks if one fails`() {
        // Given
        val pendingTasks = Flux.just(mockTask1, mockTask2)

        `when`(repository.findExecutableTasks(eq(TaskStatus.PENDING.toString()), any()))
            .thenReturn(pendingTasks)

        `when`(ibanDeletionService.processTask(eq(mockTask1)))
            .thenReturn(Mono.error(RuntimeException("Processing failed")))

        `when`(ibanDeletionService.processTask(eq(mockTask2))).thenReturn(Mono.just(mockTask2))

        // When
        scheduler.executeScheduledIbanDeletionJobs()

        // Give some time for async processing
        Thread.sleep(100)

        // Then - both tasks should be attempted despite first one failing
        verify(ibanDeletionService, times(2)).processTask(any())
    }
}
