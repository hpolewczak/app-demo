package hp.soft.ledger.repository;

import hp.soft.ledger.dto.AccountBalance;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

import static hp.soft.jooq.tables.Accounts.ACCOUNTS;
import static hp.soft.jooq.tables.LedgerLines.LEDGER_LINES;
import static org.jooq.impl.DSL.coalesce;
import static org.jooq.impl.DSL.sum;

@Repository
@RequiredArgsConstructor
public class LedgerLineRepository {
    private final DSLContext dsl;

    public Mono<Void> insert(UUID transactionId, UUID accountId, BigDecimal debit, BigDecimal credit) {
        UUID id = UUID.randomUUID();
        return Mono.from(
                dsl.insertInto(LEDGER_LINES)
                        .set(LEDGER_LINES.LL_ID, id)
                        .set(LEDGER_LINES.LL_TX_ID, transactionId)
                        .set(LEDGER_LINES.LL_ACC_ID, accountId)
                        .set(LEDGER_LINES.LL_DEBIT, debit)
                        .set(LEDGER_LINES.LL_CREDIT, credit)
        ).then();
    }

    public Flux<AccountBalance> findAccountBalances() {
        return Flux.from(
                dsl.select(
                            ACCOUNTS.ACC_CODE,
                            ACCOUNTS.ACC_NAME,
                            ACCOUNTS.ACC_TYPE,
                            coalesce(sum(LEDGER_LINES.LL_DEBIT), BigDecimal.ZERO).as("total_debits"),
                            coalesce(sum(LEDGER_LINES.LL_CREDIT), BigDecimal.ZERO).as("total_credits")
                        )
                        .from(ACCOUNTS)
                        .leftJoin(LEDGER_LINES).on(ACCOUNTS.ACC_ID.eq(LEDGER_LINES.LL_ACC_ID))
                        .groupBy(ACCOUNTS.ACC_ID, ACCOUNTS.ACC_CODE, ACCOUNTS.ACC_NAME, ACCOUNTS.ACC_TYPE)
        ).map(r -> AccountBalance.builder()
                .code(r.get(ACCOUNTS.ACC_CODE))
                .name(r.get(ACCOUNTS.ACC_NAME))
                .type(r.get(ACCOUNTS.ACC_TYPE))
                .totalDebits(r.get("total_debits", BigDecimal.class))
                .totalCredits(r.get("total_credits", BigDecimal.class))
                .balance(r.get("total_debits", BigDecimal.class).subtract(r.get("total_credits", BigDecimal.class)))
                .build());
    }
}
