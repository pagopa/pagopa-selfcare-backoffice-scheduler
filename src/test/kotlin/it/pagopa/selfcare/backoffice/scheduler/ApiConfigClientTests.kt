package it.pagopa.selfcare.backoffice.scheduler

import it.pagopa.generated.apiconfig.api.IbansApi
import it.pagopa.selfcare.backoffice.scheduler.clients.ApiConfigClient
import it.pagopa.selfcare.backoffice.scheduler.clients.ApiConfigClientProperties
import it.pagopa.selfcare.backoffice.scheduler.exceptions.ApiConfigClientException
import java.net.ConnectException
import java.util.UUID
import java.util.concurrent.TimeoutException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

@ExtendWith(MockitoExtension::class)
class ApiConfigClientTest {

    @Mock private lateinit var ibansApi: IbansApi

    private lateinit var apiConfigClient: ApiConfigClient
    private lateinit var properties: ApiConfigClientProperties

    private val creditorInstitutionCode = "12345"
    private val ibanValue = "IT00X0000000000000000123456"
    private val xRequestId = UUID.randomUUID().toString()

    @BeforeEach
    fun setup() {
        properties =
            ApiConfigClientProperties(
                maxRetryAttempts = 3,
                retryBackoffDurationMs = 100, // Ridotto per i test
                enableRetry = true,
            )
        apiConfigClient = ApiConfigClient(ibansApi, properties)
    }

    @Test
    fun `should successfully delete IBAN`() {
        // Given
        val expectedResponse = "IBAN deleted successfully"
        whenever(
                ibansApi.deleteCreditorInstitutionsIban(
                    eq(creditorInstitutionCode),
                    eq(ibanValue),
                    any(),
                )
            )
            .thenReturn(Mono.just(expectedResponse))

        // When
        val result =
            apiConfigClient.deleteCreditorInstitutionsIban(creditorInstitutionCode, ibanValue)

        // Then
        StepVerifier.create(result).expectNext(expectedResponse).verifyComplete()

        verify(ibansApi)
            .deleteCreditorInstitutionsIban(eq(creditorInstitutionCode), eq(ibanValue), any())
    }

    @Test
    fun `should successfully delete IBAN with custom xRequestId`() {
        // Given
        val expectedResponse = "IBAN deleted successfully"
        whenever(
                ibansApi.deleteCreditorInstitutionsIban(
                    creditorInstitutionCode,
                    ibanValue,
                    xRequestId,
                )
            )
            .thenReturn(Mono.just(expectedResponse))

        // When
        val result =
            apiConfigClient.deleteCreditorInstitutionsIban(
                creditorInstitutionCode,
                ibanValue,
                xRequestId,
            )

        // Then
        StepVerifier.create(result).expectNext(expectedResponse).verifyComplete()

        verify(ibansApi)
            .deleteCreditorInstitutionsIban(creditorInstitutionCode, ibanValue, xRequestId)
    }

    @Test
    fun `should throw exception when creditorInstitutionCode is blank`() {
        // When & Then
        assertThrows<IllegalArgumentException> {
            apiConfigClient.deleteCreditorInstitutionsIban("", ibanValue)
        }
    }

    @Test
    fun `should throw exception when ibanValue is blank`() {
        // When & Then
        assertThrows<IllegalArgumentException> {
            apiConfigClient.deleteCreditorInstitutionsIban(creditorInstitutionCode, "")
        }
    }

    @Test
    fun `should wrap error in ApiConfigClientException on failure`() {
        // Given
        val errorMessage = "Connection failed"
        whenever(
                ibansApi.deleteCreditorInstitutionsIban(
                    eq(creditorInstitutionCode),
                    eq(ibanValue),
                    any(),
                )
            )
            .thenReturn(Mono.error(RuntimeException(errorMessage)))

        // When
        val result =
            apiConfigClient.deleteCreditorInstitutionsIban(creditorInstitutionCode, ibanValue)

        // Then
        StepVerifier.create(result)
            .expectErrorMatches { error ->
                error is ApiConfigClientException &&
                    error.message?.contains("Failed to delete IBAN") == true &&
                    error.message?.contains(creditorInstitutionCode) == true &&
                    error.message?.contains(ibanValue) == true &&
                    error.cause?.message == errorMessage
            }
            .verify()
    }

    @Test
    fun `should retry on ConnectException and eventually succeed`() {

        var attempt = 0

        // Given
        whenever(
                ibansApi.deleteCreditorInstitutionsIban(
                    eq(creditorInstitutionCode),
                    eq(ibanValue),
                    anyOrNull(),
                )
            )
            .thenAnswer {
                Mono.defer<String> {
                    if (attempt < 2) {
                        attempt++
                        Mono.error<String>(ConnectException("Simulated failure $attempt"))
                    } else {
                        Mono.just("IBAN deleted successfully")
                    }
                }
            }
        // When
        val resultMono =
            apiConfigClient
                .deleteCreditorInstitutionsIban(creditorInstitutionCode, ibanValue)
                .retry(2)

        // Then
        StepVerifier.create(resultMono).expectNext("IBAN deleted successfully").verifyComplete()
    }

    @Test
    fun `should retry on TimeoutException and eventually succeed`() {
        // Given
        val expectedResponse = "IBAN deleted successfully"
        var attempt = 0

        // Given
        whenever(
                ibansApi.deleteCreditorInstitutionsIban(
                    eq(creditorInstitutionCode),
                    eq(ibanValue),
                    anyOrNull(),
                )
            )
            .thenAnswer {
                Mono.defer<String> {
                    if (attempt < 2) {
                        attempt++
                        Mono.error<String>(TimeoutException("TimeoutException failure $attempt"))
                    } else {
                        Mono.just("IBAN deleted successfully")
                    }
                }
            }

        // When
        val result =
            apiConfigClient.deleteCreditorInstitutionsIban(creditorInstitutionCode, ibanValue)

        // Then
        StepVerifier.create(result).expectNext(expectedResponse).verifyComplete()

        verify(ibansApi, org.mockito.kotlin.times(1))
            .deleteCreditorInstitutionsIban(eq(creditorInstitutionCode), eq(ibanValue), any())
    }

    @Test
    fun `should fail after max retry attempts`() {
        // Given
        whenever(
                ibansApi.deleteCreditorInstitutionsIban(
                    eq(creditorInstitutionCode),
                    eq(ibanValue),
                    any(),
                )
            )
            .thenReturn(Mono.error(ConnectException("Connection refused")))

        // When
        val result =
            apiConfigClient.deleteCreditorInstitutionsIban(creditorInstitutionCode, ibanValue)

        // Then
        StepVerifier.create(result).expectError(ApiConfigClientException::class.java).verify()

        verify(ibansApi, org.mockito.kotlin.times(1))
            .deleteCreditorInstitutionsIban(eq(creditorInstitutionCode), eq(ibanValue), any())
    }

    @Test
    fun `should not retry on non-retryable exceptions`() {
        // Given
        val nonRetryableException = IllegalArgumentException("Invalid argument")
        whenever(
                ibansApi.deleteCreditorInstitutionsIban(
                    eq(creditorInstitutionCode),
                    eq(ibanValue),
                    any(),
                )
            )
            .thenReturn(Mono.error(nonRetryableException))

        // When
        val result =
            apiConfigClient.deleteCreditorInstitutionsIban(creditorInstitutionCode, ibanValue)

        // Then
        StepVerifier.create(result).expectError(ApiConfigClientException::class.java).verify()

        verify(ibansApi, org.mockito.kotlin.times(1))
            .deleteCreditorInstitutionsIban(eq(creditorInstitutionCode), eq(ibanValue), any())
    }

    @Test
    fun `should not retry when retry is disabled`() {
        // Given
        val propertiesWithoutRetry =
            ApiConfigClientProperties(
                maxRetryAttempts = 3,
                retryBackoffDurationMs = 100,
                enableRetry = false,
            )
        val clientWithoutRetry = ApiConfigClient(ibansApi, propertiesWithoutRetry)

        whenever(
                ibansApi.deleteCreditorInstitutionsIban(
                    eq(creditorInstitutionCode),
                    eq(ibanValue),
                    any(),
                )
            )
            .thenReturn(Mono.error(ConnectException("Connection refused")))

        // When
        val result =
            clientWithoutRetry.deleteCreditorInstitutionsIban(creditorInstitutionCode, ibanValue)

        // Then
        StepVerifier.create(result).expectError(ApiConfigClientException::class.java).verify()

        verify(ibansApi, org.mockito.kotlin.times(1))
            .deleteCreditorInstitutionsIban(eq(creditorInstitutionCode), eq(ibanValue), any())
    }

    @Test
    fun `should include all context in error message`() {
        // Given
        val customRequestId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000").toString()
        whenever(
                ibansApi.deleteCreditorInstitutionsIban(
                    creditorInstitutionCode,
                    ibanValue,
                    customRequestId,
                )
            )
            .thenReturn(Mono.error(RuntimeException("API Error")))

        // When
        val result =
            apiConfigClient.deleteCreditorInstitutionsIban(
                creditorInstitutionCode,
                ibanValue,
                customRequestId,
            )

        // Then
        StepVerifier.create(result)
            .expectErrorMatches { error ->
                error is ApiConfigClientException &&
                    error.message?.contains(customRequestId.toString()) == true &&
                    error.message?.contains(creditorInstitutionCode) == true &&
                    error.message?.contains(ibanValue) == true
            }
            .verify()
    }
}
