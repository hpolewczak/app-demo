CREATE INDEX idx_transactions_pm_id ON transactions(tx_pm_id);
CREATE INDEX idx_ledger_lines_tx_id ON ledger_lines(ll_tx_id);
CREATE INDEX idx_ledger_lines_acc_id ON ledger_lines(ll_acc_id);
