package hp.soft.ledger.repository;

import hp.soft.payment.dto.PaymentDetail;
import org.jooq.Record;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static hp.soft.jooq.tables.Accounts.ACCOUNTS;
import static hp.soft.jooq.tables.LedgerLines.LEDGER_LINES;
import static hp.soft.jooq.tables.Transactions.TRANSACTIONS;

@Service
public class TransactionMapper {

    public List<PaymentDetail.TransactionDetail> toTransactionDetails(List<? extends Record> rows) {
        Map<UUID, List<Record>> grouped = rows.stream()
                .collect(Collectors.groupingBy(
                        r -> r.get(TRANSACTIONS.TX_ID),
                        java.util.LinkedHashMap::new,
                        Collectors.toList()
                ));

        return grouped.entrySet().stream()
                .map(entry -> {
                    Record first = entry.getValue().getFirst();
                    List<PaymentDetail.LedgerLineDetail> lines = entry.getValue().stream()
                            .map(r -> PaymentDetail.LedgerLineDetail.builder()
                                    .accountCode(r.get(ACCOUNTS.ACC_CODE))
                                    .accountName(r.get(ACCOUNTS.ACC_NAME))
                                    .debit(r.get(LEDGER_LINES.LL_DEBIT))
                                    .credit(r.get(LEDGER_LINES.LL_CREDIT))
                                    .build())
                            .toList();

                    return PaymentDetail.TransactionDetail.builder()
                            .id(first.get(TRANSACTIONS.TX_ID))
                            .description(first.get(TRANSACTIONS.TX_DESCRIPTION))
                            .amount(first.get(TRANSACTIONS.TX_AMOUNT))
                            .createdAt(first.get(TRANSACTIONS.TX_CREATED_AT))
                            .lines(lines)
                            .build();
                })
                .toList();
    }
}
