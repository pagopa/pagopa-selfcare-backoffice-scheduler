package it.pagopa.selfcare.backoffice.scheduler.audit

import co.elastic.logging.AdditionalField
import co.elastic.logging.logback.EcsEncoder

class EcsAuditLogEncoder : EcsEncoder() {

    init {
        addAdditionalField(AdditionalField("audit", "true"))
    }
}
