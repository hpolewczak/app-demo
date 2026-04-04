package hp.soft.ledger.service;

import hp.soft.ledger.dto.TxContext;
import hp.soft.ledger.repository.LedgerLineRepository;
import hp.soft.ledger.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final LedgerLineRepository ledgerLineRepository;

    public Mono<Void> createTransaction(TxContext ctx) {
        return transactionRepository
                .insert(ctx.paymentId(), ctx.description(), ctx.amount())
                .flatMap(tx ->
                        ledgerLineRepository.insert(tx.id(), ctx.debitAccount().id(), ctx.amount(), null)
                                .then(ledgerLineRepository.insert(tx.id(), ctx.creditAccount().id(), null, ctx.amount()))
                );
    }
}
