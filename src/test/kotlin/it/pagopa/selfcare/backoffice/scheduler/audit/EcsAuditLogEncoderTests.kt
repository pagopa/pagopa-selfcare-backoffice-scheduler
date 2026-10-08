package it.pagopa.selfcare.backoffice.scheduler.audit

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.LoggingEvent
import co.elastic.logging.logback.EcsEncoder
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import org.slf4j.MDC

class EcsAuditLogEncoderTest {

    @Test
    fun `audit encoder adds marker without MDC and preserves ECS fields`() {
        val context = LoggerContext()
        context.mdcAdapter = MDC.getMDCAdapter()
        val encoder = EcsAuditLogEncoder()
        encoder.context = context
        encoder.setServiceName("backoffice-scheduler")
        encoder.setServiceVersion("1.0")
        encoder.setServiceEnvironment("test")
        try {
            encoder.start()
            val event =
                LoggingEvent(
                    javaClass.name,
                    context.getLogger("auditLogs"),
                    Level.INFO,
                    "taskId={}",
                    null,
                    arrayOf("task-123"),
                )
            event.mdcPropertyMap = emptyMap()
            val json = jacksonObjectMapper().readTree(encoder.encode(event))

            assertEquals("true", json["audit"].asText())
            assertEquals("INFO", json["log.level"].asText())
            assertEquals("taskId=task-123", json["message"].asText())
            assertEquals("auditLogs", json["log.logger"].asText())
            assertEquals("backoffice-scheduler", json["service.name"].asText())
            assertEquals("1.0", json["service.version"].asText())
            assertEquals("test", json["service.environment"].asText())
        } finally {
            encoder.stop()
            context.stop()
        }
    }

    @Test
    fun `ordinary ECS encoder does not add audit marker`() {
        val context = LoggerContext()
        context.mdcAdapter = MDC.getMDCAdapter()
        val encoder = EcsEncoder()
        encoder.context = context
        try {
            encoder.start()
            val event =
                LoggingEvent(
                    javaClass.name,
                    context.getLogger("ordinary"),
                    Level.INFO,
                    "Ordinary event",
                    null,
                    null,
                )
            event.mdcPropertyMap = emptyMap()

            assertFalse(jacksonObjectMapper().readTree(encoder.encode(event)).has("audit"))
        } finally {
            encoder.stop()
            context.stop()
        }
    }
}
