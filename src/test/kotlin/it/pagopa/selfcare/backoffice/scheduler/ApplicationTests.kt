package it.pagopa.selfcare.backoffice.scheduler

import it.pagopa.generated.apiconfig.api.IbansApi
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.test.context.TestPropertySource

@SpringBootTest
@TestPropertySource(locations = ["classpath:application.test.properties"])
class ApplicationTests {

    @TestConfiguration
    class ApiClientTestConfig {

        @Bean
        @Primary
        fun mockIbansApi(): IbansApi {
            return Mockito.mock(IbansApi::class.java)
        }
    }

    @Test fun contextLoads() {}
}
