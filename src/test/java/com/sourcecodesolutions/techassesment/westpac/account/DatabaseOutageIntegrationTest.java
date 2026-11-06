package com.sourcecodesolutions.techassesment.westpac.account;

import static com.sourcecodesolutions.techassesment.westpac.account.AccountTestSupport.createAccount;
import static com.sourcecodesolutions.techassesment.westpac.account.AccountTestSupport.createRequest;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sourcecodesolutions.techassesment.westpac.TestcontainersConfiguration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Stops Postgres mid-test, so the context is discarded afterwards. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@DirtiesContext
class DatabaseOutageIntegrationTest {

    private static final String CUSTOMER = "Jane Citizen";
    private static final String NICKNAME = "Holiday Fund";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PostgreSQLContainer postgres;

    @Test
    void cachedReadsSurviveAndEverythingElseReturns503() throws Exception {
        String cachedLocation = createAccount(mockMvc, CUSTOMER, NICKNAME);
        mockMvc.perform(get(cachedLocation)).andExpect(status().isOk());

        postgres.stop();

        mockMvc.perform(get(cachedLocation))
                .andExpect(status().isOk());
        mockMvc.perform(get(AccountController.BASE_PATH + "/{id}", UUID.randomUUID()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER));
        mockMvc.perform(createRequest(CUSTOMER, NICKNAME))
                .andExpect(status().isServiceUnavailable());
    }
}
