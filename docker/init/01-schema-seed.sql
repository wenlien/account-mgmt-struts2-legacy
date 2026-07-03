-- account-mgmt-struts2-legacy — schema + seed（對應 entity mapping）
-- 由 MySQL 容器的 /docker-entrypoint-initdb.d 於首次初始化時自動執行。

CREATE DATABASE IF NOT EXISTS accountdb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE accountdb;

-- 對應 com.example.accountmgmt.hibernate.model.Account（@Table name="accounts"）
CREATE TABLE IF NOT EXISTS accounts (
  account_no  VARCHAR(20)   NOT NULL,
  owner_name  VARCHAR(100)  NOT NULL,
  balance     DECIMAL(19,2) NOT NULL DEFAULT 0.00,
  opened_date DATE          NULL,
  status      VARCHAR(10)   NOT NULL DEFAULT 'ACTIVE',
  PRIMARY KEY (account_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 對應 com.example.accountmgmt.hibernate.model.Transaction（@Table name="transactions"）
CREATE TABLE IF NOT EXISTS transactions (
  tx_id      BIGINT        NOT NULL AUTO_INCREMENT,
  account_no VARCHAR(20)   NOT NULL,
  type       VARCHAR(10)   NOT NULL,
  amount     DECIMAL(19,2) NOT NULL,
  created_at DATETIME      NOT NULL,
  PRIMARY KEY (tx_id),
  CONSTRAINT fk_tx_account FOREIGN KEY (account_no) REFERENCES accounts(account_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- seed 帳戶（可斷言事實：A001 餘額=1000.00、A001 交易 3 筆、共 4 個帳戶）
INSERT INTO accounts (account_no, owner_name, balance, opened_date, status) VALUES
  ('A001', 'Alice Chen', 1000.00,  '2024-01-15', 'ACTIVE'),
  ('A002', 'Bob Lin',     500.50,  '2024-03-20', 'ACTIVE'),
  ('A003', 'Carol Wu',      0.00,  '2024-06-01', 'FROZEN'),
  ('A004', 'David Ho',   12345.67, '2023-11-11', 'ACTIVE');

-- seed 交易（A001 淨額 = +1000 -200 +200 = 1000.00，與 accounts.balance 一致）
INSERT INTO transactions (account_no, type, amount, created_at) VALUES
  ('A001', 'DEPOSIT',  1000.00, '2024-01-15 09:00:00'),
  ('A001', 'WITHDRAW',  200.00, '2024-02-10 14:30:00'),
  ('A001', 'DEPOSIT',   200.00, '2024-03-05 11:00:00'),
  ('A002', 'DEPOSIT',   500.50, '2024-03-20 10:00:00'),
  ('A004', 'DEPOSIT', 12345.67, '2023-11-11 08:00:00');
