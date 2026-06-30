# Account Management (Struts 2 + Spring 4 + Hibernate 4) — Legacy 起點 App

> AI-Java-Migration-with-Kiro-IDE 課程的**遷移起點 app**。學員拿到這份 legacy 版本，
> 用 Kiro 主導遷移到 Spring Boot 3 + Spring Data JPA（Java 21）。
>
> 本專案由 [`bookstore-struts2-spring-hibernate`](https://github.com/)（MIT, 2024 Harshal Patil）
> **re-skin** 而來：領域從書店改為帳戶管理，並新增 Transaction、Spring Security 登入、JUnit 4 測試。
> 授權見 [LICENSE](LICENSE)。

## 功能

- 帳戶 CRUD（帳號 / 戶名 / 餘額 / 開戶日 / 狀態）
- 存款 / 提款（不含轉帳，決策 D1）
- 交易紀錄查詢
- Spring Security 登入認證

## 技術棧（遷移前）

| 層 | 技術 |
|:---|:---|
| Web | Struts 2.5（`struts.xml` 路由）+ JSP |
| 整合 | struts2-spring-plugin |
| DI | Spring 4.1（`spring-config.xml` XML bean） |
| 安全 | Spring Security 4（`WebSecurityConfigurerAdapter`） |
| 持久層 | Hibernate 4（`SessionFactory`）+ MySQL |
| 平台 | Java 8 / Maven / `javax.*` |

## 刻意保留的遷移雷（對應課程 design.md §3.3）

| 雷 | 位置 |
|:---|:---|
| Struts Action / `struts.xml` | `action/*Action.java` + `resources/struts.xml` |
| Spring 4 XML bean | `resources/spring-config.xml` |
| struts2-spring-plugin | `pom.xml` 依賴 + Action 的 `@Autowired` |
| Hibernate `SessionFactory` | `dao/impl/*Impl.java` |
| `javax.*` | entity 的 `javax.persistence`、`web.xml` 的 j2ee 命名空間 |
| `WebSecurityConfigurerAdapter` | `security/SecurityConfig.java`（Spring Security 6 已移除） |
| JUnit 4 | `src/test/.../*Test.java`（`@Before` / `org.junit.Test`） |
| `org.springframework.orm.hibernate4.*` | `spring-config.xml`（Spring 5 已移除） |

## 領域模型

- `Account`：`accountNo`(PK, String)、`ownerName`、`balance`(BigDecimal)、`openedDate`、`status`(ACTIVE/FROZEN/CLOSED)
- `Transaction`：`txId`(PK)、`account`(FK→Account)、`type`(DEPOSIT/WITHDRAW)、`amount`(BigDecimal)、`createdAt`
- 關聯：`Account 1 — N Transaction`

## 設定與執行

> ⚠️ DB 連線、Docker Compose（MySQL）、seed 資料與 `.env`（含 `DB_PASSWORD` / `ADMIN_PASSWORD`）
> 屬課程 **Phase 2（環境）** 的交付物，尚未包含在本 Phase 1 起點 app。

- 機敏值走環境變數（S2，不硬編密鑰）：
  - `DB_PASSWORD`：MySQL 連線密碼（`spring-config.xml` 以 `${DB_PASSWORD:}` 取用）
  - `ADMIN_USERNAME` / `ADMIN_PASSWORD`：登入帳密（`SecurityConfig` in-memory 認證）
- 非機敏 DB 設定在 `src/main/resources/db.properties`（DB 名 `accountdb`）。
- 建置：`mvn clean package`（產出 war，部署到 Servlet 容器如 Tomcat 9）。

## 已知待辦（Phase 2 build 驗證）

- 實機 `mvn clean package` 驗證編譯（Struts 2.5 / Spring 4.1 / Spring Security 4.1 版本對齊）。
- Docker Compose 起 MySQL + seed 資料（可斷言：A001 餘額 = 1000.00 等）。
- 凍結 golden master baseline 請求集。

## 授權

MIT License — 見 [LICENSE](LICENSE)。衍生自 Harshal Patil 的 bookstore 範例。
