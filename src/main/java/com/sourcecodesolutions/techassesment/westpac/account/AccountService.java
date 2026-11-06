package com.sourcecodesolutions.techassesment.westpac.account;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    private final AccountRepository repository;
    private final AccountProperties properties;

    AccountService(AccountRepository repository, AccountProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    /** Opens a savings account, enforcing the per-customer account limit. */
    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        String customerName = request.customerName();
        // Serialise creates per customer so the count below cannot go stale before the insert.
        repository.lockCustomer(customerName);
        if (repository.countByCustomerName(customerName) >= properties.maxPerCustomer()) {
            throw new CustomerAccountLimitExceededException(properties.maxPerCustomer());
        }
        // Flush so the database-generated account number is available for the response.
        Account account = repository.saveAndFlush(new Account(customerName, request.accountNickname()));
        log.info("Created account {}", account.getId());
        return AccountResponse.from(account);
    }

    /**
     * Cached in Redis. Caching wraps the transaction (see CachingConfiguration), so a cache hit is served
     * without touching Postgres, including while it is unavailable.
     */
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = AccountConfiguration.ACCOUNTS_CACHE, key = "#id")
    public AccountResponse getAccount(UUID id) {
        return repository.findById(id)
                .map(AccountResponse::from)
                .orElseThrow(() -> new AccountNotFoundException(id));
    }
}
