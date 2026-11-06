package com.sourcecodesolutions.techassesment.westpac.account;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Request helpers shared by the integration tests. */
final class AccountTestSupport {

    static final String CREATE_BODY = """
            {"customerName": "%s", "accountNickname": "%s"}""";

    private AccountTestSupport() {
    }

    static MockHttpServletRequestBuilder createRequest(String customerName, String nickname) {
        return post(AccountController.BASE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(CREATE_BODY.formatted(customerName, nickname));
    }

    /** Creates an account and returns its Location URL. */
    static String createAccount(MockMvc mockMvc, String customerName, String nickname) throws Exception {
        return mockMvc.perform(createRequest(customerName, nickname))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getHeader(HttpHeaders.LOCATION);
    }
}
