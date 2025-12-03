package it.pagopa.selfcare.backoffice.scheduler.documents

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import org.springframework.data.mongodb.core.mapping.FieldType

/** Definition of possible states for the task. */
enum class IbanDeletionRequestStatus {
    PENDING,
    CANCELED,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
}

/**
 * Generic model that maps the 'scheduledTasks' collection in MongoDB. This entity supports any type
 * of scheduled background processing.
 */
@Document(collection = "scheduledTasks")
data class IbanDeletionRequest(
    @Id @Field(targetType = FieldType.STRING) val id: String,
    var requestedAt: String,
    var updatedAt: String,
    val scheduledExecutionDate: String,
    var status: IbanDeletionRequestStatus = IbanDeletionRequestStatus.PENDING,
    val creditorInstitutionCode: String,
    val ibanValue: String,
)
