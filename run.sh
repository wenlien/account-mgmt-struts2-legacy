#!/bin/bash
#
# account-mgmt-struts2-legacy — 可重複執行的 build / run 腳本
#
# 用法：
#   ./run.sh            # build + 啟動 app（embedded Tomcat 7）
#   ./run.sh build      # 只 build（mvn clean package）
#   ./run.sh run        # 只啟動（用既有的 war，不重新 build）
#   ./run.sh test       # 只跑測試
#   PORT=9090 ./run.sh  # 換 port（預設 8080）
#
# 前置需求：
#   - Maven 3.9+（brew install maven）
#   - JDK 8/11/17（本腳本自動找 openjdk@17；此為 Java 8 legacy app，不能用 JDK 20+）
#   - 啟動 app 需要可連線的 MySQL（見下方 .env / db.properties）；否則 Spring 啟動會失敗
#
set -uo pipefail

APP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$APP_DIR" || exit 1

# 讓 docker / mvn / brew 等可被找到（不論從哪個 shell 呼叫）
export PATH="/opt/homebrew/bin:/usr/local/bin:$PATH"

CMD="${1:-all}"

# ---- help / 用法說明 ----
usage() {
  cat <<'EOF'
account-mgmt-struts2-legacy — build / run 腳本

用法：
  ./run.sh [指令]

指令：
  all      (預設) build + 啟動 app（mvn clean package → embedded Tomcat 7）
  build    只 build（mvn clean package，產出 target/*.war）
  run      只啟動（用既有的 war，不重新 build）
  test     只跑單元測試（mvn test）
  open     只開瀏覽器到 app 網址（假設 server 已在跑；不需 JDK/mvn）
  db-reset 把 DB 還原成 golden 出廠資料（reset to factory；需 accountdb-mysql 容器在跑）
  help     顯示這份說明（-h / --help 亦可）

啟動流程（run / all）：載入 .env → 殺佔 port 的殘留 Java process → 確認 MySQL 容器就緒 → 清除 Tomcat 殘留 config → 起 web app → 背景偵測 ready → 開瀏覽器 → 把 server 帶回前景（Ctrl-C 停止）。

環境變數（可寫進 .env）：
  APP_HOST                 開瀏覽器用的主機（預設 localhost）
  APP_PORT                 embedded Tomcat 監聽的 port（預設 8080）
  PORT                     臨時覆寫 port（優先權高於 .env 的 APP_PORT）
  JDK                      指定 JDK 版本（如 JDK=8 / JDK=17 / JDK=21）；預設優先用 openjdk@17
  RUN_USE_ENV_JAVA_HOME=1  強制沿用現有 JAVA_HOME（預設優先用 openjdk@17）
  NO_OPEN=1                run/all 啟動後不要自動開瀏覽器

範例：
  ./run.sh                 # build 後啟動，server ready 後自動開瀏覽器
  ./run.sh build           # 只編譯打包
  ./run.sh run             # 直接啟動既有 war（省去重新 build）
  ./run.sh open            # 只開瀏覽器（server 已在跑時用）
  ./run.sh db-reset        # DB 還原成 golden 出廠資料
  ./run.sh test            # 只跑測試
  PORT=9090 ./run.sh run   # 臨時換 9090 port 啟動
  NO_OPEN=1 ./run.sh run   # 啟動但不自動開瀏覽器
  JDK=8 ./run.sh run       # 用 Java 8 啟動（需 brew install openjdk@8；自動略過 --add-opens）
  JDK=21 ./run.sh run      # 用 Java 21 啟動（需 brew install openjdk@21；自動帶 --add-opens）

前置需求：
  - Maven 3.9+（brew install maven）
  - JDK 8/11/17（腳本自動找 openjdk@17；Java 8 app 建議別用 JDK 20+ 啟動）
  - 機敏 / 主機設定放 .env（cp .env.example .env 後填）：
      APP_HOST / APP_PORT / DB_PASSWORD / ADMIN_USERNAME / ADMIN_PASSWORD
  - 帳戶 / 交易頁需要可連線的 MySQL（未起時 app 仍能啟動、登入頁可開）
EOF
}

# help 要在檢查 JDK / mvn 之前就短路（沒裝工具也能看說明）
case "$CMD" in
  help|-h|--help)
    usage
    exit 0
    ;;
esac

# ---- 1. 載入機敏設定（.env，不進版控；沒有就用預設/環境變數） ----
if [ -f "$APP_DIR/.env" ]; then
  echo "[run] 載入 .env"
  set -a
  # shellcheck disable=SC1091
  . "$APP_DIR/.env"
  set +a
else
  echo "[run] 未找到 .env（可 cp .env.example .env 後填值）；沿用現有環境變數 / 預設"
fi

# ---- 1.5 host / port（由 .env 的 APP_HOST / APP_PORT 提供，避免在腳本裡 hard-code 主機資訊）
#          優先序：命令列 PORT 環境變數 > .env 的 APP_PORT > 預設 8080；host 同理 APP_HOST > localhost ----
APP_HOST="${APP_HOST:-localhost}"
APP_PORT="${PORT:-${APP_PORT:-8080}}"
APP_PATH="/account-mgmt-struts2-legacy/"
APP_URL="http://${APP_HOST}:${APP_PORT}${APP_PATH}"

# 開瀏覽器（macOS open / Linux xdg-open）
open_browser() {
  local url="${1:-$APP_URL}"
  if command -v open >/dev/null 2>&1; then
    open "$url" >/dev/null 2>&1 || true          # macOS
  elif command -v xdg-open >/dev/null 2>&1; then
    xdg-open "$url" >/dev/null 2>&1 || true       # Linux
  else
    echo "[run] （找不到 open/xdg-open，請手動開啟：$url）"
  fi
}

# 背景輪詢直到 server 可連線後，才開瀏覽器（NO_OPEN=1 可停用）
wait_and_open() {
  [ "${NO_OPEN:-0}" = "1" ] && { echo "[run] NO_OPEN=1：略過自動開瀏覽器"; return 0; }
  local url="${1:-$APP_URL}" i
  for i in $(seq 1 90); do
    if curl -s -o /dev/null -m 2 "$url" 2>/dev/null; then
      echo "[run] server 已就緒 → 開啟瀏覽器 $url"
      open_browser "$url"
      return 0
    fi
    sleep 1
  done
  echo "[run] 等候 server 逾時（90s），仍嘗試開啟瀏覽器 $url"
  open_browser "$url"
}

# ---- 不需 JDK/mvn 的指令：只開瀏覽器 / DB 出廠重置 ----
case "$CMD" in
  open|url|browser)
    echo "[run] 開啟 → ${APP_URL}"
    open_browser "$APP_URL"
    exit 0
    ;;
  db-reset|reset-db|db-seed)
    SQL="$APP_DIR/docker/golden/reset-to-factory.sql"
    [ -f "$SQL" ] || { echo "[run] ERROR: 找不到 $SQL" >&2; exit 1; }
    command -v docker >/dev/null 2>&1 || { echo "[run] ERROR: 找不到 docker" >&2; exit 1; }
    if ! docker ps --format '{{.Names}}' 2>/dev/null | grep -qx accountdb-mysql; then
      echo "[run] ERROR: MySQL 容器 accountdb-mysql 未運行。先啟動：docker start accountdb-mysql（或 docker compose up -d）" >&2
      exit 1
    fi
    echo "[run] 套用 golden 出廠資料 → accountdb（reset to factory）"
    if docker exec -i -e MYSQL_PWD="${DB_PASSWORD:-devpass}" accountdb-mysql \
         mysql -uaccountuser accountdb < "$SQL"; then
      echo "[run] 完成。目前 golden 帳戶："
      docker exec -e MYSQL_PWD="${DB_PASSWORD:-devpass}" accountdb-mysql \
         mysql -uaccountuser -e "SELECT account_no,balance,status FROM accounts" accountdb 2>/dev/null
      exit 0
    fi
    echo "[run] ERROR: 套用失敗。" >&2
    exit 1
    ;;
esac

# ---- 2. 解析 JAVA_HOME（優先 openjdk@17；Java 8 target 不能用 JDK 20+） ----
resolve_java_home() {
  # 這是 Java 8 legacy app + embedded Tomcat 7：預設優先用 JDK 17（最穩）。
  # 選版優先序：JDK=<版本> > RUN_USE_ENV_JAVA_HOME + JAVA_HOME > openjdk@17 > java_home > 現有 JAVA_HOME。

  # 2a-0. 明確指定版本：JDK=8 / JDK=17 / JDK=21 ...（先試 brew openjdk@N，再試 java_home）
  if [ -n "${JDK:-}" ]; then
    local p jh v want major
    case "$JDK" in 8|1.8) want=8 ;; *) want="$JDK" ;; esac
    # (a) brew openjdk@N：版本由 formula 保證，命中即用
    if command -v brew >/dev/null 2>&1; then
      p="$(brew --prefix "openjdk@${JDK}" 2>/dev/null)/libexec/openjdk.jdk/Contents/Home"
      if [ -x "$p/bin/java" ]; then export JAVA_HOME="$p"; return 0; fi
    fi
    # (b) java_home：注意它找不到指定版本時會「回退成預設 JDK 且 exit 0」，
    #     故必須驗證回傳的 major 真的等於所求，才採用。
    case "$JDK" in 8|1.8) v=1.8 ;; *) v="$JDK" ;; esac
    if [ -x /usr/libexec/java_home ]; then
      jh="$(/usr/libexec/java_home -v "$v" 2>/dev/null)"
      if [ -n "$jh" ] && [ -x "$jh/bin/java" ]; then
        major="$("$jh/bin/java" -version 2>&1 | awk -F'"' '/version/{print $2; exit}' \
          | awk -F. '{ if ($1=="1") print $2; else print $1 }')"
        if [ "$major" = "$want" ]; then export JAVA_HOME="$jh"; return 0; fi
      fi
    fi
    echo "[run] ERROR: 找不到 JDK ${JDK}（試過 brew openjdk@${JDK} 與 java_home -v ${v}）。安裝：brew install openjdk@${JDK}" >&2
    return 1
  fi

  # 2a. 明確要求沿用環境 JAVA_HOME
  if [ "${RUN_USE_ENV_JAVA_HOME:-0}" = "1" ] && [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    return 0
  fi
  # 2b. 優先 brew openjdk@17
  if command -v brew >/dev/null 2>&1; then
    local p
    p="$(brew --prefix openjdk@17 2>/dev/null)/libexec/openjdk.jdk/Contents/Home"
    if [ -x "$p/bin/java" ]; then
      export JAVA_HOME="$p"
      return 0
    fi
  fi
  # 2c. macOS java_home（依序試 17 / 11 / 8）
  if [ -x /usr/libexec/java_home ]; then
    local jh
    for v in 17 11 1.8; do
      jh="$(/usr/libexec/java_home -v "$v" 2>/dev/null)" && [ -n "$jh" ] && { export JAVA_HOME="$jh"; return 0; }
    done
  fi
  # 2d. 最後才沿用現有 JAVA_HOME（可能是 JDK 20+；build 可行、run 風險自負）
  if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    echo "[run] WARN: 找不到 JDK 17，沿用現有 JAVA_HOME=$JAVA_HOME（若為 JDK 20+，啟動 embedded Tomcat 可能不穩）" >&2
    return 0
  fi
  return 1
}

if ! resolve_java_home; then
  echo "[run] ERROR: 找不到可用的 JDK 8/11/17。請先安裝，例如：brew install openjdk@17" >&2
  exit 1
fi

# 確保 mvn 在 PATH（brew 安裝路徑）
export PATH="/opt/homebrew/bin:/usr/local/bin:$PATH"

if ! command -v mvn >/dev/null 2>&1; then
  echo "[run] ERROR: 找不到 mvn。請先安裝：brew install maven" >&2
  exit 1
fi

echo "[run] JAVA_HOME = $JAVA_HOME"
"$JAVA_HOME/bin/java" -version
echo "[run] mvn: $(command -v mvn)"

# 偵測 JDK major 版本（1.8 → 8，其餘取開頭數字）
JAVA_MAJOR="$("$JAVA_HOME/bin/java" -version 2>&1 | awk -F'"' '/version/{print $2; exit}' \
  | awk -F. '{ if ($1=="1") print $2; else print $1 }')"
echo "[run] 偵測到 JDK major = ${JAVA_MAJOR:-unknown}"

# ---- 2.5 JDK 9+ 相容：Spring 4 的 CGLIB 會反射存取 java.lang.ClassLoader.defineClass，
#          JDK 16+ 預設禁止 → 需開 --add-opens。embedded Tomcat 跑在 Maven JVM 內，設在 MAVEN_OPTS。
#          注意：--add-opens 是 JDK 9+ 旗標，JDK 8 會拒絕（連 mvn 都起不來），故 8 一律略過。 ----
if [ "${JAVA_MAJOR:-8}" -ge 9 ] 2>/dev/null; then
  ADD_OPENS="--add-opens java.base/java.lang=ALL-UNNAMED"
  ADD_OPENS="$ADD_OPENS --add-opens java.base/java.lang.reflect=ALL-UNNAMED"
  ADD_OPENS="$ADD_OPENS --add-opens java.base/java.util=ALL-UNNAMED"
  ADD_OPENS="$ADD_OPENS --add-opens java.base/java.text=ALL-UNNAMED"
  ADD_OPENS="$ADD_OPENS --add-opens java.base/java.math=ALL-UNNAMED"
  export MAVEN_OPTS="${MAVEN_OPTS:-} $ADD_OPENS"
  echo "[run] JDK ${JAVA_MAJOR}：已加 --add-opens（CGLIB 相容）"
else
  echo "[run] JDK ${JAVA_MAJOR:-8}：無模組系統，略過 --add-opens"
fi

# ---- 3. 依指令執行 ----
TOMCAT_PLUGIN="org.apache.tomcat.maven:tomcat7-maven-plugin:2.2"
TOMCAT_GOAL="run-war"

# 確保 MySQL container 已啟動且可連線（最多等 30s）；若未運行則嘗試 docker compose up。
ensure_db_ready() {
  local container="accountdb-mysql"
  local max_wait=30 i

  # 1) container 是否存在且在跑？不在跑就嘗試啟動
  if ! docker ps --format '{{.Names}}' 2>/dev/null | grep -qx "$container"; then
    echo "[run] MySQL 容器 $container 未運行，嘗試啟動..."
    if [ -f "$APP_DIR/docker-compose.yml" ]; then
      docker compose -f "$APP_DIR/docker-compose.yml" up -d 2>/dev/null \
        || docker-compose -f "$APP_DIR/docker-compose.yml" up -d 2>/dev/null \
        || { echo "[run] ERROR: 無法啟動 MySQL 容器。請手動執行 docker compose up -d" >&2; return 1; }
    else
      docker start "$container" 2>/dev/null \
        || { echo "[run] ERROR: 無法啟動 $container。請手動執行 docker start $container" >&2; return 1; }
    fi
  fi

  # 2) 等待 MySQL 可接受連線（mysqladmin ping）
  echo "[run] 等待 MySQL 就緒..."
  for i in $(seq 1 "$max_wait"); do
    if docker exec "$container" mysqladmin ping -h localhost -uroot -p"${MYSQL_ROOT_PASSWORD:-rootpass}" 2>/dev/null | grep -q "alive"; then
      echo "[run] MySQL 已就緒（${i}s）"
      return 0
    fi
    sleep 1
  done
  echo "[run] WARN: 等候 MySQL 就緒逾時（${max_wait}s），仍繼續啟動 app（Spring 連線可能失敗）" >&2
  return 0
}

# 啟動 web app → 背景等它 ready 後開瀏覽器 → 用 wait 把 server 帶回前景（Ctrl-C 可停）。
run_server() {
  # Step 1: 確保 DB 先就緒
  ensure_db_ready || exit 1

  # Step 2: 檢查 port 是否被佔用；若被上次殘留 process 佔住則自動清理
  local port_pid
  port_pid="$(lsof -ti :"$APP_PORT" 2>/dev/null | head -1)"
  if [ -n "$port_pid" ]; then
    echo "[run] port ${APP_PORT} 被 PID ${port_pid} 佔用，嘗試釋放..."
    kill "$port_pid" 2>/dev/null
    sleep 2
    if lsof -ti :"$APP_PORT" >/dev/null 2>&1; then
      kill -9 "$(lsof -ti :"$APP_PORT" 2>/dev/null)" 2>/dev/null || true
      sleep 1
    fi
    if lsof -ti :"$APP_PORT" >/dev/null 2>&1; then
      echo "[run] ERROR: 無法釋放 port ${APP_PORT}。請手動停止佔用程式後再試。" >&2
      exit 1
    fi
    echo "[run] port ${APP_PORT} 已釋放"
  fi

  # Step 3: 清除 embedded Tomcat 殘留設定（讓 plugin 從頭建 clean config）
  rm -rf "$APP_DIR/target/tomcat"

  # Step 4: 驗證關鍵環境變數已 export（重開機後 .env 靠上方 set -a 載入）
  if [ -z "${DB_PASSWORD:-}" ]; then
    echo "[run] WARN: DB_PASSWORD 未設定，Hibernate 連線可能失敗" >&2
  fi
  if [ -z "${ADMIN_PASSWORD:-}" ]; then
    echo "[run] WARN: ADMIN_PASSWORD 未設定，登入功能不可用" >&2
  fi

  echo "[run] 啟動 app（embedded Tomcat 7）→ ${APP_URL}"
  echo "[run] 登入：${ADMIN_USERNAME:-admin} / ****。Ctrl-C 可停止。"

  # Step 5: 在背景起 web app（stdout/stderr 仍輸出到終端）
  mvn -Dmaven.tomcat.port="$APP_PORT" "${TOMCAT_PLUGIN}:${TOMCAT_GOAL}" &
  local srv=$!

  # Step 6: 背景等 server ready 後才開 browser（不阻塞 server 輸出）
  wait_and_open "$APP_URL" &

  # Step 7: 把 server 帶回前景：wait 阻塞直到 server 結束；Ctrl-C 會連背景一起收掉
  wait "$srv"
}

case "$CMD" in
  build)
    mvn clean package
    ;;
  test)
    mvn test
    ;;
  run)
    run_server
    ;;
  all|"")
    mvn clean package || exit $?
    run_server
    ;;
  *)
    echo "[run] 未知指令: $CMD" >&2
    echo "" >&2
    usage >&2
    exit 2
    ;;
esac
