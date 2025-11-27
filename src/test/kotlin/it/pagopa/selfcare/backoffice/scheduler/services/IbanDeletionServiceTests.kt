package it.pagopa.selfcare.backoffice.scheduler.services

import it.pagopa.selfcare.backoffice.scheduler.clients.ApiConfigClient
import it.pagopa.selfcare.backoffice.scheduler.documents.ScheduledTask
import it.pagopa.selfcare.backoffice.scheduler.documents.TaskStatus
import it.pagopa.selfcare.backoffice.scheduler.documents.TaskType
import it.pagopa.selfcare.backoffice.scheduler.repositories.ScheduledTaskRepository
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

@ExtendWith(MockitoExtension::class)
class IbanDeletionServiceTest {

    @Mock private lateinit var apiConfigClient: ApiConfigClient

    @Mock private lateinit var repository: ScheduledTaskRepository

    private lateinit var service: IbanDeletionService

    @BeforeEach
    fun setup() {
        service = IbanDeletionService(apiConfigClient, repository)
    }

    @Test
    fun `should successfully process IBAN deletion task`() {
        // Given
        val task =
            ScheduledTask(
                id = "task-123",
                type = TaskType.IBAN_DELETION,
                data =
                    mapOf(
                        "creditorInstitutionCode" to "12345",
                        "ibanValue" to "IT60X0542811101000000123456",
                    ),
                userId = "user-123",
                scheduledExecutionDate = Instant.now(),
            )

        whenever(repository.save(any())).thenReturn(Mono.just(task))
        whenever(apiConfigClient.deleteCreditorInstitutionsIban(any(), any(), any()))
            .thenReturn(Mono.just("Success"))

        // When & Then
        StepVerifier.create(service.processTask(task))
            .assertNext { result ->
                assertEquals(TaskStatus.COMPLETED, result.status)
                assertNotNull(result.completedAt)
            }
            .verifyComplete()
    }
}
