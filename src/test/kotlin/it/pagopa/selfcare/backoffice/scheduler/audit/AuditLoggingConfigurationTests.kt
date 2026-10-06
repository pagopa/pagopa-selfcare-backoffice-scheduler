package it.pagopa.selfcare.backoffice.scheduler.audit

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.slf4j.LoggerFactory
import org.springframework.boot.logging.LogLevel
import org.springframework.boot.logging.LoggingInitializationContext
import org.springframework.boot.logging.LoggingSystem
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.mock.env.MockEnvironment

@ExtendWith(OutputCaptureExtension::class)
class AuditLoggingConfigurationTest {

    private val loggingSystem = LoggingSystem.get(javaClass.classLoader)
    private var previousRootLevel = LogLevel.INFO

    @BeforeEach
    fun rememberLoggingLevel() {
        previousRootLevel =
            loggingSystem.getLoggerConfiguration(LoggingSystem.ROOT_LOGGER_NAME).effectiveLevel
    }

    @AfterEach
    fun restoreLogging() {
        loggingSystem.cleanUp()
        loggingSystem.initialize(
            LoggingInitializationContext(MockEnvironment()),
            "classpath:logback-spring.xml",
            null,
        )
        loggingSystem.setLogLevel(null, previousRootLevel)
    }

    @ParameterizedTest
    @ValueSource(strings = ["local", "test"])
    fun `audit events are emitted once as ECS JSON in every profile`(
        profile: String,
        output: CapturedOutput,
    ) {
        val environment = MockEnvironment().withProperty("build.version", "test-version")
        environment.setActiveProfiles(profile)
        loggingSystem.cleanUp()
        loggingSystem.beforeInitialize()
        loggingSystem.initialize(
            LoggingInitializationContext(environment),
            "classpath:logback-spring.xml",
            null,
        )
        val auditLogger = LoggerFactory.getLogger("auditLogs")
        val ordinaryLogger = LoggerFactory.getLogger("it.pagopa.audit-config-test")
        loggingSystem.setLogLevel(null, LogLevel.ERROR)

        auditLogger.info("Audit routing test")
        ordinaryLogger.error("Ordinary routing test")

        val auditLines =
            output.out.lineSequence().filter { it.contains("Audit routing test") }.toList()
        assertEquals(1, auditLines.size)
        val json = jacksonObjectMapper().readTree(auditLines.single())
        assertEquals("true", json["audit"].asText())
        assertEquals("auditLogs", json["log.logger"].asText())
        assertEquals("test-version", json["service.version"].asText())

        val ordinaryLines =
            output.out.lineSequence().filter { it.contains("Ordinary routing test") }.toList()
        assertEquals(1, ordinaryLines.size)
        if (profile != "local") {
            assertFalse(jacksonObjectMapper().readTree(ordinaryLines.single()).has("audit"))
        }
    }
}
