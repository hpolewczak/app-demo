package hp.soft.account.repository;

import hp.soft.account.dto.Account;
import hp.soft.account.dto.AccountCode;
import hp.soft.account.dto.AccountType;
import hp.soft.jooq.tables.records.AccountsRecord;
import org.springframework.stereotype.Service;

@Service
public class AccountMapper {

    public Account toAccount(AccountsRecord r) {
        return Account.builder()
                .id(r.getAccId())
                .code(AccountCode.valueOf(r.getAccCode()))
                .name(r.getAccName())
                .type(AccountType.valueOf(r.getAccType()))
                .customerId(r.getAccCustomerId())
                .merchantId(r.getAccMerchantId())
                .build();
    }
}
