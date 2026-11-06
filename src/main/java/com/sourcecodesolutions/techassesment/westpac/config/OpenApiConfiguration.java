package com.sourcecodesolutions.techassesment.westpac.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import java.util.Map;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** API-level metadata for the generated OpenAPI document. Operations and schemas come from annotations. */
@Configuration(proxyBeanMethods = false)
class OpenApiConfiguration {

    private static final String TITLE = "Savings Account API";
    private static final String VERSION = "v1";
    private static final String DESCRIPTION = "Opens and retrieves savings bank accounts. "
            + "Errors are returned as RFC 9457 problem details.";
    // Fixed server keeps the exported spec stable regardless of the host that generated it.
    private static final String LOCAL_SERVER_URL = "http://localhost:8080";
    private static final String LOCAL_SERVER_DESCRIPTION = "Local development";

    private static final String PROBLEM_DETAIL_SCHEMA = "ProblemDetail";
    // Java-side map that Jackson flattens into top-level fields; it never appears on the wire.
    private static final String PROBLEM_DETAIL_PROPERTIES_FIELD = "properties";
    private static final String ERRORS_FIELD = "errors";
    private static final String ERRORS_DESCRIPTION = "Validation messages per request field (validation failures only)";

    // TODO: declare the OAuth2 security scheme once authentication is added.
    @Bean
    OpenAPI accountsOpenApi() {
        return new OpenAPI()
                .info(new Info().title(TITLE).version(VERSION).description(DESCRIPTION))
                .servers(List.of(new Server().url(LOCAL_SERVER_URL).description(LOCAL_SERVER_DESCRIPTION)));
    }

    /** Makes the ProblemDetail schema match the JSON actually returned, including the validation `errors` map. */
    @Bean
    OpenApiCustomizer problemDetailSchemaCustomizer() {
        return openApi -> {
            Map<String, Schema> schemas = openApi.getComponents().getSchemas();
            if (schemas == null || !schemas.containsKey(PROBLEM_DETAIL_SCHEMA)) {
                return;
            }
            Schema<?> problemDetail = schemas.get(PROBLEM_DETAIL_SCHEMA);
            problemDetail.getProperties().remove(PROBLEM_DETAIL_PROPERTIES_FIELD);
            problemDetail.addProperty(ERRORS_FIELD, new ObjectSchema()
                    .description(ERRORS_DESCRIPTION)
                    .additionalProperties(new ArraySchema().items(new StringSchema())));
        };
    }
}
