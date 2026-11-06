package com.sourcecodesolutions.techassesment.westpac.account;

import static com.sourcecodesolutions.techassesment.westpac.account.AccountTestSupport.createAccount;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sourcecodesolutions.techassesment.westpac.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;

/** Stops Redis mid-test, so the context is discarded afterwards. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@DirtiesContext
class RedisOutageIntegrationTest {

    private static final String CUSTOMER = "Jane Citizen";
    private static final String NICKNAME = "Holiday Fund";
    // The Postgres container is also a GenericContainer, so select Redis by bean name.
    private static final String REDIS_CONTAINER_BEAN = "redisContainer";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    @Qualifier(REDIS_CONTAINER_BEAN)
    private GenericContainer<?> redis;

    @Test
    void readsFallBackToDatabaseWhenCacheIsDown() throws Exception {
        String location = createAccount(mockMvc, CUSTOMER, NICKNAME);

        redis.stop();

        mockMvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerName").value(CUSTOMER));
    }
}
