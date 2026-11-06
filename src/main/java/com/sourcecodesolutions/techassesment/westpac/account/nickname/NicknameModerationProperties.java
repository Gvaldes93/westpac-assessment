package com.sourcecodesolutions.techassesment.westpac.account.nickname;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.validation.annotation.Validated;

/**
 * @param wordListLocation newline-separated list of offensive terms; blank lines and {@code #} comments are ignored
 */
@Validated
@ConfigurationProperties(prefix = "accounts.nickname-moderation")
record NicknameModerationProperties(@NotNull Resource wordListLocation) {
}
