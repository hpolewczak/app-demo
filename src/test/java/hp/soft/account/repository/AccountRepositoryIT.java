package hp.soft.account.repository;

import hp.soft.BaseIntegrationTestIT;
import hp.soft.account.dto.Account;
import hp.soft.account.dto.AccountCode;
import hp.soft.account.dto.AccountType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

class AccountRepositoryIT extends BaseIntegrationTestIT {
    @Autowired
    private AccountRepository accountRepository;

    @Test
    void findByCode_returnsAccountWhenExists() {
        StepVerifier.create(accountRepository.findByCode(AccountCode.ZILCH_CASH, null, null))
                .expectNextMatches(account ->
                        account.code() == AccountCode.ZILCH_CASH
                                && account.type() == AccountType.ASSET
                                && account.customerId() == null
                                && account.merchantId() == null)
                .verifyComplete();
    }

    @Test
    void findByCode_returnsEmptyWhenNotFound() {
        UUID unknownCustomerId = UUID.randomUUID();
        StepVerifier.create(accountRepository.findByCode(AccountCode.CUSTOMER_RECEIVABLE, unknownCustomerId, null))
                .verifyComplete();
    }

    @Test
    void insert_createsNewAccount() {
        UUID customerId = UUID.randomUUID();
        Account newAccount = Account.builder()
                .id(UUID.randomUUID())
                .code(AccountCode.CUSTOMER_RECEIVABLE)
                .name("Customer Receivable")
                .type(AccountType.ASSET)
                .customerId(customerId)
                .merchantId(null)
                .build();

        StepVerifier.create(accountRepository.insert(newAccount))
                .expectNextMatches(account ->
                        account.code() == AccountCode.CUSTOMER_RECEIVABLE
                                && account.customerId().equals(customerId))
                .verifyComplete();
    }

    @Test
    void insert_onConflictReturnsExisting() {
        UUID merchantId = UUID.randomUUID();
        Account first = Account.builder()
                .id(UUID.randomUUID())
                .code(AccountCode.MERCHANT_PAYABLE)
                .name("Merchant Payable")
                .type(AccountType.LIABILITY)
                .customerId(null)
                .merchantId(merchantId)
                .build();

        Account duplicated = Account.builder()
                .id(UUID.randomUUID())
                .code(AccountCode.MERCHANT_PAYABLE)
                .name("Merchant Payable")
                .type(AccountType.LIABILITY)
                .customerId(null)
                .merchantId(merchantId)
                .build();

        Mono<Account> insertTwice = accountRepository.insert(first)
                .flatMap(existing -> accountRepository.insert(duplicated));

        StepVerifier.create(insertTwice)
                .expectNextMatches(account ->
                        account.id().equals(first.id())
                                && account.merchantId().equals(merchantId))
                .verifyComplete();
    }

    @Test
    void insert_concurrentInsertsReturnSameAccount() {
        UUID customerId = UUID.randomUUID();
        Account account1 = Account.builder()
                .id(UUID.randomUUID())
                .code(AccountCode.CUSTOMER_RECEIVABLE)
                .name("Customer Receivable")
                .type(AccountType.ASSET)
                .customerId(customerId)
                .merchantId(null)
                .build();

        Account account2 = Account.builder()
                .id(UUID.randomUUID())
                .code(AccountCode.CUSTOMER_RECEIVABLE)
                .name("Customer Receivable")
                .type(AccountType.ASSET)
                .customerId(customerId)
                .merchantId(null)
                .build();

        Mono<UUID[]> concurrentInserts = Mono.zip(
                accountRepository.insert(account1),
                accountRepository.insert(account2)
        ).map(tuple -> new UUID[]{tuple.getT1().id(), tuple.getT2().id()});

        StepVerifier.create(concurrentInserts)
                .expectNextMatches(ids -> ids[0].equals(ids[1]))
                .verifyComplete();
    }
}
