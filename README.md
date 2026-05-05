# Game Ranking System (Java + JSP + JDBC)

Course project implementation for a game ranking information system.

## Tech Stack
- Backend: Java Servlet + JSP + JDBC
- Database: MySQL
- Frontend: HTML + CSS
- Server: Tomcat 9+

## Main Features
- Login with `player_id + password`
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
1. Run `sql/schema.sql`
2. Run `sql/seed.sql`

Default admin account:
- `player_id = 1`
- password: `admin123`

## Important Config
Edit DB credentials in:
- `src/com/rankingsys/util/DBUtil.java`

Default now:
- URL: `jdbc:mysql://localhost:3306/ranking_system?useSSL=false&serverTimezone=UTC`
- user: `root`
- password: `123456`

## Tomcat Deployment
1. Create a Dynamic Web Project in Eclipse/IDEA and import this folder.
2. Add MySQL Connector/J (`mysql-connector-j-8.x.jar`) into project `WEB-INF/lib`.
3. Use Tomcat 9+ runtime.
4. Run project and open:
   - `http://localhost:8080/<your-context>/`

## Notes
- Password is stored as SHA-256 hash.
- The project avoids ORM and heavy MVC frameworks as required.
