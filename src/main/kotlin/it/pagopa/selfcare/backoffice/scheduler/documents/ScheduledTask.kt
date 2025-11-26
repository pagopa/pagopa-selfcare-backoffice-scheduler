package it.pagopa.selfcare.backoffice.scheduler.documents

import java.time.Instant
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

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
    @Id val id: String? = null,
    val type: TaskType,
    val data: Map<String, Any>,
    val userId: String,
    val requestedAt: Instant = Instant.now(),
    val scheduledExecutionDate: Instant,
    var status: TaskStatus = TaskStatus.PENDING,
    var cancellationRequested: Boolean = false,
    var cancellationRequestedAt: Instant? = null,
    var processedAt: Instant? = null,
    var completedAt: Instant? = null,
)
