package com.sourcecodesolutions.techassesment.westpac.account;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

/** Savings account row. Never leaves the service layer; map to {@link AccountResponse} instead. */
@Entity
@Table(name = "account")
class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Assigned by the database sequence default and read back after insert.
    @Generated(event = EventType.INSERT)
    @Column(name = "account_number", nullable = false, unique = true, insertable = false, updatable = false)
    private Long accountNumber;

    @Column(name = "customer_name", nullable = false, length = AccountConstraints.CUSTOMER_NAME_MAX_LENGTH)
    private String customerName;

    @Column(name = "nickname", length = AccountConstraints.NICKNAME_MAX_LENGTH)
    private String nickname;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Account() {
        // Required by JPA.
    }

    Account(String customerName, String nickname) {
        this.customerName = customerName;
        this.nickname = nickname;
    }

    UUID getId() {
        return id;
    }

    Long getAccountNumber() {
        return accountNumber;
    }

    String getCustomerName() {
        return customerName;
    }

    String getNickname() {
        return nickname;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}
