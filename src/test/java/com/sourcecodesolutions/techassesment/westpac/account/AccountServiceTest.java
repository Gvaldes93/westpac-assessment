package com.sourcecodesolutions.techassesment.westpac.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class AccountServiceTest {

    private static final int MAX_ACCOUNTS = 5;
    private static final String CUSTOMER = "Jane Citizen";
    private static final String NICKNAME = "Holiday Fund";

    private final AccountRepository repository = mock(AccountRepository.class);
    private final AccountService service =
            new AccountService(repository, new AccountProperties(MAX_ACCOUNTS, Duration.ofMinutes(1)));

    @Test
    void createsAccountWhenCustomerIsUnderTheLimit() {
        when(repository.countByCustomerName(CUSTOMER)).thenReturn((long) MAX_ACCOUNTS - 1);
        when(repository.saveAndFlush(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AccountResponse response = service.createAccount(new CreateAccountRequest(CUSTOMER, NICKNAME));

        assertThat(response.customerName()).isEqualTo(CUSTOMER);
        assertThat(response.accountNickname()).isEqualTo(NICKNAME);
    }

    @Test
    void locksCustomerBeforeCountingAccounts() {
        when(repository.saveAndFlush(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.createAccount(new CreateAccountRequest(CUSTOMER, null));

        InOrder order = inOrder(repository);
        order.verify(repository).lockCustomer(CUSTOMER);
        order.verify(repository).countByCustomerName(CUSTOMER);
        order.verify(repository).saveAndFlush(any(Account.class));
    }

    @Test
    void rejectsAccountWhenCustomerIsAtTheLimit() {
        when(repository.countByCustomerName(CUSTOMER)).thenReturn((long) MAX_ACCOUNTS);

        assertThatThrownBy(() -> service.createAccount(new CreateAccountRequest(CUSTOMER, NICKNAME)))
                .isInstanceOf(CustomerAccountLimitExceededException.class);
        verify(repository, never()).saveAndFlush(any(Account.class));
    }

    @Test
    void throwsNotFoundForUnknownAccount() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getAccount(id)).isInstanceOf(AccountNotFoundException.class);
    }
}
