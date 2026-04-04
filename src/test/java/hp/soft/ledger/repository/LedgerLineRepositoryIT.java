package hp.soft.ledger.repository;

import hp.soft.BaseIntegrationTestIT;
import hp.soft.account.dto.AccountCode;
import hp.soft.account.repository.AccountRepository;
import hp.soft.account.dto.Account;
import hp.soft.account.dto.AccountType;
import hp.soft.ledger.dto.Transaction;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.UUID;

import static hp.soft.jooq.tables.Payments.PAYMENTS;
import static org.junit.jupiter.api.Assertions.*;

class LedgerLineRepositoryIT extends BaseIntegrationTestIT {
    @Autowired
    private LedgerLineRepository ledgerLineRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private DSLContext dsl;

    @Test
    void insert_createsLedgerLineWithDebit() {
        UUID accountId = createAccount();
        UUID txId = createTransaction();

        StepVerifier.create(ledgerLineRepository.insert(txId, accountId, new BigDecimal("100.00"), null))
                .verifyComplete();
    }

    @Test
    void insert_createsLedgerLineWithCredit() {
        UUID accountId = createAccount();
        UUID txId = createTransaction();

        StepVerifier.create(ledgerLineRepository.insert(txId, accountId, null, new BigDecimal("100.00")))
                .verifyComplete();
    }

    @Test
    void findAccountBalances_aggregatesDebitsAndCredits() {
        UUID account1Id = createAccount();
        UUID account2Id = createAccount();
        UUID txId = createTransaction();

        // Debit account1, credit account2
        ledgerLineRepository.insert(txId, account1Id, new BigDecimal("300.00"), null).block();
        ledgerLineRepository.insert(txId, account2Id, null, new BigDecimal("300.00")).block();

        StepVerifier.create(ledgerLineRepository.findAccountBalances()
                        .filter(ab -> ab.totalDebits().compareTo(BigDecimal.ZERO) != 0
                                || ab.totalCredits().compareTo(BigDecimal.ZERO) != 0)
                        .collectList())
                .assertNext(balances -> {
                    assertTrue(balances.size() >= 2);

                    var debitAccount = balances.stream()
                            .filter(b -> b.totalDebits().compareTo(new BigDecimal("300.0000")) == 0)
                            .findFirst().orElseThrow();
                    assertEquals(0, debitAccount.totalCredits().compareTo(BigDecimal.ZERO));
                    assertEquals(0, debitAccount.balance().compareTo(new BigDecimal("300.0000")));

                    var creditAccount = balances.stream()
                            .filter(b -> b.totalCredits().compareTo(new BigDecimal("300.0000")) == 0)
                            .findFirst().orElseThrow();
                    assertEquals(0, creditAccount.totalDebits().compareTo(BigDecimal.ZERO));
                    assertEquals(0, creditAccount.balance().compareTo(new BigDecimal("-300.0000")));
                })
                .verifyComplete();
    }

    @Test
    void findAccountBalances_returnsZeroForAccountsWithNoLines() {
        // ZILCH_CASH is seeded by V2 migration and has no ledger lines initially
        StepVerifier.create(ledgerLineRepository.findAccountBalances()
                        .filter(ab -> ab.code().equals("ZILCH_CASH"))
                )
                .assertNext(ab -> {
                    assertEquals(0, ab.totalDebits().compareTo(BigDecimal.ZERO));
                    assertEquals(0, ab.totalCredits().compareTo(BigDecimal.ZERO));
                    assertEquals(0, ab.balance().compareTo(BigDecimal.ZERO));
                })
                .verifyComplete();
    }

    private UUID createAccount() {
        UUID id = UUID.randomUUID();
        Account account = Account.builder()
                .id(id)
                .code(AccountCode.CUSTOMER_RECEIVABLE)
                .name("Test Account " + id)
                .type(AccountType.ASSET)
                .customerId(UUID.randomUUID())
                .build();
        accountRepository.insert(account).block();
        return id;
    }

    private UUID createTransaction() {
        UUID paymentId = UUID.randomUUID();
        reactor.core.publisher.Mono.from(
                dsl.insertInto(PAYMENTS)
                        .set(PAYMENTS.PM_ID, paymentId)
                        .set(PAYMENTS.PM_CUSTOMER_ID, UUID.randomUUID())
                        .set(PAYMENTS.PM_MERCHANT_ID, UUID.randomUUID())
                        .set(PAYMENTS.PM_AMOUNT, new BigDecimal("300.00"))
                        .set(PAYMENTS.PM_STATUS, "CREATED")
        ).block();

        Transaction tx = transactionRepository.insert(paymentId, "Test", new BigDecimal("300.00")).block();
        return tx.id();
    }
}
