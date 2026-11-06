package com.sourcecodesolutions.techassesment.westpac.account;

import static com.sourcecodesolutions.techassesment.westpac.account.AccountTestSupport.createAccount;
import static com.sourcecodesolutions.techassesment.westpac.account.AccountTestSupport.createRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sourcecodesolutions.techassesment.westpac.TestcontainersConfiguration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/** End-to-end against real Postgres and Redis. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AccountApiIntegrationTest {

    private static final String CUSTOMER = "Jane Citizen";
    private static final String NICKNAME = "Holiday Fund";
    private static final String ACCOUNT_NUMBER_FORMAT = "^\\d{10}$";
    // "stupid" from the bundled word list, disguised with separators and a look-alike digit.
    private static final String DISGUISED_OFFENSIVE_NICKNAME = "St.u.p1d Savings";
    private static final String OFFENSIVE_NICKNAME_MESSAGE = "must not contain offensive language";
    private static final String COUNT_ACCOUNTS_SQL = "SELECT count(*) FROM account";
    // Below the Hikari pool size: each racing thread holds a connection while waiting for the customer lock.
    private static final int CONCURRENT_REQUESTS = 8;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountProperties accountProperties;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void resetState() {
        jdbcTemplate.execute("TRUNCATE TABLE account");
        cacheManager.getCache(AccountConfiguration.ACCOUNTS_CACHE).clear();
    }

    @Test
    void createdAccountCanBeRetrieved() throws Exception {
        String location = createAccount(mockMvc, CUSTOMER, NICKNAME);

        mockMvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.accountNumber", matchesPattern(ACCOUNT_NUMBER_FORMAT)))
                .andExpect(jsonPath("$.customerName").value(CUSTOMER))
                .andExpect(jsonPath("$.accountNickname").value(NICKNAME))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    void customerCannotExceedAccountLimitRegardlessOfNameCase() throws Exception {
        for (int i = 0; i < accountProperties.maxPerCustomer(); i++) {
            String name = i % 2 == 0 ? CUSTOMER : CUSTOMER.toUpperCase();
            createAccount(mockMvc, name, NICKNAME);
        }

        mockMvc.perform(createRequest(CUSTOMER.toLowerCase(), NICKNAME))
                .andExpect(status().isUnprocessableContent());
        assertThat(accountCount()).isEqualTo(accountProperties.maxPerCustomer());

        // Other customers are unaffected.
        createAccount(mockMvc, "John Smith", NICKNAME);
    }

    @Test
    void offensiveNicknameIsRejectedUsingTheBundledWordList() throws Exception {
        // Unlike the web-slice test, nothing is mocked: this proves Spring injects the real checker into the validator.
        mockMvc.perform(createRequest(CUSTOMER, DISGUISED_OFFENSIVE_NICKNAME))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.accountNickname", hasItem(OFFENSIVE_NICKNAME_MESSAGE)));
        assertThat(accountCount()).isZero();
    }

    @Test
    void concurrentCreatesNeverExceedAccountLimit() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();
        try (ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_REQUESTS)) {
            IntStream.range(0, CONCURRENT_REQUESTS).forEach(i -> results.add(executor.submit(() -> {
                start.await();
                return tryCreate();
            })));
            start.countDown();
        }

        long succeeded = 0;
        for (Future<Boolean> result : results) {
            succeeded += result.get() ? 1 : 0;
        }
        assertThat(succeeded).isEqualTo(accountProperties.maxPerCustomer());
        assertThat(accountCount()).isEqualTo(accountProperties.maxPerCustomer());
    }

    @Test
    void getIsServedFromCacheOnceLoaded() throws Exception {
        String location = createAccount(mockMvc, CUSTOMER, NICKNAME);
        mockMvc.perform(get(location)).andExpect(status().isOk());

        // Remove the row behind the cache's back; the cached copy must still be returned.
        jdbcTemplate.execute("DELETE FROM account");

        mockMvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerName").value(CUSTOMER));
    }

    @Test
    void unknownAccountIsNotFound() throws Exception {
        mockMvc.perform(get(AccountController.BASE_PATH + "/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    private boolean tryCreate() {
        try {
            accountService.createAccount(new CreateAccountRequest(CUSTOMER, NICKNAME));
            return true;
        } catch (CustomerAccountLimitExceededException e) {
            return false;
        }
    }

    private int accountCount() {
        return jdbcTemplate.queryForObject(COUNT_ACCOUNTS_SQL, Integer.class);
    }
}
