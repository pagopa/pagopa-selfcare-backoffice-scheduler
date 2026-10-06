package it.pagopa.selfcare.backoffice.scheduler.audit

import org.slf4j.MDC

class AuditScope private constructor(private val state: AuditState, private val isOwner: Boolean) :
    AutoCloseable {

    override fun close() {
        if (--state.depth == 0) {
            if (isOwner) {
                MDC.remove(MDC_AUDIT_KEY)
            }
            auditState.remove()
        }
    }

    companion object {
        private const val MDC_AUDIT_KEY = "audit"
        private const val MDC_AUDIT_VALUE = "true"
        private val auditState = ThreadLocal.withInitial(::AuditState)

        @JvmStatic
        fun enable(): AuditScope {
            val state = auditState.get()

            if (state.depth++ == 0) {
                MDC.put(MDC_AUDIT_KEY, MDC_AUDIT_VALUE)
                return AuditScope(state, true)
            }

            return AuditScope(state, false)
        }
    }

    private class AuditState(var depth: Int = 0)
}
