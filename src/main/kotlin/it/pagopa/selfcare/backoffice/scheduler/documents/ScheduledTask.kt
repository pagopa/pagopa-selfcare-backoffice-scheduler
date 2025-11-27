package it.pagopa.selfcare.backoffice.scheduler.documents

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import org.springframework.data.mongodb.core.mapping.FieldType

/**
 * Defines the type of scheduled task. This is crucial for the scheduler to dispatch the task to the
 * correct handler.
 */
enum class TaskType {
    IBAN_DELETION
}

/** Definition of possible states for the task. */
enum class TaskStatus {
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
data class ScheduledTask(
    @Id @Field(targetType = FieldType.STRING) val id: String,
    val type: TaskType,
    val data: Map<String, Any>,
    val userId: String,
    val requestedAt: String,
    val scheduledExecutionDate: String,
    var status: TaskStatus = TaskStatus.PENDING,
    var cancellationRequested: Boolean = false,
    var cancellationRequestedAt: String? = null,
    var processedAt: String? = null,
    var completedAt: String? = null,
)
