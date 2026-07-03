-- ============================================================
-- Golden sample data — Reset to Factory
-- ------------------------------------------------------------
-- 將 accountdb 還原成「出廠 golden 狀態」。可重複執行（idempotent）：
-- 會確保 schema 存在 → 清空既有資料 → 重新灌入 golden 資料。
--
-- 套用方式：
--   ./run.sh db-reset
--   或手動：docker exec -i accountdb-mysql \
--             mysql -uaccountuser -p<DB_PASSWORD> accountdb < docker/golden/reset-to-factory.sql
--
-- ⚠️ 這份 golden 資料需與 docker/init/01-schema-seed.sql 的 seed 保持一致
--    （init 只在容器首次初始化時自動跑；本檔用於之後任意時間的 factory reset）。
--
-- 可斷言事實（golden baseline）：
--   - 帳戶共 4 筆：A001 / A002 / A003 / A004
--   - A001 餘額 = 1000.00、狀態 ACTIVE、交易 3 筆（DEPOSIT 1000 / WITHDRAW 200 / DEPOSIT 200）
--   - A003 狀態 FROZEN、餘額 0.00
--   - 交易共 5 筆，tx_id 自 1 起（reset 後自增歸零）
-- ============================================================

CREATE DATABASE IF NOT EXISTS accountdb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE accountdb;

-- 確保 schema 存在（對齊 entity mapping；首次在乾淨 DB 執行時也可用）
CREATE TABLE IF NOT EXISTS accounts (
  account_no  VARCHAR(20)   NOT NULL,
  owner_name  VARCHAR(100)  NOT NULL,
  balance     DECIMAL(19,2) NOT NULL DEFAULT 0.00,
  opened_date DATE          NULL,
  status      VARCHAR(10)   NOT NULL DEFAULT 'ACTIVE',
  PRIMARY KEY (account_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS transactions (
  tx_id      BIGINT        NOT NULL AUTO_INCREMENT,
  account_no VARCHAR(20)   NOT NULL,
  type       VARCHAR(10)   NOT NULL,
  amount     DECIMAL(19,2) NOT NULL,
  created_at DATETIME      NOT NULL,
  PRIMARY KEY (tx_id),
  CONSTRAINT fk_tx_account FOREIGN KEY (account_no) REFERENCES accounts(account_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 清空既有資料（FK 安全：先關檢查，用 DELETE，再重置自增）。
-- 用 DELETE 而非 TRUNCATE：MySQL 不允許 TRUNCATE 被 FK 參照的父表。
SET FOREIGN_KEY_CHECKS = 0;
DELETE FROM transactions;
DELETE FROM accounts;
ALTER TABLE transactions AUTO_INCREMENT = 1;
SET FOREIGN_KEY_CHECKS = 1;

-- ---- golden 帳戶 ----
INSERT INTO accounts (account_no, owner_name, balance, opened_date, status) VALUES
  ('A001', 'Alice Chen', 1000.00,  '2024-01-15', 'ACTIVE'),
  ('A002', 'Bob Lin',     500.50,  '2024-03-20', 'ACTIVE'),
  ('A003', 'Carol Wu',      0.00,  '2024-06-01', 'FROZEN'),
  ('A004', 'David Ho',   12345.67, '2023-11-11', 'ACTIVE');

-- ---- golden 交易（A001 淨額 = +1000 -200 +200 = 1000.00，與 balance 一致） ----
INSERT INTO transactions (account_no, type, amount, created_at) VALUES
  ('A001', 'DEPOSIT',  1000.00, '2024-01-15 09:00:00'),
  ('A001', 'WITHDRAW',  200.00, '2024-02-10 14:30:00'),
  ('A001', 'DEPOSIT',   200.00, '2024-03-05 11:00:00'),
  ('A002', 'DEPOSIT',   500.50, '2024-03-20 10:00:00'),
  ('A004', 'DEPOSIT', 12345.67, '2023-11-11 08:00:00');
