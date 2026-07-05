# Requirements — Account Management (Legacy)

## 功能需求

### R-A1：帳戶管理

| ID | 需求 | 狀態 |
|:---|:---|:---|
| R-A1.1 | 帳戶 CRUD：帳號 / 戶名 / 餘額 / 開戶日 / 狀態 | Done |
| R-A1.2 | 帳號格式 `[A|B]NNN`：A = 台幣帳戶、B = 外幣帳戶 | Done |
| R-A1.3 | 開戶時僅選擇類別（可多選），帳號自動累進（查 DB max + 1） | Done |
| R-A1.4 | 帳戶生命週期：ACTIVE ↔ FROZEN → CLOSED（終態） | Done |
| R-A1.5 | 關閉帳戶需餘額 = 0 + 填寫註記 | Done |
| R-A1.6 | 批次關戶：可一次選多個帳號關閉，逐一驗證 | Done |
| R-A1.7 | 狀態變更需填 note（單筆與批次皆是）：凍結填 freeze note（`[E3004] FREEZE_NOTE_REQUIRED`）、解凍填 activate note（`[E3005] ACTIVATE_NOTE_REQUIRED`），空白皆拒絕。note 寫入狀態變更 log | Done |

### R-A2：交易

| ID | 需求 | 狀態 |
|:---|:---|:---|
| R-A2.1 | 存款 / 提款（餘額不足拒絕） | Done |
| R-A2.2 | 轉帳：只允許同類別帳號之間（A→A / B→B），跨類別拒絕 | Done |
| R-A2.3 | 每筆交易記錄 balance_after（交易後餘額） | Done |
| R-A2.4 | 轉帳記錄 target_account_no（目標帳號） | Done |

### R-A3：歷史紀錄

| ID | 需求 | 狀態 |
|:---|:---|:---|
| R-A3.1 | Transaction History：顯示金額交易（type/amount/balance_after/target/date） | Done |
| R-A3.2 | Status Change History：顯示狀態變更（from_status/to_status/note/date） | Done |

### R-A4：認證

| ID | 需求 | 狀態 |
|:---|:---|:---|
| R-A4.1 | Spring Security form-based login | Done |
| R-A4.2 | admin 走 in-memory 認證，帳密走環境變數（不硬編） | Done |
| R-A4.3 | 一般客戶走 DB-backed 認證（users 表），密碼以 BCrypt 雜湊儲存 | Done |

### R-A5：系統帳號（登入使用者）

| ID | 需求 | 狀態 |
|:---|:---|:---|
| R-A5.1 | 開立銀行帳戶時同時建立系統登入帳號（username / password） | Done |
| R-A5.2 | 一個 owner 對應一個登入帳號，名下可有多個銀行帳戶（Account.ownerUsername 關聯） | Done |
| R-A5.3 | 密碼最少 8 字元、BCrypt 雜湊、不存明文 | Done |
| R-A5.4 | 使用者可自助變更密碼（須提供正確舊密碼） | Done |
| R-A5.5 | admin 可重設任一使用者密碼（無需舊密碼） | Done |
| R-A5.6 | owner name 中央管理於 User.displayName（一改全帳戶同步）；帳戶表顯示參照 user 資訊 | Done |
| R-A5.7 | 密碼二次確認：開戶 / 自助改密 / admin 重設密碼皆需輸入兩次且一致，否則 `[E5006] PASSWORD_MISMATCH` | Done |
| R-A5.8 | Edit owner 一併維護聯絡資料：admin 可改該 user 的顯示名稱 + 電話 + 地址（+ 選填新密碼，二次確認）；電話 / 地址同步該 user 名下所有帳戶 | Done |

### R-A9：開戶資料與電話唯一性（本輪強化）

| ID | 需求 | 狀態 |
|:---|:---|:---|
| R-A9.1 | 開戶時電話（phone）與地址（address）為必填，空白拒絕（`[E4003] PHONE_REQUIRED` / `[E4004] ADDRESS_REQUIRED`） | Done |
| R-A9.2 | 電話為 identity：同一電話 + 同一類別（TWD/FOREIGN）非 CLOSED 帳戶只能有一戶；重複開戶拒絕（`[E4005] DUPLICATE_ACCOUNT_FOR_PHONE`）。一人可有 1 台幣 + 1 外幣；關戶後可重開 | Done |

### R-A6：角色與授權（RBAC）

| ID | 需求 | 狀態 |
|:---|:---|:---|
| R-A6.1 | admin 可檢視 / 操作所有帳號 | Done |
| R-A6.2 | 一般 user 只能檢視 / 操作自己名下帳號 | Done |
| R-A6.3 | user 可：對自己帳戶存款 / 提款 / 轉帳（轉帳限自己帳戶轉出）、變更自己密碼 | Done |
| R-A6.4 | user 不可：開戶、凍結 / 解凍 / 關戶、看稽核頁、變更 owner name | Done |
| R-A6.8 | **admin 只能檢視紀錄，不可代做交易**：deposit / withdraw / transfer 一律拒絕 admin（`[E6003] ADMIN_CANNOT_TRANSACT`）；交易須由帳戶擁有者本人執行。admin 仍可檢視所有帳戶與交易紀錄、管理帳戶生命週期（開戶 / 凍結 / 解凍 / 關戶）與使用者 | Done |
| R-A6.5 | user 不可變更 username 與帳戶號碼；owner name 由 admin 中央管理 | Done |
| R-A6.6 | 授權雙層防護：URL 層（Spring Security 角色）＋ Action / service 層 ownership 檢查 | Done |
| R-A6.7 | 批次操作支援 freeze / activate / close（勾選多筆帳號） | Done |

### R-A7：錯誤碼

| ID | 需求 | 狀態 |
|:---|:---|:---|
| R-A7.1 | 所有錯誤都有錯誤碼，訊息前綴 `[Exxxx]`（如 `[E2001] Insufficient balance`） | Done |
| R-A7.2 | 錯誤碼分類：1xxx 帳戶 / 2xxx 交易 / 3xxx 生命週期 / 4xxx 開戶 / 5xxx 使用者 / 6xxx 授權 / 7xxx 查詢輸入 | Done |

### R-A8：稽核紀錄

| ID | 需求 | 狀態 |
|:---|:---|:---|
| R-A8.1 | 所有交易 / 狀態變更 / 帳戶操作寫入獨立 audit_log（記錄操作者、動作、對象、成功與否、錯誤碼） | Done |
| R-A8.2 | admin 可從頁面檢視全部稽核紀錄 | Done |
| R-A8.3 | 稽核頁支援過濾：操作者 / 動作 / 帳號 / 成功與否 / 錯誤碼 / 日期區間 / detail（純文字 contains 或 regex） | Done |
| R-A8.4 | owner name / 密碼 / 電話 / 地址 的變更皆寫 audit（UPDATE_OWNER、CHANGE_PASSWORD）。detail 必須記錄**變更對象（user=X）**與**實際變更的欄位**（比對舊值，只列真的有變更的欄位；成功與失敗事件皆寫，失敗記 attempted 欄位）；不寫籠統的「name/phone/address updated」 | Done |
| R-A8.5 | Audit Log 頁支援排序：sort-by 下拉選單 + 可直接點欄位標題（第一次該欄位 ASC、再次點同欄位切 DESC），沿用當前過濾條件 | Done |
| R-A8.6 | Edit Owner 頁面就地呈現該使用者的 profile 變更歷史（新到舊），完整全站稽核仍在 Audit Log 頁 | Done |

### R-A10：清單導覽（本輪）

| ID | 需求 | 狀態 |
|:---|:---|:---|
| R-A10.1 | 帳戶清單點擊「帳號」→ 進入該帳戶交易畫面（transactions） | Done |
| R-A10.2 | 帳戶清單點擊「owner name」→ 進入帳號編輯畫面（editOwnerForm，admin 專屬；一般 user 顯示純文字不連結） | Done |

## 非功能需求

| ID | 需求 | 說明 |
|:---|:---|:---|
| S1 | 一鍵啟動 | `./run.sh` 自動處理 DB 啟動、port 清理、build、開瀏覽器 |
| S2 | 機敏值不硬編 | DB_PASSWORD / ADMIN_PASSWORD 走 .env + 環境變數 |
| S3 | Golden baseline | `./run.sh db-reset` 可還原出廠資料（手動觸發） |
| S5 | 金額精度 | 所有金額用 BigDecimal / DECIMAL(19,2) |

## Security Requirements

- 認證：admin 走 Spring Security in-memory（環境變數帳密）；一般客戶走 DB-backed（users 表）
- 密碼儲存：BCrypt 雜湊，不存明文；最少 8 字元
- 授權：RBAC 雙層（URL 角色規則 ＋ Action/service ownership 檢查）；所有頁面需登入
- 稽核：所有 mutating 操作寫 audit_log（含操作者身分與失敗事件的錯誤碼）
- 錯誤碼：所有錯誤前綴 `[Exxxx]`，便於除錯與追蹤
- Secrets 管理：.env 不進版控（.gitignore）；seed 使用者的預設密碼（user1234）僅供 lab
- Input validation：金額 > 0、帳號格式 [A|B]NNN、關戶需餘額 = 0、轉帳註記 ≤ 7 字元、密碼 ≥ 8 字元、username 唯一、開戶電話 / 地址必填、密碼二次確認一致、同電話同類別非 CLOSED 唯一

## 維護規則（Process）

- **需求變更 → 一併更新文件**：任何功能 / 規則變更，必須同步更新 `requirements.md` / `design.md` / `tasks.md` / `README.md` / `.postman.json`，並為新功能補上 test case（service 層單元測試優先）。
- **golden sample data 變更 → 下次啟動須 factory reset**：一旦改動 `docker/golden/reset-to-factory.sql` 或 `docker/init/01-schema-seed.sql`，下次啟動前須執行 `./run.sh db-reset` 讓 DB 回到最新出廠資料。
- **golden timestamp 上限**：所有 golden 時間（users/accounts/transactions/audit_log）不得晚於 **2026-06-30**，且維持 id 與時間同序（升序）。

## 已知遷移雷（安全相關）

- Spring Security 4.1.5 的 `BCryptPasswordEncoder` **只接受 `$2a$` / `$2b$` 前綴，不接受 `$2y$`**
  （htpasswd 預設產 `$2y$`）。seed 密碼雜湊須用 `$2a$`（本專案由 app 自身 encoder 產生驗證）。
  遷移到新版 Spring Security 時此限制已放寬，可一併檢視。
