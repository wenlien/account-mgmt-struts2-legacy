# Tasks — Account Management (Legacy)

## 已完成

- [x] 帳戶 CRUD（Account entity + DAO + Service + Action + JSP）
- [x] 交易功能（存款 / 提款 / 轉帳）
- [x] 帳戶生命週期（ACTIVE ↔ FROZEN → CLOSED）
- [x] 狀態變更寫入交易 log（STATUS type + fromStatus/toStatus）
- [x] History 分兩類顯示（Transaction History + Status Change History）
- [x] Transaction 加 balanceAfter / targetAccountNo 欄位
- [x] 帳號格式 [A|B]NNN + 自動累進
- [x] 同類別轉帳限制（跨類別拒絕）
- [x] 批次關戶（closeAccounts）
- [x] Golden sample data（含 B 類帳戶 + STATUS 假資料）
- [x] Spring Security login（環境變數注入帳密）
- [x] run.sh 一鍵啟動（DB 自動拉起 + port 清理 + build + 開瀏覽器）
- [x] 轉帳註記（≤7 字元，寫入轉出/轉入雙方紀錄）
- [x] 系統登入帳號（#6）：開戶同時建 user、DB-backed 認證、BCrypt 雜湊、自助改密碼
- [x] 角色授權 RBAC（#7/#8）：admin 全權、user 只看/操作自己名下、僅可轉帳+改戶名+改密碼；URL + service 雙層防護
- [x] 錯誤碼體系（#5）：BankErrorCode + CodedException，訊息前綴 [Exxxx]
- [x] 稽核 audit_log（#9）：所有交易/狀態變更記操作者+錯誤碼；admin 過濾查詢頁
- [x] Golden 擴充：DB 使用者、accounts.owner_username、含 transfer 的交易、audit 假資料
- [x] 存款/提款開放給帳戶擁有者本人（非僅 admin）；service 層 ownership 檢查
- [x] owner name 中央化（User.displayName，Account @ManyToOne owner 唯讀參照；admin 一改全帳戶同步）
- [x] user 帳戶清單移除 Actions 欄（唯一自助＝改密碼）
- [x] admin 中央管理：改 owner 名稱、重設 user 密碼（無需舊密碼）
- [x] 批次 freeze / activate（與批次 close 並列）
- [x] audit 過濾強化：errorCode + detail 純文字/regex（regex 語法錯誤回 E7001）
- [x] golden id 與時間同序（單一 INSERT 依 created_at 升序）+ 更多元資料（6 user / 9 帳戶含 CLOSED）
- [x] 品牌改名 AnyCompany Business Capital (ABC) + `abc-logo.svg`（舊 AWS logo 保留）
- [x] 密碼二次確認（開戶 / 自助改密 / admin 重設，`[E5006]`）
- [x] 開戶電話 + 地址必填（`[E4003]`/`[E4004]`）
- [x] 電話唯一性：同電話同類別非 CLOSED 限一戶（`[E4005]`），一人可 1 台幣 + 1 外幣
- [x] Edit owner 擴充：改顯示名 + 電話 + 地址（同步該 user 所有帳戶）+ 選填新密碼
- [x] Actions 欄改下拉選單（Edit Owner / Freeze / Activate / Close）
- [x] golden timestamp 全改 2026 + 補 phone/address seed
- [x] 維護規則：需求變更一併更新文件 + 補 test case；改 golden 則下次啟動 factory reset
- [x] item1：admin 只能看紀錄，不可代做交易（deposit/withdraw/transfer 擋 admin，`[E6003]`）；transactionList 對 admin 隱藏交易表單
- [x] item2：凍結需填 freeze note（單筆 `freezeAccountForm`、批次 `batchFreezeForm`，`[E3004]`）；note 寫入 status log
- [x] 解凍也需填 activate note（單筆 `activateAccountForm`、批次 `batchActivateForm`，`[E3005]`）
- [x] 交易頁欄位改名：Transaction History「Note」→「Transaction Note」、Status Change History「Note」→「Status Note」
- [x] 主場景 diagrams（design.md §7）：ER diagram、帳戶建立流程（含電話檢查）、開戶/轉帳/凍結 sequence；狀態機於 §3.0
- [x] golden timestamp 全部 ≤ 2026-06-30（維持 id 與時間同序）
- [x] 帳戶清單導覽：點「帳號」→ 交易畫面、點「owner name」→ 編輯畫面（admin）
- [x] profile 變更（owner name/密碼/電話/地址）寫 audit，detail 記「變更對象 + 實際變更欄位」（比對舊值、含失敗）；Edit Owner 頁面就地呈現該使用者變更歷史
- [x] Audit Log 排序：sort-by 下拉 + 點欄位標題（1st ASC / 2nd DESC），沿用過濾條件
- [x] JUnit 4 測試（66 tests pass：UserService 13 / Audit 9 / Account 27 / Transaction 17）+ Postman/newman

## 待做（課程遷移用）

- [ ] 遷移到 Spring Boot 3 + Spring Data JPA（Java 21）
- [ ] javax.persistence → jakarta.persistence
- [ ] Struts2 Action → @RestController
- [ ] Spring XML beans → Java Config / auto-configuration
- [ ] WebSecurityConfigurerAdapter → SecurityFilterChain bean
- [ ] JUnit 4 → JUnit 5
- [ ] Hibernate SessionFactory → Spring Data Repository（含新增的 UserDAO / AuditLogDAO）
- [ ] Embedded Tomcat 7 → Spring Boot embedded Tomcat
- [ ] `CustomUserDetailsService` + `AuthenticationManagerBuilder` → 新版 `SecurityFilterChain` + `UserDetailsService` bean
- [ ] BCrypt：SS4 只認 `$2a$`/`$2b$`（拒 `$2y$`）；遷移到新版 Spring Security 後此限制放寬，可重新檢視 seed 雜湊

## 指令速查

| 指令 | 用途 |
|:---|:---|
| `./run.sh` | build + 啟動（預設） |
| `./run.sh run` | 只啟動（不重新 build） |
| `./run.sh build` | 只 build |
| `./run.sh test` | 只跑測試 |
| `./run.sh db-reset` | 還原 golden 出廠資料 |
| `./run.sh open` | 只開瀏覽器 |
| `./run.sh help` | 顯示完整說明 |
