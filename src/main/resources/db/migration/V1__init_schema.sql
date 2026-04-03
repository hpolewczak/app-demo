CREATE TABLE accounts (
    acc_id UUID PRIMARY KEY,
    acc_code VARCHAR(50) NOT NULL,
    acc_name VARCHAR(255) NOT NULL,
    acc_type VARCHAR(20) NOT NULL,
    acc_customer_id UUID,
    acc_merchant_id UUID,
    UNIQUE(acc_code, acc_customer_id, acc_merchant_id),
    CONSTRAINT chk_account_type CHECK (acc_type IN ('ASSET', 'LIABILITY'))
);

CREATE TABLE payments (
    pm_id UUID PRIMARY KEY,
    pm_customer_id UUID NOT NULL,
    pm_merchant_id UUID NOT NULL,
    pm_amount NUMERIC(19,4) NOT NULL,
    pm_status VARCHAR(20) NOT NULL,
    pm_idempotency_key VARCHAR(255) UNIQUE,
    pm_created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE transactions (
    tx_id UUID PRIMARY KEY,
    tx_description VARCHAR(255) NOT NULL,
    tx_pm_id UUID NOT NULL REFERENCES payments(pm_id),
    tx_amount NUMERIC(19,4) NOT NULL,
    tx_created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE ledger_lines (
    ll_id UUID PRIMARY KEY,
    ll_tx_id UUID NOT NULL REFERENCES transactions(tx_id),
    ll_acc_id UUID NOT NULL REFERENCES accounts(acc_id),
    ll_debit NUMERIC(19,4),
    ll_credit NUMERIC(19,4),
    ll_created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_debit_or_credit CHECK (
        (ll_debit IS NOT NULL AND ll_credit IS NULL) OR
        (ll_debit IS NULL AND ll_credit IS NOT NULL)
    )
);

