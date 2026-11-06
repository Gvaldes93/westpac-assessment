package com.sourcecodesolutions.techassesment.westpac.account;

import com.sourcecodesolutions.techassesment.westpac.account.nickname.NotOffensive;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request body for opening a savings account. Id and account number are server generated, so any
 * client-supplied values for them are ignored.
 */
@Schema(description = "Request to open a savings account")
record CreateAccountRequest(
        @Schema(description = "Customer's full name; trimmed, compared case-insensitively for the account limit",
                example = "Jane Citizen")
        @NotBlank
        @Size(max = AccountConstraints.CUSTOMER_NAME_MAX_LENGTH)
        @Pattern(regexp = AccountConstraints.CUSTOMER_NAME_PATTERN,
                message = "must contain only letters, spaces, apostrophes, hyphens or periods")
        String customerName,

        @Schema(description = "Optional nickname; trimmed, blank means none, must not be offensive",
                example = "Holiday Fund")
        @Size(min = AccountConstraints.NICKNAME_MIN_LENGTH, max = AccountConstraints.NICKNAME_MAX_LENGTH)
        @NotOffensive
        String accountNickname) {

    // Normalise before validation: surrounding whitespace is not meaningful and a blank optional nickname means "none".
    CreateAccountRequest {
        customerName = customerName == null ? null : customerName.strip();
        accountNickname = accountNickname == null || accountNickname.isBlank() ? null : accountNickname.strip();
    }
}
