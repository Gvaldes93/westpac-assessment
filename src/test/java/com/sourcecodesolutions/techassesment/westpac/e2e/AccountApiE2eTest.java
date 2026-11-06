package com.sourcecodesolutions.techassesment.westpac.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.sourcecodesolutions.techassesment.westpac.TestcontainersConfiguration;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.client.EntityExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Black-box end-to-end tests: the packaged application on a real port, real Postgres and Redis, driven
 * over HTTP exactly as a client would. Lives in its own package so it cannot reach application internals;
 * all state is created through the API. Field-level validation and edge cases are covered by the
 * MockMvc-based tests and deliberately not repeated here.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class AccountApiE2eTest {

    // Contract values from the published API, intentionally not shared with production code.
    private static final String ACCOUNTS_PATH = "/api/v1/accounts";
    private static final String ACCOUNT_PATH = ACCOUNTS_PATH + "/{id}";
    private static final int MAX_ACCOUNTS_PER_CUSTOMER = 5;
    private static final String ACCOUNT_NUMBER_FORMAT = "\\d{10}";
    private static final String BASE_URL = "http://localhost:";
    private static final String NICKNAME = "Holiday Fund";
    // Asserted on the raw body: JSON path "does not exist" would also accept an explicit null.
    private static final String NICKNAME_FIELD = "accountNickname";
    private static final String CREATE_BODY = """
            {"customerName": "%s", "accountNickname": "%s"}""";
    private static final String CREATE_BODY_WITHOUT_NICKNAME = """
            {"customerName": "%s"}""";
    private static final String CUSTOMER_NAME_PREFIX = "Customer ";
    private static final char FIRST_LETTER = 'a';
    private static final int HEX_RADIX = 16;
    private static final String UUID_SEPARATOR = "-";
    private static final String EMPTY = "";

    @LocalServerPort
    private int port;

    private RestTestClient client;

    @BeforeEach
    void setUpClient() {
        client = RestTestClient.bindToServer().baseUrl(BASE_URL + port).build();
    }

    @Test
    void createAccountReturns201WithLocationAndGeneratedFields() {
        String customer = uniqueCustomerName();

        EntityExchangeResult<byte[]> result = client.post().uri(ACCOUNTS_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body(CREATE_BODY.formatted(customer, NICKNAME))
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.id").value(String.class, id -> assertThat(UUID.fromString(id)).isNotNull())
                .jsonPath("$.accountNumber").value(String.class, number -> assertThat(number).matches(ACCOUNT_NUMBER_FORMAT))
                .jsonPath("$.customerName").isEqualTo(customer)
                .jsonPath("$.accountNickname").isEqualTo(NICKNAME)
                .jsonPath("$.createdAt").isNotEmpty()
                .returnResult();

        URI location = result.getResponseHeaders().getLocation();
        assertThat(location).isNotNull();
        assertThat(location.toString()).isEqualTo(BASE_URL + port + ACCOUNTS_PATH + "/" + idOf(result));
    }

    @Test
    void createAccountWithoutNicknameSucceeds() {
        client.post().uri(ACCOUNTS_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body(CREATE_BODY_WITHOUT_NICKNAME.formatted(uniqueCustomerName()))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .consumeWith(response -> assertThat(new String(response.getResponseBody(), StandardCharsets.UTF_8))
                        .doesNotContain(NICKNAME_FIELD));
    }

    @Test
    void getAccountReturnsExactlyWhatWasCreated() {
        EntityExchangeResult<byte[]> created = create(uniqueCustomerName());
        String createdBody = new String(created.getResponseBody(), StandardCharsets.UTF_8);

        // Read twice: the first call loads from Postgres, the second is served from Redis.
        for (int read = 0; read < 2; read++) {
            client.get().uri(created.getResponseHeaders().getLocation())
                    .exchange()
                    .expectStatus().isOk()
                    .expectHeader().contentType(MediaType.APPLICATION_JSON)
                    .expectBody().json(createdBody, JsonCompareMode.STRICT);
        }
    }

    @Test
    void getUnknownAccountReturns404Problem() {
        client.get().uri(ACCOUNT_PATH, UUID.randomUUID())
                .exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo(HttpStatus.NOT_FOUND.value())
                .jsonPath("$.detail").isNotEmpty();
    }

    @Test
    void sixthAccountForSameCustomerIsRejectedWith422Problem() {
        String customer = uniqueCustomerName();
        for (int i = 0; i < MAX_ACCOUNTS_PER_CUSTOMER; i++) {
            create(customer);
        }

        client.post().uri(ACCOUNTS_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body(CREATE_BODY.formatted(customer, NICKNAME))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT)
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT.value())
                .jsonPath("$.detail").isNotEmpty();

        // The limit is per customer: someone else can still open an account.
        create(uniqueCustomerName());
    }

    private EntityExchangeResult<byte[]> create(String customer) {
        return client.post().uri(ACCOUNTS_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body(CREATE_BODY.formatted(customer, NICKNAME))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .returnResult();
    }

    private static String idOf(EntityExchangeResult<byte[]> result) {
        String location = result.getResponseHeaders().getLocation().toString();
        return location.substring(location.lastIndexOf('/') + 1);
    }

    /** Names may only contain letters, so map a random UUID's hex digits onto a-p. */
    private static String uniqueCustomerName() {
        StringBuilder name = new StringBuilder(CUSTOMER_NAME_PREFIX);
        for (char hex : UUID.randomUUID().toString().replace(UUID_SEPARATOR, EMPTY).toCharArray()) {
            name.append((char) (FIRST_LETTER + Character.digit(hex, HEX_RADIX)));
        }
        return name.toString();
    }
}
