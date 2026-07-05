-- account-mgmt-struts2-legacy — schema + seed（對應 entity mapping）
-- 由 MySQL 容器的 /docker-entrypoint-initdb.d 於首次初始化時自動執行。
-- ⚠️ 需與 docker/golden/reset-to-factory.sql 保持一致。
-- 時間全在 2026（item4）；每帳戶含 phone/address（item3）；電話為身分識別（item5）。
-- id 與時間同序（item5）：transactions / audit_log 皆以單一 INSERT 依 created_at 升序。

CREATE DATABASE IF NOT EXISTS accountdb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE accountdb;

CREATE TABLE IF NOT EXISTS users (
  username      VARCHAR(50)  NOT NULL,
  password_hash VARCHAR(100) NOT NULL,
  role          VARCHAR(20)  NOT NULL DEFAULT 'ROLE_USER',
  display_name  VARCHAR(100) NULL,
  created_at    DATETIME     NOT NULL,
  PRIMARY KEY (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS accounts (
  account_no     VARCHAR(20)   NOT NULL,
  owner_name     VARCHAR(100)  NOT NULL,
  phone          VARCHAR(20)   NULL,
  address        VARCHAR(255)  NULL,
  balance        DECIMAL(19,2) NOT NULL DEFAULT 0.00,
  opened_date    DATE          NULL,
  status         VARCHAR(10)   NOT NULL DEFAULT 'ACTIVATED',
  closed_date    DATE          NULL,
  close_note     VARCHAR(255)  NULL,
  owner_username VARCHAR(50)   NULL,
  PRIMARY KEY (account_no),
  CONSTRAINT fk_acct_owner FOREIGN KEY (owner_username) REFERENCES users(username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS transactions (
  tx_id              BIGINT        NOT NULL AUTO_INCREMENT,
  account_no         VARCHAR(20)   NOT NULL,
  type               VARCHAR(10)   NOT NULL,
  amount             DECIMAL(19,2) NOT NULL,
  balance_after      DECIMAL(19,2) NULL,
  target_account_no  VARCHAR(20)   NULL,
  from_status        VARCHAR(10)   NULL,
  to_status          VARCHAR(10)   NULL,
  created_at         DATETIME      NOT NULL,
  note               VARCHAR(255)  NULL,
  PRIMARY KEY (tx_id),
  CONSTRAINT fk_tx_account FOREIGN KEY (account_no) REFERENCES accounts(account_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS audit_log (
  id                BIGINT        NOT NULL AUTO_INCREMENT,
  actor             VARCHAR(50)   NOT NULL,
  action            VARCHAR(30)   NOT NULL,
  target_account_no VARCHAR(20)   NULL,
  success           TINYINT(1)    NOT NULL DEFAULT 1,
  error_code        VARCHAR(10)   NULL,
  detail            VARCHAR(500)  NULL,
  created_at        DATETIME      NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO users (username, password_hash, role, display_name, created_at) VALUES
  ('alice', '$2a$10$uokIAIzB/lawPgsZ3SOxOu9C895lvt2HdUga/GRHeKwXYZprXGr8y', 'ROLE_USER', 'Alice Chen', '2026-02-15 08:58:00'),
  ('bob',   '$2a$10$uokIAIzB/lawPgsZ3SOxOu9C895lvt2HdUga/GRHeKwXYZprXGr8y', 'ROLE_USER', 'Bob Lin',    '2026-04-20 09:58:00'),
  ('carol', '$2a$10$uokIAIzB/lawPgsZ3SOxOu9C895lvt2HdUga/GRHeKwXYZprXGr8y', 'ROLE_USER', 'Carol Wu',   '2026-06-18 08:58:00'),
  ('david', '$2a$10$uokIAIzB/lawPgsZ3SOxOu9C895lvt2HdUga/GRHeKwXYZprXGr8y', 'ROLE_USER', 'David Ho',   '2026-01-11 07:58:00'),
  ('eve',   '$2a$10$uokIAIzB/lawPgsZ3SOxOu9C895lvt2HdUga/GRHeKwXYZprXGr8y', 'ROLE_USER', 'Eve Wang',   '2026-06-02 08:58:00'),
  ('frank', '$2a$10$uokIAIzB/lawPgsZ3SOxOu9C895lvt2HdUga/GRHeKwXYZprXGr8y', 'ROLE_USER', 'Frank Wu',   '2026-06-24 08:58:00');

INSERT INTO accounts (account_no, owner_name, phone, address, balance, opened_date, status, closed_date, close_note, owner_username) VALUES
  ('A001', 'Alice Chen', '0911-111-111', '1 Market St, Taipei',     850.00, '2026-02-15', 'ACTIVATED', NULL,         NULL,                         'alice'),
  ('A002', 'Bob Lin',    '0922-222-222', '2 Bank Rd, Taipei',       950.50, '2026-04-20', 'ACTIVATED', NULL,         NULL,                         'bob'),
  ('A003', 'Carol Wu',   '0933-333-333', '3 Finance Ave, Taichung',   0.00, '2026-06-18', 'FROZEN',    NULL,         NULL,                         'carol'),
  ('A004', 'David Ho',   '0944-444-444', '4 Commerce Blvd, Kaohsiung', 12345.67, '2026-01-11', 'ACTIVATED', NULL,    NULL,                         'david'),
  ('A005', 'Eve Wang',   '0955-555-555', '5 Capital Ln, Hsinchu',   2200.00, '2026-06-02', 'ACTIVATED', NULL,         NULL,                         'eve'),
  ('A006', 'Frank Wu',   '0966-666-666', '6 Ledger St, Tainan',        0.00, '2026-06-24', 'CLOSED',    '2026-06-28', 'Customer requested closure', 'frank'),
  ('B001', 'Alice Chen', '0911-111-111', '1 Market St, Taipei',     4700.00, '2026-03-01', 'ACTIVATED', NULL,         NULL,                         'alice'),
  ('B002', 'Bob Lin',    '0922-222-222', '2 Bank Rd, Taipei',       2300.00, '2026-05-10', 'ACTIVATED', NULL,         NULL,                         'bob'),
  ('B003', 'Eve Wang',   '0955-555-555', '5 Capital Ln, Hsinchu',   1000.00, '2026-06-02', 'ACTIVATED', NULL,         NULL,                         'eve');

INSERT INTO transactions (account_no, type, amount, balance_after, target_account_no, from_status, to_status, created_at, note) VALUES
  ('A004', 'STATUS',      0.00,     NULL, NULL,   NULL,        'ACTIVATED', '2026-01-11 07:59:00', 'INITIATING -> ACTIVATED: Account opened'),
  ('A004', 'DEPOSIT', 12345.67, 12345.67, NULL,   NULL,         NULL,       '2026-01-11 08:00:00', NULL),
  ('A001', 'STATUS',      0.00,     NULL, NULL,   NULL,        'ACTIVATED', '2026-02-15 08:59:00', 'INITIATING -> ACTIVATED: Account opened'),
  ('A001', 'DEPOSIT',  1000.00,  1000.00, NULL,   NULL,         NULL,       '2026-02-15 09:00:00', NULL),
  ('B001', 'STATUS',      0.00,     NULL, NULL,   NULL,        'ACTIVATED', '2026-03-01 08:59:00', 'INITIATING -> ACTIVATED: Account opened'),
  ('B001', 'DEPOSIT',  5000.00,  5000.00, NULL,   NULL,         NULL,       '2026-03-01 09:00:00', NULL),
  ('A001', 'WITHDRAW',  200.00,   800.00, NULL,   NULL,         NULL,       '2026-03-10 14:30:00', NULL),
  ('A001', 'DEPOSIT',   200.00,  1000.00, NULL,   NULL,         NULL,       '2026-04-05 11:00:00', NULL),
  ('A002', 'STATUS',      0.00,     NULL, NULL,   NULL,        'ACTIVATED', '2026-04-20 09:59:00', 'INITIATING -> ACTIVATED: Account opened'),
  ('A002', 'DEPOSIT',   500.50,   500.50, NULL,   NULL,         NULL,       '2026-04-20 10:00:00', NULL),
  ('A002', 'STATUS',      0.00,     NULL, NULL,   'ACTIVATED', 'FROZEN',    '2026-05-01 09:00:00', 'ACTIVATED -> FROZEN: Identity verification failed'),
  ('A002', 'STATUS',      0.00,     NULL, NULL,   'FROZEN',    'ACTIVATED', '2026-05-05 14:00:00', 'FROZEN -> ACTIVATED: Identity verified, unfrozen'),
  ('B002', 'STATUS',      0.00,     NULL, NULL,   NULL,        'ACTIVATED', '2026-05-10 08:59:00', 'INITIATING -> ACTIVATED: Account opened'),
  ('B002', 'DEPOSIT',  2000.00,  2000.00, NULL,   NULL,         NULL,       '2026-05-10 09:00:00', NULL),
  ('A001', 'WITHDRAW',  150.00,   850.00, 'A002', NULL,         NULL,       '2026-05-10 10:00:00', 'rent'),
  ('A002', 'DEPOSIT',   150.00,   650.50, NULL,   NULL,         NULL,       '2026-05-10 10:00:01', 'rent'),
  ('B001', 'WITHDRAW',  300.00,  4700.00, 'B002', NULL,         NULL,       '2026-05-10 11:00:00', 'fx'),
  ('B002', 'DEPOSIT',   300.00,  2300.00, NULL,   NULL,         NULL,       '2026-05-10 11:00:01', 'fx'),
  ('A005', 'STATUS',      0.00,     NULL, NULL,   NULL,        'ACTIVATED', '2026-06-02 08:59:00', 'INITIATING -> ACTIVATED: Account opened'),
  ('B003', 'STATUS',      0.00,     NULL, NULL,   NULL,        'ACTIVATED', '2026-06-02 08:59:01', 'INITIATING -> ACTIVATED: Account opened'),
  ('A005', 'DEPOSIT',  3000.00,  3000.00, NULL,   NULL,         NULL,       '2026-06-02 09:00:00', NULL),
  ('B003', 'DEPOSIT',   800.00,   800.00, NULL,   NULL,         NULL,       '2026-06-02 09:05:00', NULL),
  ('A005', 'WITHDRAW',  500.00,  2500.00, NULL,   NULL,         NULL,       '2026-06-10 13:00:00', NULL),
  ('A005', 'WITHDRAW',  300.00,  2200.00, 'A002', NULL,         NULL,       '2026-06-15 10:00:00', 'salary'),
  ('A002', 'DEPOSIT',   300.00,   950.50, NULL,   NULL,         NULL,       '2026-06-15 10:00:01', 'salary'),
  ('A003', 'STATUS',      0.00,     NULL, NULL,   NULL,        'ACTIVATED', '2026-06-18 08:59:00', 'INITIATING -> ACTIVATED: Account opened'),
  ('A003', 'STATUS',      0.00,     NULL, NULL,   'ACTIVATED', 'FROZEN',    '2026-06-20 10:00:00', 'ACTIVATED -> FROZEN: Suspicious transaction'),
  ('A006', 'STATUS',      0.00,     NULL, NULL,   NULL,        'ACTIVATED', '2026-06-24 08:59:00', 'INITIATING -> ACTIVATED: Account opened'),
  ('A006', 'DEPOSIT',   100.00,   100.00, NULL,   NULL,         NULL,       '2026-06-24 09:00:00', NULL),
  ('A006', 'WITHDRAW',  100.00,     0.00, NULL,   NULL,         NULL,       '2026-06-26 15:00:00', NULL),
  ('A006', 'STATUS',      0.00,     NULL, NULL,   'ACTIVATED', 'CLOSED',    '2026-06-28 16:00:00', 'ACTIVATED -> CLOSED: Customer requested closure'),
  ('B003', 'DEPOSIT',   200.00,  1000.00, NULL,   NULL,         NULL,       '2026-06-29 09:00:00', NULL),
  ('A001', 'DEPOSIT',   500.00,  1350.00, NULL,   NULL,         NULL,       '2026-06-30 12:00:00', 'payroll'),
  ('A001', 'WITHDRAW',  500.00,   850.00, NULL,   NULL,         NULL,       '2026-06-30 12:01:00', 'rent'),
  ('A002', 'DEPOSIT',   200.00,  1150.50, NULL,   NULL,         NULL,       '2026-06-30 12:10:00', 'refund'),
  ('A002', 'WITHDRAW',  200.00,   950.50, NULL,   NULL,         NULL,       '2026-06-30 12:11:00', 'atm'),
  ('A004', 'WITHDRAW',  345.67, 12000.00, NULL,   NULL,         NULL,       '2026-06-30 12:20:00', 'bill'),
  ('A004', 'DEPOSIT',   345.67, 12345.67, NULL,   NULL,         NULL,       '2026-06-30 12:21:00', 'adjust'),
  ('B001', 'DEPOSIT',   300.00,  5000.00, NULL,   NULL,         NULL,       '2026-06-30 12:40:00', 'fx in'),
  ('B001', 'WITHDRAW',  300.00,  4700.00, NULL,   NULL,         NULL,       '2026-06-30 12:41:00', 'fx out'),
  ('B003', 'WITHDRAW',  100.00,   900.00, NULL,   NULL,         NULL,       '2026-06-30 12:50:00', 'fee'),
  ('B003', 'DEPOSIT',   100.00,  1000.00, NULL,   NULL,         NULL,       '2026-06-30 12:51:00', 'fee rev');

INSERT INTO audit_log (actor, action, target_account_no, success, error_code, detail, created_at) VALUES
  ('admin', 'OPEN_ACCOUNT',   'A004', 1, NULL,   'user=david opened account',                    '2026-01-11 07:59:00'),
  ('admin', 'OPEN_ACCOUNT',   'A001', 1, NULL,   'user=alice opened account',                    '2026-02-15 08:59:00'),
  ('admin', 'OPEN_ACCOUNT',   'B001', 1, NULL,   'user=alice opened account',                    '2026-03-01 08:59:00'),
  ('admin', 'OPEN_ACCOUNT',   'A002', 1, NULL,   'user=bob opened account',                      '2026-04-20 09:59:00'),
  ('admin', 'FREEZE',         'A002', 1, NULL,   'status -> FROZEN (Identity verification failed)', '2026-05-01 09:00:00'),
  ('admin', 'ACTIVATE',       'A002', 1, NULL,   'status -> ACTIVATED (Identity verified, unfrozen)', '2026-05-05 14:00:00'),
  ('alice', 'TRANSFER',       'A001', 1, NULL,   '-> A002 amount=150.00 note=rent',              '2026-05-10 10:00:00'),
  ('alice', 'TRANSFER',       'B001', 1, NULL,   '-> B002 amount=300.00 note=fx',                '2026-05-10 11:00:00'),
  ('bob',   'WITHDRAW',       'A002', 0, 'E2001', '[E2001] Insufficient balance for account A002', '2026-05-20 09:00:00'),
  ('admin', 'DEPOSIT',        'A001', 0, 'E6003', '[E6003] Admin can only view records; deposits must be done by the account owner', '2026-05-21 09:00:00'),
  ('admin', 'OPEN_ACCOUNT',   'A005', 1, NULL,   'user=eve opened account',                      '2026-06-02 08:59:00'),
  ('bob',   'TRANSFER',       'A002', 0, 'E2004', '[E2004] Cross-category transfer not allowed: A002 -> B002', '2026-06-03 09:00:00'),
  ('eve',   'WITHDRAW',       'A005', 1, NULL,   'amount=500.00',                                '2026-06-10 13:00:00'),
  ('eve',   'TRANSFER',       'A005', 1, NULL,   '-> A002 amount=300.00 note=salary',            '2026-06-15 10:00:00'),
  ('admin', 'FREEZE',         'A003', 1, NULL,   'status -> FROZEN (Suspicious transaction)',    '2026-06-20 10:00:00'),
  ('carol', 'DEPOSIT',        'A003', 0, 'E1002', '[E1002] Account is not active: A003 (FROZEN)', '2026-06-20 11:00:00'),
  ('admin', 'UPDATE_OWNER',    NULL,  1, NULL,   'user=frank changed: owner name, phone, address', '2026-06-22 09:00:00'),
  ('admin', 'OPEN_ACCOUNT',   'A006', 1, NULL,   'user=frank opened account',                    '2026-06-24 08:59:00'),
  ('frank', 'WITHDRAW',       'A006', 1, NULL,   'amount=100.00',                                '2026-06-26 15:00:00'),
  ('admin', 'CLOSE',          'A006', 1, NULL,   'closed: Customer requested closure',           '2026-06-28 16:00:00'),
  ('eve',   'DEPOSIT',        'B003', 1, NULL,   'amount=200.00',                                '2026-06-29 09:00:00'),
  ('alice', 'CHANGE_PASSWORD', NULL,  1, NULL,   'user=alice changed: password (self)',          '2026-06-29 10:00:00'),
  ('admin', 'CHANGE_PASSWORD', NULL,  0, 'E5003', 'user=admin password change failed (self): Password must be at least 8 characters', '2026-06-30 11:00:00'),
  ('alice', 'DEPOSIT',        'A001', 1, NULL,   'amount=500.00',                                '2026-06-30 12:00:00'),
  ('alice', 'WITHDRAW',       'A001', 1, NULL,   'amount=500.00',                                '2026-06-30 12:01:00'),
  ('bob',   'DEPOSIT',        'A002', 1, NULL,   'amount=200.00',                                '2026-06-30 12:10:00'),
  ('bob',   'WITHDRAW',       'A002', 1, NULL,   'amount=200.00',                                '2026-06-30 12:11:00'),
  ('david', 'WITHDRAW',       'A004', 1, NULL,   'amount=345.67',                                '2026-06-30 12:20:00'),
  ('david', 'DEPOSIT',        'A004', 1, NULL,   'amount=345.67',                                '2026-06-30 12:21:00'),
  ('alice', 'DEPOSIT',        'B001', 1, NULL,   'amount=300.00',                                '2026-06-30 12:40:00'),
  ('alice', 'WITHDRAW',       'B001', 1, NULL,   'amount=300.00',                                '2026-06-30 12:41:00'),
  ('eve',   'WITHDRAW',       'B003', 1, NULL,   'amount=100.00',                                '2026-06-30 12:50:00'),
  ('eve',   'DEPOSIT',        'B003', 1, NULL,   'amount=100.00',                                '2026-06-30 12:51:00'),
  ('admin', 'UPDATE_OWNER',    NULL,  1, NULL,   'user=alice changed: phone',                    '2026-06-30 13:00:00'),
  ('admin', 'CHANGE_PASSWORD', NULL,  1, NULL,   'user=alice changed: password (admin reset)',   '2026-06-30 13:05:00'),
  ('alice', 'CHANGE_PASSWORD', NULL,  1, NULL,   'user=alice changed: password (self)',          '2026-06-30 13:10:00'),
  ('admin', 'UPDATE_OWNER',    NULL,  1, NULL,   'user=bob changed: owner name',                 '2026-06-30 13:15:00'),
  ('admin', 'UPDATE_OWNER',    NULL,  1, NULL,   'user=eve changed: address',                    '2026-06-30 13:20:00'),
  ('eve',   'CHANGE_PASSWORD', NULL,  1, NULL,   'user=eve changed: password (self)',            '2026-06-30 13:25:00'),
  ('admin', 'UPDATE_OWNER',    NULL,  1, NULL,   'user=alice changed: address',                  '2026-06-30 13:30:00');
