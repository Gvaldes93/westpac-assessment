package com.sourcecodesolutions.techassesment.westpac;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Keeps the committed spec in sync with the code. On drift, regenerate with
 * {@code ./gradlew test --tests '*OpenApiSpecTest' -PupdateOpenApiSpec=true} and review the diff.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OpenApiSpecTest {

    private static final String SPEC_URL = "/v3/api-docs.yaml";
    private static final Path COMMITTED_SPEC = Path.of("docs", "openapi.yaml");
    private static final String UPDATE_PROPERTY = "openapi.spec.update";
    private static final String SWAGGER_UI_URL = "/swagger-ui/index.html";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void committedSpecMatchesGeneratedSpec() throws Exception {
        String generated = mockMvc.perform(get(SPEC_URL))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        if (Boolean.getBoolean(UPDATE_PROPERTY)) {
            Files.createDirectories(COMMITTED_SPEC.getParent());
            Files.writeString(COMMITTED_SPEC, generated, StandardCharsets.UTF_8);
        }

        assertThat(COMMITTED_SPEC).exists();
        assertThat(Files.readString(COMMITTED_SPEC, StandardCharsets.UTF_8))
                .as("docs/openapi.yaml is out of date; regenerate with -PupdateOpenApiSpec=true")
                .isEqualTo(generated);
    }

    @Test
    void swaggerUiIsServed() throws Exception {
        mockMvc.perform(get(SWAGGER_UI_URL)).andExpect(status().isOk());
    }
}
