package com.sourcecodesolutions.techassesment.westpac.account;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sourcecodesolutions.techassesment.westpac.account.nickname.OffensiveNicknameChecker;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.CannotCreateTransactionException;

/** Web-layer contract: request validation and exception-to-status mapping. */
@WebMvcTest(AccountController.class)
class AccountControllerTest {

    private static final String ACCOUNT_URL = AccountController.BASE_PATH + "/{id}";
    private static final String BODY = """
            {"customerName": "%s", "accountNickname": "%s"}""";
    private static final String BODY_WITHOUT_NICKNAME = """
            {"customerName": "%s"}""";
    private static final String CUSTOMER = "Jane Citizen";
    private static final String NICKNAME = "Holiday Fund";
    private static final String ACCOUNT_NUMBER = "1000000000";
    private static final int MAX_ACCOUNTS = 5;
    private static final String CONNECTION_FAILURE_SQL_STATE = "08006";
    private static final String SYNTAX_ERROR_SQL_STATE = "42601";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;

    @MockitoBean
    private OffensiveNicknameChecker offensiveNicknameChecker;

    @Test
    void createReturns201WithLocationAndBody() throws Exception {
        AccountResponse created = account(NICKNAME);
        when(accountService.createAccount(any())).thenReturn(created);

        mockMvc.perform(createRequest(BODY.formatted(CUSTOMER, NICKNAME)))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, endsWith(created.id().toString())))
                .andExpect(jsonPath("$.id").value(created.id().toString()))
                .andExpect(jsonPath("$.accountNumber").value(ACCOUNT_NUMBER))
                .andExpect(jsonPath("$.accountNickname").value(NICKNAME));
    }

    @Test
    void nicknameIsOptional() throws Exception {
        when(accountService.createAccount(any())).thenReturn(account(null));

        mockMvc.perform(createRequest(BODY_WITHOUT_NICKNAME.formatted(CUSTOMER)))
                .andExpect(status().isCreated());
    }

    @Test
    void missingCustomerNameIsRejected() throws Exception {
        mockMvc.perform(createRequest("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.customerName").exists());
        verify(accountService, never()).createAccount(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"   ", "Jane123", "<script>"})
    void invalidCustomerNameIsRejected(String customerName) throws Exception {
        mockMvc.perform(createRequest(BODY_WITHOUT_NICKNAME.formatted(customerName)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.customerName").exists());
    }

    @ParameterizedTest
    @ValueSource(strings = {"abcd", "  abcd  ", "abcdefghijklmnopqrstuvwxyzabcde"})
    void nicknameOutsideFiveToThirtyCharactersIsRejected(String nickname) throws Exception {
        mockMvc.perform(createRequest(BODY.formatted(CUSTOMER, nickname)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.accountNickname").exists());
    }

    @ParameterizedTest
    @ValueSource(strings = {"abcde", "abcdefghijklmnopqrstuvwxyzabcd"})
    void nicknameAtLengthBoundariesIsAccepted(String nickname) throws Exception {
        when(accountService.createAccount(any())).thenReturn(account(nickname));

        mockMvc.perform(createRequest(BODY.formatted(CUSTOMER, nickname)))
                .andExpect(status().isCreated());
    }

    @Test
    void offensiveNicknameIsRejected() throws Exception {
        when(offensiveNicknameChecker.isOffensive(anyString())).thenReturn(true);

        mockMvc.perform(createRequest(BODY.formatted(CUSTOMER, NICKNAME)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.accountNickname", hasItem("must not contain offensive language")));
        verify(accountService, never()).createAccount(any());
    }

    @Test
    void malformedJsonIsRejected() throws Exception {
        mockMvc.perform(createRequest("{not json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void customerAtAccountLimitGets422() throws Exception {
        when(accountService.createAccount(any())).thenThrow(new CustomerAccountLimitExceededException(MAX_ACCOUNTS));

        mockMvc.perform(createRequest(BODY.formatted(CUSTOMER, NICKNAME)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void unknownAccountGets404() throws Exception {
        UUID id = UUID.randomUUID();
        when(accountService.getAccount(id)).thenThrow(new AccountNotFoundException(id));

        mockMvc.perform(get(ACCOUNT_URL, id))
                .andExpect(status().isNotFound());
    }

    @Test
    void nonUuidAccountIdGets400() throws Exception {
        mockMvc.perform(get(ACCOUNT_URL, "not-a-uuid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void databaseUnavailableGets503WithRetryAfter() throws Exception {
        UUID id = UUID.randomUUID();
        when(accountService.getAccount(id)).thenThrow(new CannotCreateTransactionException("connection refused"));

        mockMvc.perform(get(ACCOUNT_URL, id))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER));
    }

    @Test
    void genericPersistenceErrorCausedByLostConnectionGets503() throws Exception {
        // Shape seen when Postgres dies mid-transaction: the failed rollback surfaces as an uncategorised error.
        UUID id = UUID.randomUUID();
        SQLException lostConnection = new SQLException("I/O error", CONNECTION_FAILURE_SQL_STATE);
        when(accountService.getAccount(id)).thenThrow(new JpaSystemException(new RuntimeException(lostConnection)));

        mockMvc.perform(get(ACCOUNT_URL, id))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER));
    }

    @Test
    void failedRollbackOnClosedConnectionGets503() throws Exception {
        // Shape produced by Hikari + Hibernate when Postgres disappears mid-transaction.
        UUID id = UUID.randomUUID();
        SQLException closed = new SQLException("Connection is closed");
        when(accountService.getAccount(id)).thenThrow(new JpaSystemException(
                new org.hibernate.TransactionException("Unable to rollback against JDBC Connection", closed)));

        mockMvc.perform(get(ACCOUNT_URL, id))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void genericPersistenceErrorNotCausedByConnectionGets500() throws Exception {
        UUID id = UUID.randomUUID();
        SQLException badSql = new SQLException("syntax error", SYNTAX_ERROR_SQL_STATE);
        when(accountService.getAccount(id)).thenThrow(new JpaSystemException(new RuntimeException(badSql)));

        mockMvc.perform(get(ACCOUNT_URL, id))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void unexpectedErrorGets500WithoutLeakingDetails() throws Exception {
        String secret = "internal detail";
        UUID id = UUID.randomUUID();
        when(accountService.getAccount(id)).thenThrow(new IllegalStateException(secret));

        mockMvc.perform(get(ACCOUNT_URL, id))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value(not(secret)));
    }

    private static MockHttpServletRequestBuilder createRequest(String json) {
        return post(AccountController.BASE_PATH).contentType(MediaType.APPLICATION_JSON).content(json);
    }

    private static AccountResponse account(String nickname) {
        return new AccountResponse(UUID.randomUUID(), ACCOUNT_NUMBER, CUSTOMER, nickname, Instant.now());
    }
}
