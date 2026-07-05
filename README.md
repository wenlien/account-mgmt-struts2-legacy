# Account Management (Struts 2 + Spring 4 + Hibernate 4) — Legacy App

> AI-Java-Migration-with-Kiro-IDE 課程的**遷移起點 app**。學員拿到這份 legacy 版本，
> 用 Kiro 主導遷移到 Spring Boot 3 + Spring Data JPA（Java 21）。

## 功能

- **帳戶管理**：開戶（選類別 + 可帶初始存款自動產生帳號，同時建立登入帳號）/ 編輯戶名 / 凍結 / 解凍 / 關戶（含批次）
- **帳號規則**：格式 `[A|B]NNN`（A = 台幣、B = 外幣），自動累進
- **交易**：存款 / 提款 / 轉帳（**僅帳戶擁有者本人可做，admin 只能看紀錄不可代做**；轉帳限同類別帳號、可帶 ≤7 字元註記寫入雙方紀錄）
- **狀態變更需註記**：凍結填 freeze note、解凍填 activate note（單筆與批次皆必填），寫入狀態變更紀錄（Status Note）
- **歷史紀錄**：分 Transaction History（金額異動 + 餘額 + 轉帳目標 + 註記）和 Status Change History（前/後狀態）
- **認證 / 授權（RBAC）**：admin 走 in-memory（環境變數）、一般客戶走 DB + BCrypt；admin 看/管所有帳號、可中央改 owner 名稱與重設密碼；user 只看/操作自己名下（存/提/轉）+ 改自己密碼
- **owner name 中央化**：owner 名稱存於 User，帳戶表參照 user 資訊；admin 改一次，該 user 名下所有帳戶同步。Edit owner 一併可改電話 / 地址（同步該 user 所有帳戶）+ 選填新密碼
- **批次操作**：勾選多筆帳號批次 freeze / activate / close
- **錯誤碼**：所有錯誤前綴 `[Exxxx]`
- **稽核**：所有交易 / 狀態變更 / profile 變更（owner name/密碼/電話/地址，含失敗）寫入獨立 audit_log，detail 記錄「變更對象 + 實際變更欄位」；admin 稽核頁支援多條件過濾（操作者/動作/帳號/成功與否/錯誤碼/日期/detail 純文字或 regex）與排序（下拉或點欄位標題，ASC/DESC 切換）
- **就地變更歷史**：Edit Owner 頁面下方就地呈現該使用者的 profile 變更紀錄；帳戶清單可點「帳號」進交易畫面、點「owner name」進編輯畫面
- **開戶資料**：開戶時電話 + 地址必填；密碼需二次確認；同電話同類別（台幣/外幣）非 CLOSED 限一戶（一人可 1 台幣 + 1 外幣）
- **UI**：branded sticky header（`abc-logo.svg` + "Anycompany Business Capital (ABC)" + 登入帳戶/角色 + 功能連結 + Logout）；admin 帳戶清單的操作改為下拉選單（Edit Owner / Freeze / Activate / Close）

## 技術棧

| 層 | 技術 |
|:---|:---|
| Web | Struts 2.5 + JSP |
| DI | Spring 4.3（XML bean） |
| Security | Spring Security 4 |
| ORM | Hibernate 4 + MySQL |
| Build | Maven / Embedded Tomcat 7 |
| Runtime | JDK 17（`--add-opens` 相容） |

## 快速啟動

```bash
# 前置：Docker 已裝、MySQL 容器已建（docker compose up -d）
# 一鍵 build + 啟動 + 開瀏覽器：
./run.sh

# 只啟動（已 build 過）：
./run.sh run
```

腳本自動處理：
1. 載入 `.env`（DB_PASSWORD / ADMIN_PASSWORD 等）
2. 確認 MySQL container 就緒（沒跑會自動 `docker compose up -d`）
3. 清理殘留佔 port 的 Java process
4. Build（`all` 模式）
5. 啟動 Embedded Tomcat
6. Server ready 後自動開瀏覽器

登入：admin 帳密在 `.env`（`ADMIN_USERNAME` / `ADMIN_PASSWORD`）；一般客戶為 DB 使用者 alice/bob/carol/david（密碼 `user1234`，見下方 Golden Baseline）。

## 指令

| 指令 | 用途 |
|:---|:---|
| `./run.sh` | build + 啟動（預設） |
| `./run.sh run` | 只啟動 |
| `./run.sh build` | 只 build |
| `./run.sh test` | 只跑測試 |
| `./run.sh db-reset` | 還原 golden 出廠資料（手動觸發） |
| `./run.sh open` | 只開瀏覽器 |
| `./run.sh help` | 完整說明 |

## 領域模型

- **Account**：`accountNo`（`[A|B]NNN`）、`ownerName`、`balance`、`openedDate`、`status`（ACTIVE/FROZEN/CLOSED）、`closedDate`、`closeNote`、`ownerUsername`（FK→users）、`phone`、`address`
- **Transaction**：`txId`、`account`（FK）、`type`（DEPOSIT/WITHDRAW/STATUS）、`amount`、`balanceAfter`、`targetAccountNo`、`fromStatus`、`toStatus`、`createdAt`、`note`
- **User**：`username`（PK）、`passwordHash`（BCrypt）、`role`、`displayName`、`createdAt`
- **AuditLog**：`id`、`actor`、`action`、`targetAccountNo`、`success`、`errorCode`、`detail`、`createdAt`
- **AccountCategory**：TWD（前綴 A）/ FOREIGN（前綴 B）

## Golden Baseline（出廠資料）

| 帳號 | 戶名 | owner | 類別 | 餘額 | 狀態 |
|:---|:---|:---|:---|:---|:---|
| A001 | Alice Chen | alice | 台幣 | 850.00 | ACTIVE |
| A002 | Bob Lin | bob | 台幣 | 950.50 | ACTIVE |
| A003 | Carol Wu | carol | 台幣 | 0.00 | FROZEN |
| A004 | David Ho | david | 台幣 | 12345.67 | ACTIVE |
| A005 | Eve Wang | eve | 台幣 | 2200.00 | ACTIVE |
| A006 | Frank Wu | frank | 台幣 | 0.00 | CLOSED |
| B001 | Alice Chen | alice | 外幣 | 4700.00 | ACTIVE |
| B002 | Bob Lin | bob | 外幣 | 2300.00 | ACTIVE |
| B003 | Eve Wang | eve | 外幣 | 1000.00 | ACTIVE |

- 交易 42 筆（含 3 組 transfer 配對兩腿：A001→A002 rent / B001→B002 fx / A005→A002 salary，及一批 6/30 額外收付供示範）+ 狀態變更 + 稽核 40 筆（含豐富 profile 變更歷史）
- id 與時間同序（transactions / audit_log 依 created_at 升序）
- `./run.sh db-reset` 可還原

### 登入帳號（golden）

| 角色 | 帳號 | 密碼 | 可見帳戶 |
|:---|:---|:---|:---|
| ADMIN | admin | 見 `.env`（`ADMIN_PASSWORD`） | 全部 |
| USER | alice | user1234 | A001, B001 |
| USER | bob | user1234 | A002, B002 |
| USER | carol | user1234 | A003 |
| USER | david | user1234 | A004 |
| USER | eve | user1234 | A005, B003 |
| USER | frank | user1234 | A006（CLOSED） |

> admin 走 in-memory（環境變數）；alice/bob/carol/david/eve/frank 為 DB 使用者（BCrypt）。lab 用預設密碼，正式環境請改。

## 業務規則

1. 帳號由系統自動產生（開戶只選類別），不可手動指定；開戶同時建立登入帳號（一 owner 一帳號）
2. 開戶時電話 + 地址必填（`[E4003]`/`[E4004]`）；密碼需二次確認（`[E5006]`）
3. 電話為 identity：同電話 + 同類別（台幣/外幣）非 CLOSED 限一戶（`[E4005]`）；一人可 1 台幣 + 1 外幣，關戶後可重開
4. 只允許同類別帳號間轉帳（A→A / B→B）；跨類別拒絕；轉帳註記 ≤ 7 字元，寫入轉出/轉入雙方紀錄
5. 關戶需餘額 = 0 + 填註記；支援一次關多個帳號
6. CLOSED 為終態：不可再交易、不可再變更狀態
7. 授權：admin 管帳戶生命週期與使用者（開戶/凍結/解凍/關戶、中央改 owner 名稱/電話/地址/密碼、批次操作）+ 檢視所有帳戶與紀錄，但**不可代做交易**（存/提/轉一律拒絕 admin，`[E6003]`）；user 只能看/操作自己名下帳戶（存/提/轉，轉帳限自己轉出）+ 改自己密碼；不可開戶/凍結/關戶/看稽核/改 owner 資料/改 username/改帳號
8. 狀態變更（單筆與批次）須填 note：凍結填 freeze note（`[E3004]`）、解凍填 activate note（`[E3005]`）
9. owner 名稱與聯絡資料中央管理於 User / 帳戶（一改全帳戶同步）；帳戶表顯示參照 user 資訊
10. 所有錯誤帶錯誤碼 `[Exxxx]`；所有交易/狀態變更寫入 audit_log
11. 需求變更一併更新文件 + 補 test case；改 golden 則下次啟動須 `./run.sh db-reset`

## 設定（.env）

```bash
cp .env.example .env
# 填入：
APP_HOST=localhost
APP_PORT=8080
ADMIN_USERNAME=admin
ADMIN_PASSWORD=<your-password>
DB_PASSWORD=<mysql-password>
```

## 刻意保留的遷移雷

| 雷 | 位置 |
|:---|:---|
| Struts Action / `struts.xml` | `action/*Action.java` + `resources/struts.xml` |
| Spring 4 XML bean | `resources/spring-config.xml` |
| Hibernate `SessionFactory` | `dao/impl/*Impl.java` |
| `javax.persistence.*` | entity annotations |
| `WebSecurityConfigurerAdapter` | `security/SecurityConfig.java` |
| JUnit 4 | `src/test/.../*Test.java` |
| Servlet 3.0 web.xml | `webapp/WEB-INF/web.xml` |

## 授權

MIT License — 見 [LICENSE](LICENSE)。
