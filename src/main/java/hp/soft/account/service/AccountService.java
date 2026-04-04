package hp.soft.account.service;

import hp.soft.account.dto.Account;
import hp.soft.account.dto.AccountCode;
import hp.soft.account.dto.AccountType;
import hp.soft.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    public Mono<Account> findZilchCash() {
        return accountRepository.findByCode(AccountCode.ZILCH_CASH, null, null);
    }

    public Mono<Account> findOrCreateCustomerReceivable(UUID customerId) {
        return findOrCreate(
                AccountCode.CUSTOMER_RECEIVABLE,
                AccountType.ASSET,
                "Customer Receivable",
                customerId,
                null
        );
    }

    public Mono<Account> findOrCreateMerchantPayable(UUID merchantId) {
        return findOrCreate(
                AccountCode.MERCHANT_PAYABLE,
                AccountType.LIABILITY,
                "Merchant Payable",
                null,
                merchantId
        );
    }

    private Mono<Account> findOrCreate(AccountCode code, AccountType type, String name,
                                       UUID customerId, UUID merchantId) {
        return accountRepository.findByCode(code, customerId, merchantId)
                .switchIfEmpty(Mono.defer(() -> accountRepository.insert(
                        Account.builder()
                                .id(UUID.randomUUID())
                                .code(code)
                                .name(name)
                                .type(type)
                                .customerId(customerId)
                                .merchantId(merchantId)
                                .build()
                )));
    }
}
