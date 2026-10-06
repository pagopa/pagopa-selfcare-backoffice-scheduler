package it.pagopa.selfcare.backoffice.scheduler.audit

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.slf4j.MDC
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.test.StepVerifier

class AuditLoggerTest {

    private val auditLogger = AuditLogger()
    private lateinit var context: LoggerContext
    private lateinit var logger: Logger
    private lateinit var appender: ListAppender<ILoggingEvent>

    @BeforeEach
    fun setup() {
        context = LoggerContext()
        context.mdcAdapter = MDC.getMDCAdapter()
        logger = context.getLogger("audit-test")
        logger.level = Level.INFO
        appender =
            object : ListAppender<ILoggingEvent>() {
                override fun append(event: ILoggingEvent) {
                    event.prepareForDeferredProcessing()
                    super.append(event)
                }
            }
        appender.context = context
        appender.start()
        logger.addAppender(appender)
    }

    @AfterEach
    fun cleanup() {
        context.stop()
        MDC.remove("audit")
    }

    @ParameterizedTest
    @ValueSource(strings = ["INFO", "WARN", "ERROR"])
    fun `log event contains audit marker and formatted arguments`(level: String) {
        when (level) {
            "INFO" -> auditLogger.info(logger, "taskId={} result={}", "task-123", null)
            "WARN" -> auditLogger.warn(logger, "taskId={} result={}", "task-123", null)
            "ERROR" -> auditLogger.error(logger, "taskId={} result={}", "task-123", null)
            else -> error("Unexpected level: $level")
        }

        val event = appender.list.single()
        assertEquals(Level.valueOf(level), event.level)
        assertEquals("taskId=task-123 result=null", event.formattedMessage)
        assertEquals("true", event.mdcPropertyMap["audit"])
        assertNull(MDC.get("audit"))
    }

    @Test
    fun `error log retains throwable and audit marker`() {
        val failure = IllegalStateException("Deletion failed")

        auditLogger.error(logger, "Cannot delete IBAN", failure)

        val event = appender.list.single()
        assertEquals(Level.ERROR, event.level)
        assertEquals("Cannot delete IBAN", event.formattedMessage)
        assertEquals(failure.javaClass.name, event.throwableProxy.className)
        assertEquals("Deletion failed", event.throwableProxy.message)
        assertEquals("true", event.mdcPropertyMap["audit"])
        assertNull(MDC.get("audit"))
    }

    @Test
    fun `ordinary logs after audit call do not contain audit marker`() {
        auditLogger.info(logger, "Audit event")
        logger.info("Ordinary event")

        assertEquals("true", appender.list[0].mdcPropertyMap["audit"])
        assertNull(appender.list[1].mdcPropertyMap["audit"])
        assertEquals(2, appender.list.size)
    }

    @Test
    fun `audit logger preserves an enclosing scope`() {
        AuditScope.enable().use {
            auditLogger.warn(logger, "Audit event")
            assertEquals("true", MDC.get("audit"))
        }

        assertEquals("true", appender.list.single().mdcPropertyMap["audit"])
        assertNull(MDC.get("audit"))
    }

    @Test
    fun `audit marker is removed when logger throws`() {
        val failingLogger = mock<org.slf4j.Logger>()
        val failure = IllegalStateException("Logger failed")
        doAnswer {
                assertEquals("true", MDC.get("audit"))
                throw failure
            }
            .whenever(failingLogger)
            .info("Audit event", *emptyArray<Any?>())

        val thrown =
            assertFailsWith<IllegalStateException> {
                auditLogger.info(failingLogger, "Audit event")
            }

        assertSame(failure, thrown)
        assertNull(MDC.get("audit"))
    }

    @Test
    fun `audit call after reactive scheduler switch marks only its event`() {
        val scheduler = Schedulers.newSingle("audit-test")
        try {
            StepVerifier.create(
                    Mono.just("task-123").publishOn(scheduler).doOnNext { taskId ->
                        auditLogger.info(logger, "taskId={}", taskId)
                        assertNull(MDC.get("audit"))
                        logger.info("Ordinary event")
                    }
                )
                .expectNext("task-123")
                .expectComplete()
                .verify(Duration.ofSeconds(5))

            assertEquals("taskId=task-123", appender.list[0].formattedMessage)
            assertEquals("true", appender.list[0].mdcPropertyMap["audit"])
            assertNull(appender.list[1].mdcPropertyMap["audit"])
            assertEquals(2, appender.list.size)
            assertNull(MDC.get("audit"))
        } finally {
            scheduler.dispose()
        }
    }
}
