package it.pagopa.selfcare.backoffice.scheduler.services

import it.pagopa.selfcare.backoffice.scheduler.clients.ApiConfigClient
import it.pagopa.selfcare.backoffice.scheduler.documents.IbanDeletionRequest
import it.pagopa.selfcare.backoffice.scheduler.documents.IbanDeletionRequestStatus
import it.pagopa.selfcare.backoffice.scheduler.repositories.IbanDeletionRequestRepository
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.jupiter.api.BeforeEach
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

    @Mock private lateinit var repository: IbanDeletionRequestRepository

    private lateinit var service: IbanDeletionService

    private val creditorInstitutionCodeMock = "77777777777"

    private val ibanMock = "IT0000000000000000234"

    @BeforeEach
    fun setup() {
        service = IbanDeletionService(apiConfigClient, repository)
    }

    @Test
    fun `should successfully process IBAN deletion task`() {
        // Given
        val task =
            IbanDeletionRequest(
                id = "2",
                requestedAt = Instant.now().toString(),
                scheduledExecutionDate = Instant.now().toString(),
                updatedAt = Instant.now().toString(),
                status = IbanDeletionRequestStatus.PENDING,
                creditorInstitutionCode = creditorInstitutionCodeMock,
                ibanValue = ibanMock,
            )

        whenever(repository.save(any())).thenReturn(Mono.just(task))
        whenever(apiConfigClient.deleteCreditorInstitutionsIban(any(), any(), any()))
            .thenReturn(Mono.just("Success"))

        // When & Then
        StepVerifier.create(service.processTask(task))
            .assertNext { result ->
                assertEquals(IbanDeletionRequestStatus.COMPLETED, result.status)
                assertNotNull(result.updatedAt)
            }
            .verifyComplete()
    }
}
