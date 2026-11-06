package com.sourcecodesolutions.techassesment.westpac.account;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.time.Instant;
import java.util.UUID;

/** Public representation of a savings account. Also the value stored in the Redis cache. */
@Schema(description = "A savings account")
record AccountResponse(
        @Schema(description = "Generated account id", requiredMode = RequiredMode.REQUIRED)
        UUID id,
        @Schema(description = "Generated 10-digit account number", example = "1000000000",
                requiredMode = RequiredMode.REQUIRED)
        String accountNumber,
        @Schema(example = "Jane Citizen", requiredMode = RequiredMode.REQUIRED)
        String customerName,
        @Schema(description = "Absent when no nickname was given", example = "Holiday Fund")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String accountNickname,
        @Schema(description = "When the account was opened (UTC)", requiredMode = RequiredMode.REQUIRED)
        Instant createdAt) {

    static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                String.valueOf(account.getAccountNumber()),
                account.getCustomerName(),
                account.getNickname(),
                account.getCreatedAt());
    }
}
