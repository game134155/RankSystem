# Game Ranking System (Java + JSP + JDBC)

Course project implementation for a game ranking information system.

## Tech Stack
- Backend: Java Servlet + JSP + JDBC
- Database: MySQL
- Frontend: HTML + CSS
- Server: Tomcat 9+

## Main Features
- Login with `player_id + password`
- Announcement center:
  - Admin can publish announcements
  - Players and admins can view all announcements
- Role-based access:
  - Player: view own ranking and match history
  - Admin: view all players, drill into any player page, add 5v5 match
- Add 5v5 match (admin only):
  - Input 5 winner usernames and 5 loser usernames
  - System inserts one `match_history` record
  - System inserts 10 `match_player_result` records
  - System updates each player's `mmr`, `wins/losses`, `tier_id`
  - Full transaction with rollback

## Project Structure
- `src/com/rankingsys/...`: Java source code
- `WebContent/WEB-INF/jsp`: JSP pages
- `WebContent/css/style.css`: styles
- `sql/schema.sql`: DDL
- `sql/seed.sql`: test data

## Database Setup
1. One-command reset (drop db + run `schema.sql` + run `seed.sql`):

   **Windows (PowerShell)** — run from project root:
   ```powershell
   .\scripts\reset-db.ps1
   ```
   If execution policy blocks the script:
   ```powershell
   powershell -ExecutionPolicy Bypass -File .\scripts\reset-db.ps1
   ```

   **macOS / Linux / Git Bash**:
   ```bash
   chmod +x scripts/reset-db.sh
   ./scripts/reset-db.sh
   ```

2. Optional: override connection by env vars:

   PowerShell:
   ```powershell
   $env:DB_HOST='localhost'; $env:DB_PORT='3306'; $env:DB_USER='root'; $env:DB_PASS='12345678'; $env:DB_NAME='ranking_system'; .\scripts\reset-db.ps1
   ```

   Bash:
   ```bash
   DB_HOST=localhost DB_PORT=3306 DB_USER=root DB_PASS=12345678 DB_NAME=ranking_system ./scripts/reset-db.sh
   ```

Default admin account (after seed):
- `player_id = 1`
- password: `admin123`

## Important Config
Edit DB credentials in:
- `src/com/rankingsys/util/DBUtil.java`

Default now:
- URL: `jdbc:mysql://localhost:3306/ranking_system?useSSL=false&serverTimezone=UTC`
- user: `root`
- password: `12345678`

## One-command Run (Maven)
1. Ensure MySQL is running and initialize data:
   ```bash
   ./scripts/reset-db.sh
   ```
2. Start web app directly with Maven:
   ```bash
   mvn jetty:run
   ```
3. Open in browser:
   - `http://localhost:8080/`

## Tomcat Deployment (Optional)
1. Create a Dynamic Web Project in Eclipse/IDEA and import this folder.
2. Use Tomcat 10+ runtime.
3. Build WAR and deploy:
   ```bash
   mvn clean package
   ```
4. Open:
   - `http://localhost:8080/<your-context>/`

## Notes
- Password is stored as SHA-256 hash.
- The project avoids ORM and heavy MVC frameworks as required.
根据 player_id 精确查一行用户（如果有）。
取出库里的 password_hash。
对输入密码做 SHA-256，再和库中哈希做字符串比较。
不相等就返回 null（登录失败）。
通过哈希对比的办法，我们更有效的绕过了sql注入攻击