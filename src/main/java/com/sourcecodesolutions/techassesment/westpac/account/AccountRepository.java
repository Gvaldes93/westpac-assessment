package com.sourcecodesolutions.techassesment.westpac.account;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface AccountRepository extends JpaRepository<Account, UUID> {

    /** Customer identity is the case-insensitive name (backed by an index on lower(customer_name)). */
    @Query("select count(a) from Account a where lower(a.customerName) = lower(:customerName)")
    long countByCustomerName(String customerName);

    /**
     * Takes a transaction-scoped Postgres advisory lock for the customer so concurrent creates for the
     * same customer cannot both pass the account-limit check. Released automatically on commit/rollback.
     * Hash collisions only cause unnecessary serialisation, never incorrect results.
     */
    @Query(value = "SELECT 1 FROM pg_advisory_xact_lock(hashtext(lower(:customerName)))", nativeQuery = true)
    int lockCustomer(String customerName);
}
