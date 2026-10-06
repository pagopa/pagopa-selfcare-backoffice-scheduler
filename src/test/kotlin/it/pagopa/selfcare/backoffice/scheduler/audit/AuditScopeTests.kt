package it.pagopa.selfcare.backoffice.scheduler.audit

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
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

    @Test
    fun `nested scope preserves marker until outer scope closes`() {
        AuditScope.enable().use {
            AuditScope.enable().use { assertEquals("true", MDC.get("audit")) }
            assertEquals("true", MDC.get("audit"))
        }

        assertNull(MDC.get("audit"))
    }

    @Test
    fun `scope clears marker when operation throws`() {
        assertFailsWith<IllegalStateException> {
            AuditScope.enable().use {
                assertEquals("true", MDC.get("audit"))
                throw IllegalStateException("Operation failed")
            }
        }

        assertNull(MDC.get("audit"))
        AuditScope.enable().use { assertEquals("true", MDC.get("audit")) }
        assertNull(MDC.get("audit"))
    }

    @Test
    fun `scope preserves unrelated MDC entries`() {
        MDC.put("taskId", "task-123")
        try {
            AuditScope.enable().use { assertEquals("task-123", MDC.get("taskId")) }

            assertEquals("task-123", MDC.get("taskId"))
            assertNull(MDC.get("audit"))
        } finally {
            MDC.remove("taskId")
        }
    }

    @Test
    fun `concurrent scopes are isolated between threads`() {
        val executor = Executors.newFixedThreadPool(2)
        val outerScopeOpened = CountDownLatch(1)
        val otherScopeClosed = CountDownLatch(1)

        try {
            val first =
                executor.submit {
                    AuditScope.enable().use {
                        outerScopeOpened.countDown()
                        assertTrue(otherScopeClosed.await(5, TimeUnit.SECONDS))
                        assertEquals("true", MDC.get("audit"))
                    }
                    assertNull(MDC.get("audit"))
                }
            val second =
                executor.submit {
                    try {
                        assertTrue(outerScopeOpened.await(5, TimeUnit.SECONDS))
                        assertNull(MDC.get("audit"))
                        AuditScope.enable().use { assertEquals("true", MDC.get("audit")) }
                        assertNull(MDC.get("audit"))
                    } finally {
                        otherScopeClosed.countDown()
                    }
                }

            first.get(10, TimeUnit.SECONDS)
            second.get(10, TimeUnit.SECONDS)
            assertNull(MDC.get("audit"))
        } finally {
            executor.shutdownNow()
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS))
        }
    }
}
