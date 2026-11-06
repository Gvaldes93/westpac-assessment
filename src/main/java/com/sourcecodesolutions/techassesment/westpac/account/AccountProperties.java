package com.sourcecodesolutions.techassesment.westpac.account;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Business settings for accounts, bound from the {@code accounts.*} properties.
 *
 * @param maxPerCustomer maximum number of accounts a single customer may hold
 * @param cacheTtl       how long GET responses stay in the Redis cache
 */
@Validated
@ConfigurationProperties(prefix = "accounts")
record AccountProperties(
        @Positive int maxPerCustomer,
        @NotNull Duration cacheTtl) {
}
