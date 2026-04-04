package hp.soft.ledger.repository;

import hp.soft.ledger.dto.Transaction;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static hp.soft.jooq.tables.Transactions.TRANSACTIONS;

@Repository
@RequiredArgsConstructor
public class TransactionRepository {

    private final DSLContext dsl;

    public Mono<Transaction> insert(UUID paymentId, String description, java.math.BigDecimal amount) {
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
}
