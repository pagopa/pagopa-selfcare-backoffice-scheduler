package it.pagopa.selfcare.backoffice.scheduler.audit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.slf4j.MDC

class AuditScopeTest {

    @Test
    fun `scope marks audit logs and clears marker when closed`() {
        try {
            AuditScope.enable().use { assertEquals("true", MDC.get("audit")) }

            assertNull(MDC.get("audit"))
        } finally {
            MDC.remove("audit")
        }
    }
}
