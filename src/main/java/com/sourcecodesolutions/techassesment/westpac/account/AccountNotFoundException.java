package com.sourcecodesolutions.techassesment.westpac.account;

import java.util.UUID;

public class AccountNotFoundException extends RuntimeException {

    AccountNotFoundException(UUID id) {
        super("Account %s was not found".formatted(id));
    }
}
