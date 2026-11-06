package com.sourcecodesolutions.techassesment.westpac.account;

/** Field limits shared by the API contract and the database mapping. */
final class AccountConstraints {

    static final int CUSTOMER_NAME_MAX_LENGTH = 100;
    static final int NICKNAME_MIN_LENGTH = 5;
    static final int NICKNAME_MAX_LENGTH = 30;

    /** Letters plus the punctuation common in real names (spaces, apostrophes, hyphens, periods). */
    static final String CUSTOMER_NAME_PATTERN = "^\\p{L}[\\p{L} .'-]*$";

    private AccountConstraints() {
    }
}
