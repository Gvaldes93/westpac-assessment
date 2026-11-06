-- 10-digit account numbers, allocated by the database so they are unique without application coordination.
-- TODO: real account numbers would follow the bank's format (BSB + number + check digit).
CREATE SEQUENCE account_number_seq START WITH 1000000000 INCREMENT BY 1 MAXVALUE 9999999999;

CREATE TABLE account (
    id             UUID                     PRIMARY KEY,
    account_number BIGINT                   NOT NULL DEFAULT nextval('account_number_seq'),
    customer_name  VARCHAR(100)             NOT NULL,
    nickname       VARCHAR(30),
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_account_account_number UNIQUE (account_number),
    CONSTRAINT ck_account_nickname_length CHECK (nickname IS NULL OR char_length(nickname) BETWEEN 5 AND 30)
);

ALTER SEQUENCE account_number_seq OWNED BY account.account_number;

-- Supports the case-insensitive per-customer account count.
CREATE INDEX ix_account_customer_name_lower ON account (lower(customer_name));
