package hp.soft.ledger.repository;

import hp.soft.ledger.dto.AccountBalance;
import org.jooq.Record;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

import static hp.soft.jooq.tables.Accounts.ACCOUNTS;

@Component
public class LedgerLineMapper {

    public AccountBalance toAccountBalance(Record r) {
        BigDecimal totalDebits = r.get("total_debits", BigDecimal.class);
        BigDecimal totalCredits = r.get("total_credits", BigDecimal.class);

        return AccountBalance.builder()
                .code(r.get(ACCOUNTS.ACC_CODE))
                .name(r.get(ACCOUNTS.ACC_NAME))
                .type(r.get(ACCOUNTS.ACC_TYPE))
                .totalDebits(totalDebits)
                .totalCredits(totalCredits)
                .balance(totalDebits.subtract(totalCredits))
                .build();
    }
}
