package hp.soft.ledger.repository;

import hp.soft.ledger.dto.Transaction;
import hp.soft.payment.dto.PaymentDetail;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static hp.soft.jooq.tables.Accounts.ACCOUNTS;
import static hp.soft.jooq.tables.LedgerLines.LEDGER_LINES;
import static hp.soft.jooq.tables.Transactions.TRANSACTIONS;

@Repository
@RequiredArgsConstructor
public class TransactionRepository {
    private final DSLContext dsl;
    private final TransactionMapper transactionMapper;

    public Mono<Transaction> insert(UUID paymentId, String description, BigDecimal amount) {
        UUID id = UUID.randomUUID();
        return Mono.from(
                dsl.insertInto(TRANSACTIONS)
                        .set(TRANSACTIONS.TX_ID, id)
                        .set(TRANSACTIONS.TX_DESCRIPTION, description)
                        .set(TRANSACTIONS.TX_PM_ID, paymentId)
                        .set(TRANSACTIONS.TX_AMOUNT, amount)
        ).thenReturn(Transaction.builder()
                .id(id)
                .description(description)
                .paymentId(paymentId)
                .amount(amount)
                .build());
    }

    public Mono<List<PaymentDetail.TransactionDetail>> findDetailsByPaymentId(UUID paymentId) {
        return Flux.from(
                dsl.select(
                                TRANSACTIONS.TX_ID,
                                TRANSACTIONS.TX_DESCRIPTION,
                                TRANSACTIONS.TX_AMOUNT,
                                TRANSACTIONS.TX_CREATED_AT,
                                LEDGER_LINES.LL_DEBIT,
                                LEDGER_LINES.LL_CREDIT,
                                ACCOUNTS.ACC_CODE,
                                ACCOUNTS.ACC_NAME
                        )
                        .from(TRANSACTIONS)
                        .join(LEDGER_LINES).on(TRANSACTIONS.TX_ID.eq(LEDGER_LINES.LL_TX_ID))
                        .join(ACCOUNTS).on(LEDGER_LINES.LL_ACC_ID.eq(ACCOUNTS.ACC_ID))
                        .where(TRANSACTIONS.TX_PM_ID.eq(paymentId))
                        .orderBy(TRANSACTIONS.TX_CREATED_AT, ACCOUNTS.ACC_CODE)
        ).collectList()
                .map(transactionMapper::toTransactionDetails);
    }
}
