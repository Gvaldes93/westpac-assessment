package com.sourcecodesolutions.techassesment.westpac.account;

public class CustomerAccountLimitExceededException extends RuntimeException {

    // The customer name is deliberately left out of the message to keep PII out of logs and responses.
    CustomerAccountLimitExceededException(int maxAccounts) {
        super("Customer already holds the maximum of %d accounts".formatted(maxAccounts));
    }
}
