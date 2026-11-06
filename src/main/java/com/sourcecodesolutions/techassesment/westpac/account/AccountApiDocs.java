package com.sourcecodesolutions.techassesment.westpac.account;

/** Compile-time constants for OpenAPI annotations, which cannot reference enums such as HttpStatus. */
final class AccountApiDocs {

    static final String TAG = "Accounts";
    static final String PROBLEM_JSON = "application/problem+json";

    static final String OK = "200";
    static final String CREATED = "201";
    static final String BAD_REQUEST = "400";
    static final String NOT_FOUND = "404";
    static final String UNPROCESSABLE_CONTENT = "422";
    static final String SERVICE_UNAVAILABLE = "503";

    private AccountApiDocs() {
    }
}
