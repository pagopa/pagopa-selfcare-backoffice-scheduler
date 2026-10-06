package it.pagopa.selfcare.backoffice.scheduler.audit

import org.slf4j.Logger
import org.springframework.stereotype.Component

@Component
class AuditLogger {

    fun info(logger: Logger, format: String, vararg args: Any?) {
        AuditScope.enable().use { logger.info(format, *args) }
    }

    fun warn(logger: Logger, format: String, vararg args: Any?) {
        AuditScope.enable().use { logger.warn(format, *args) }
    }

    fun error(logger: Logger, format: String, vararg args: Any?) {
        AuditScope.enable().use { logger.error(format, *args) }
    }

    fun error(logger: Logger, message: String, throwable: Throwable) {
        AuditScope.enable().use { logger.error(message, throwable) }
    }
}
