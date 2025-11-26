package it.pagopa.selfcare.backoffice.scheduler.clients

import it.pagopa.generated.apiconfig.api.IbansApi
import it.pagopa.selfcare.backoffice.scheduler.exceptions.ApiConfigClientException
import java.time.Duration
import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.util.retry.Retry

@ConfigurationProperties(prefix = "apiconfig")
data class ApiConfigClientProperties(
    val maxRetryAttempts: Long = 3,
    val retryBackoffDurationMs: Long = 1000,
    val enableRetry: Boolean = true,
)

@Component
@EnableConfigurationProperties(ApiConfigClientProperties::class)
class ApiConfigClient(
    private val ibanApiConfigApi: IbansApi,
    private val properties: ApiConfigClientProperties,
) {

    companion object {
        private val logger = LoggerFactory.getLogger(ApiConfigClient::class.java)
    }

    /**
     * Deletes an IBAN associated with a creditor institution
     *
     * @param creditorInstitutionCode The code of the creditor institution
     * @param ibanValue The IBAN value to delete
     * @param xRequestId Optional request ID for tracing (auto-generated if not provided)
     * @return A Mono emitting the deletion result or an error
     */
    fun deleteCreditorInstitutionsIban(
        creditorInstitutionCode: String,
        ibanValue: String,
        xRequestId: String = UUID.randomUUID().toString(),
    ): Mono<String> {
        require(creditorInstitutionCode.isNotBlank()) { "creditorInstitutionCode cannot be blank" }
        require(ibanValue.isNotBlank()) { "ibanValue cannot be blank" }

        logger.debug(
            "Deleting IBAN - xRequestId: {}, creditorInstitutionCode: {}, ibanValue: {}",
            xRequestId,
            creditorInstitutionCode,
            ibanValue,
        )

        val apiCall =
            ibanApiConfigApi
                .deleteCreditorInstitutionsIban(creditorInstitutionCode, ibanValue, xRequestId)
                .doOnSuccess {
                    logger.info(
                        "IBAN deleted successfully - xRequestId: {}, creditorInstitutionCode: {}, ibanValue: {}",
                        xRequestId,
                        creditorInstitutionCode,
                        ibanValue,
                    )
                }
                .doOnError { error ->
                    logger.error(
                        "Error deleting IBAN - xRequestId: {}, creditorInstitutionCode: {}, ibanValue: {}",
                        xRequestId,
                        creditorInstitutionCode,
                        ibanValue,
                        error,
                    )
                }

        return if (properties.enableRetry) {
                apiCall.retryWhen(
                    Retry.backoff(
                            properties.maxRetryAttempts,
                            Duration.ofMillis(properties.retryBackoffDurationMs),
                        )
                        .filter { isRetryableException(it) }
                        .doBeforeRetry { signal ->
                            logger.warn(
                                "Retrying deleteCreditorInstitutionsIban - xRequestId: {}, attempt: {}/{}, creditorInstitutionCode: {}, ibanValue: {}",
                                xRequestId,
                                signal.totalRetries() + 1,
                                properties.maxRetryAttempts,
                                creditorInstitutionCode,
                                ibanValue,
                            )
                        }
                )
            } else {
                apiCall
            }
            .onErrorMap { error ->
                ApiConfigClientException(
                    "Failed to delete IBAN - xRequestId: $xRequestId, creditorInstitutionCode: $creditorInstitutionCode, ibanValue: $ibanValue",
                    error,
                )
            }
    }

    private fun isRetryableException(throwable: Throwable): Boolean {
        return throwable is java.net.ConnectException ||
            throwable is java.util.concurrent.TimeoutException ||
            throwable is java.net.SocketTimeoutException ||
            throwable is org.springframework.web.reactive.function.client.WebClientRequestException
    }
}
