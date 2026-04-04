package hp.soft.account.service;

import hp.soft.account.dto.Account;
import hp.soft.account.dto.AccountCode;
import hp.soft.account.dto.AccountType;
import hp.soft.account.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AccountServiceTest {
    private final AccountRepository accountRepository = mock(AccountRepository.class);
    private final AccountService accountService = new AccountService(accountRepository);

    @Test
    void findZilchCash_delegatesToRepository() {
        Account zilchCash = Account.builder()
                .id(UUID.randomUUID())
                .code(AccountCode.ZILCH_CASH)
                .name("Zilch Cash")
                .type(AccountType.ASSET)
                .build();

        when(accountRepository.findByCode(AccountCode.ZILCH_CASH, null, null))
                .thenReturn(Mono.just(zilchCash));

        StepVerifier.create(accountService.findZilchCash())
                .expectNext(zilchCash)
                .verifyComplete();
    }

    @Test
    void findOrCreateCustomerReceivable_returnsExistingAccount() {
        UUID customerId = UUID.randomUUID();
        Account existing = Account.builder()
                .id(UUID.randomUUID())
                .code(AccountCode.CUSTOMER_RECEIVABLE)
                .name("Customer Receivable")
                .type(AccountType.ASSET)
                .customerId(customerId)
                .build();

        when(accountRepository.findByCode(AccountCode.CUSTOMER_RECEIVABLE, customerId, null))
                .thenReturn(Mono.just(existing));

        StepVerifier.create(accountService.findOrCreateCustomerReceivable(customerId))
                .expectNext(existing)
                .verifyComplete();

        verify(accountRepository, never()).insert(any());
    }

    @Test
    void findOrCreateCustomerReceivable_createsWhenNotFound() {
        UUID customerId = UUID.randomUUID();
        Account created = Account.builder()
                .id(UUID.randomUUID())
                .code(AccountCode.CUSTOMER_RECEIVABLE)
                .name("Customer Receivable")
                .type(AccountType.ASSET)
                .customerId(customerId)
                .build();

        when(accountRepository.findByCode(AccountCode.CUSTOMER_RECEIVABLE, customerId, null))
                .thenReturn(Mono.empty());
        when(accountRepository.insert(any(Account.class)))
                .thenReturn(Mono.just(created));

        StepVerifier.create(accountService.findOrCreateCustomerReceivable(customerId))
                .expectNextMatches(account ->
                        account.code() == AccountCode.CUSTOMER_RECEIVABLE
                                && account.customerId().equals(customerId))
                .verifyComplete();

        verify(accountRepository).insert(any(Account.class));
    }

    @Test
    void findOrCreateMerchantPayable_returnsExistingAccount() {
        UUID merchantId = UUID.randomUUID();
        Account existing = Account.builder()
                .id(UUID.randomUUID())
                .code(AccountCode.MERCHANT_PAYABLE)
                .name("Merchant Payable")
                .type(AccountType.LIABILITY)
                .merchantId(merchantId)
                .build();

        when(accountRepository.findByCode(AccountCode.MERCHANT_PAYABLE, null, merchantId))
                .thenReturn(Mono.just(existing));

        StepVerifier.create(accountService.findOrCreateMerchantPayable(merchantId))
                .expectNext(existing)
                .verifyComplete();

        verify(accountRepository, never()).insert(any());
    }

    @Test
    void findOrCreateMerchantPayable_createsWhenNotFound() {
        UUID merchantId = UUID.randomUUID();
        Account created = Account.builder()
                .id(UUID.randomUUID())
                .code(AccountCode.MERCHANT_PAYABLE)
                .name("Merchant Payable")
                .type(AccountType.LIABILITY)
                .merchantId(merchantId)
                .build();

        when(accountRepository.findByCode(AccountCode.MERCHANT_PAYABLE, null, merchantId))
                .thenReturn(Mono.empty());
        when(accountRepository.insert(any(Account.class)))
                .thenReturn(Mono.just(created));

        StepVerifier.create(accountService.findOrCreateMerchantPayable(merchantId))
                .expectNextMatches(account ->
                        account.code() == AccountCode.MERCHANT_PAYABLE
                                && account.merchantId().equals(merchantId))
                .verifyComplete();

        verify(accountRepository).insert(any(Account.class));
    }
}
