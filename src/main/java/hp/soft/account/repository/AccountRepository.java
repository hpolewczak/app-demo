package hp.soft.account.repository;

import hp.soft.account.dto.Account;
import hp.soft.account.dto.AccountCode;
import hp.soft.account.dto.AccountType;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static hp.soft.jooq.tables.Accounts.ACCOUNTS;

@Repository
@RequiredArgsConstructor
public class AccountRepository {
    private final DSLContext dsl;

    public Mono<Account> findByCode(AccountCode code, UUID customerId, UUID merchantId) {
        return Mono.from(
                dsl.selectFrom(ACCOUNTS)
                        .where(ACCOUNTS.ACC_CODE.eq(code.name()))
                        .and(customerId != null
                                ? ACCOUNTS.ACC_CUSTOMER_ID.eq(customerId)
                                : ACCOUNTS.ACC_CUSTOMER_ID.isNull())
                        .and(merchantId != null
                                ? ACCOUNTS.ACC_MERCHANT_ID.eq(merchantId)
                                : ACCOUNTS.ACC_MERCHANT_ID.isNull())
        ).map(this::toAccount);
    }

    public Mono<Account> insert(Account account) {
        return Mono.from(
                dsl.insertInto(ACCOUNTS)
                        .set(ACCOUNTS.ACC_ID, account.id())
                        .set(ACCOUNTS.ACC_CODE, account.code().name())
                        .set(ACCOUNTS.ACC_NAME, account.name())
                        .set(ACCOUNTS.ACC_TYPE, account.type().name())
                        .set(ACCOUNTS.ACC_CUSTOMER_ID, account.customerId())
                        .set(ACCOUNTS.ACC_MERCHANT_ID, account.merchantId())
                        .onConflict(ACCOUNTS.ACC_CODE, ACCOUNTS.ACC_CUSTOMER_ID, ACCOUNTS.ACC_MERCHANT_ID)
                        .doNothing()
        ).then(findByCode(account.code(), account.customerId(), account.merchantId()));
    }

    private Account toAccount(hp.soft.jooq.tables.records.AccountsRecord r) {
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
