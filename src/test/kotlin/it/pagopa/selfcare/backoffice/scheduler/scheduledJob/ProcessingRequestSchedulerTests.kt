package it.pagopa.selfcare.backoffice.scheduler.scheduledJob

import it.pagopa.selfcare.backoffice.scheduler.documents.IbanDeletionRequest
import it.pagopa.selfcare.backoffice.scheduler.documents.IbanDeletionRequestStatus
import it.pagopa.selfcare.backoffice.scheduler.repositories.IbanDeletionRequestsRepository
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

    @Mock private lateinit var repository: IbanDeletionRequestsRepository

    @Mock private lateinit var ibanDeletionService: IbanDeletionService

    @InjectMocks private lateinit var scheduler: ProcessingRequestScheduler

    private val creditorInstitutionCodeMock = "77777777777"
    private val ibanMock = "IT0000000000000000234"

    private val mockTask1 =
        IbanDeletionRequest(
            id = "1",
            requestedAt = Instant.now().toString(),
            scheduledExecutionDate = Instant.now().toString(),
            updatedAt = Instant.now().toString(),
            status = IbanDeletionRequestStatus.PENDING,
            creditorInstitutionCode = creditorInstitutionCodeMock,
            ibanValue = ibanMock,
        )

    private val mockTask2 =
        IbanDeletionRequest(
            id = "2",
            requestedAt = Instant.now().toString(),
            scheduledExecutionDate = Instant.now().toString(),
            updatedAt = Instant.now().toString(),
            status = IbanDeletionRequestStatus.PENDING,
            creditorInstitutionCode = creditorInstitutionCodeMock,
            ibanValue = ibanMock,
        )

    @Test
    fun `should fetch pending tasks and process them`() {
        // Given
        val pendingTasks = Flux.just(mockTask1, mockTask2)

        `when`(
                repository.findExecutableTasks(
                    eq(IbanDeletionRequestStatus.PENDING.toString()),
                    any(),
                )
            )
            .thenReturn(pendingTasks)

        `when`(ibanDeletionService.processTask(any())).thenReturn(Mono.just(mockTask1))

        // When
        scheduler.executeScheduledIbanDeletionJobs()

        // Give some time for async processing
        Thread.sleep(100)

        // Then
        verify(repository, times(1))
            .findExecutableTasks(eq(IbanDeletionRequestStatus.PENDING.toString()), any())
        verify(ibanDeletionService, times(2)).processTask(any())
    }

    @Test
    fun `should handle empty repository result gracefully`() {
        // Given
        `when`(
                repository.findExecutableTasks(
                    eq(IbanDeletionRequestStatus.PENDING.toString()),
                    any(),
                )
            )
            .thenReturn(Flux.empty())

        // When
        scheduler.executeScheduledIbanDeletionJobs()

        // Give some time for async processing
        Thread.sleep(100)

        // Then
        verify(repository, times(1))
            .findExecutableTasks(eq(IbanDeletionRequestStatus.PENDING.toString()), any())
        verify(ibanDeletionService, times(0)).processTask(any())
    }

    @Test
    fun `should continue processing remaining tasks if one fails`() {
        // Given
        val pendingTasks = Flux.just(mockTask1, mockTask2)

        `when`(
                repository.findExecutableTasks(
                    eq(IbanDeletionRequestStatus.PENDING.toString()),
                    any(),
                )
            )
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
