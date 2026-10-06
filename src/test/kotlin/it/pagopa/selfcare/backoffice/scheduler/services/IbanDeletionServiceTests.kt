package it.pagopa.selfcare.backoffice.scheduler.services

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import it.pagopa.selfcare.backoffice.scheduler.clients.ApiConfigClient
import it.pagopa.selfcare.backoffice.scheduler.documents.IbanDeletionRequest
import it.pagopa.selfcare.backoffice.scheduler.documents.IbanDeletionRequestStatus
import it.pagopa.selfcare.backoffice.scheduler.repositories.IbanDeletionRequestsRepository
import java.time.Duration
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.test.StepVerifier

@ExtendWith(MockitoExtension::class)
class IbanDeletionServiceTest {

    @Mock private lateinit var apiConfigClient: ApiConfigClient

    @Mock private lateinit var repository: IbanDeletionRequestsRepository

    private lateinit var service: IbanDeletionService

    private lateinit var auditLogger: Logger
    private lateinit var auditAppender: ListAppender<ILoggingEvent>
    private var previousAuditLevel: Level? = null

    private val creditorInstitutionCodeMock = "77777777777"

    private val ibanMock = "IT0000000000000000234"

    @BeforeEach
    fun setup() {
        service = IbanDeletionService(apiConfigClient, repository)
        auditLogger = LoggerFactory.getLogger("auditLogs") as Logger
        previousAuditLevel = auditLogger.level
        auditLogger.level = Level.INFO
        auditAppender =
            object : ListAppender<ILoggingEvent>() {
                override fun append(event: ILoggingEvent) {
                    event.prepareForDeferredProcessing()
                    super.append(event)
                }
            }
        auditAppender.context = auditLogger.loggerContext
        auditAppender.start()
        auditLogger.addAppender(auditAppender)
    }

    @AfterEach
    fun cleanup() {
        auditLogger.detachAppender(auditAppender)
        auditAppender.stop()
        auditLogger.level = previousAuditLevel
    }

    @Test
    fun `should successfully process IBAN deletion task`() {
        // Given
        val task = createTask()

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

        assertAuditEvent()
    }

    @Test
    fun `ApiConfig failure marks task as failed without success audit event`() {
        val task = createTask()
        whenever(repository.save(any())).thenAnswer {
            Mono.just(it.getArgument<IbanDeletionRequest>(0))
        }
        whenever(apiConfigClient.deleteCreditorInstitutionsIban(any(), any(), any()))
            .thenReturn(Mono.error(IllegalStateException("Deletion failed")))

        StepVerifier.create(service.processTask(task))
            .assertNext { assertEquals(IbanDeletionRequestStatus.FAILED, it.status) }
            .verifyComplete()

        assertTrue(auditAppender.list.isEmpty())
    }

    @Test
    fun `canceled task does not emit deletion audit event`() {
        val task = createTask(IbanDeletionRequestStatus.CANCELED)
        whenever(repository.save(any())).thenReturn(Mono.just(task))

        StepVerifier.create(service.processTask(task))
            .assertNext { assertEquals(IbanDeletionRequestStatus.CANCELED, it.status) }
            .verifyComplete()

        assertTrue(auditAppender.list.isEmpty())
    }

    @Test
    fun `successful deletion emits audit event after reactive scheduler switch`() {
        val task = createTask()
        val scheduler = Schedulers.newSingle("iban-audit-test")
        try {
            whenever(repository.save(any())).thenReturn(Mono.just(task))
            whenever(apiConfigClient.deleteCreditorInstitutionsIban(any(), any(), any()))
                .thenReturn(Mono.just("Success").publishOn(scheduler))

            StepVerifier.create(service.processTask(task))
                .assertNext { assertEquals(IbanDeletionRequestStatus.COMPLETED, it.status) }
                .expectComplete()
                .verify(Duration.ofSeconds(5))

            assertAuditEvent()
            assertTrue(auditAppender.list.single().threadName.startsWith("iban-audit-test"))
        } finally {
            scheduler.dispose()
        }
    }

    private fun assertAuditEvent() {
        val event = auditAppender.list.single()
        assertEquals("auditLogs", event.loggerName)
        assertEquals(Level.INFO, event.level)
        assertEquals(
            "event=IBAN_SCHEDULED_DELETE institutionTaxCode=77777777777 " +
                "IBAN=IT0000000000000000234 userId=pagopa-selfcare-backoffice-scheduler",
            event.formattedMessage,
        )
    }

    private fun createTask(status: IbanDeletionRequestStatus = IbanDeletionRequestStatus.PENDING) =
        IbanDeletionRequest(
            id = "2",
            requestedAt = Instant.now().toString(),
            scheduledExecutionDate = Instant.now().toString(),
            updatedAt = Instant.now().toString(),
            status = status,
            creditorInstitutionCode = creditorInstitutionCodeMock,
            ibanValue = ibanMock,
        )
}
