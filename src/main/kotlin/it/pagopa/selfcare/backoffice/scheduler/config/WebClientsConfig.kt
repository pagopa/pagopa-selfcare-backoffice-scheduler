package it.pagopa.accounting.reconciliation.bdi.ingestion.config

import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import it.pagopa.generated.apiconfig.ApiClient
import it.pagopa.generated.apiconfig.api.IbansApi
import java.util.concurrent.TimeUnit
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.Connection
import reactor.netty.http.client.HttpClient
import reactor.netty.transport.NameResolverProvider

@Configuration
class WebClientsConfig {

    @Bean
    fun apiConfigWebClient(
        @Value("\${apiconfig.server.uri}") serverUri: String,
        @Value("\${apiconfig.server.readTimeoutMillis}") readTimeoutMillis: Int,
        @Value("\${apiconfig.server.connectionTimeoutMillis}") connectionTimeoutMillis: Int,
    ): WebClient {

        val httpClient =
            HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeoutMillis)
                .doOnConnected { connection: Connection ->
                    connection.addHandlerLast(
                        ReadTimeoutHandler(readTimeoutMillis.toLong(), TimeUnit.MILLISECONDS)
                    )
                }
                .resolver { nameResolverSpec: NameResolverProvider.NameResolverSpec ->
                    nameResolverSpec.ndots(1)
                }

        return ApiClient.buildWebClientBuilder()
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .baseUrl(serverUri)
            .build()
    }

    @Bean
    fun ibanApiConfigApi(
        @Value("\${bdi.server.uri}") serverUri: String,
        apiConfigWebClient: WebClient,
    ): IbansApi {
        val apiClient = ApiClient(apiConfigWebClient)
        apiClient.setBasePath(serverUri)
        return IbansApi(apiClient)
    }
}
