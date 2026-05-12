<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Login - Ranking System</title>
    <link rel="stylesheet" href="<%=request.getContextPath()%>/css/style.css">
</head>
<body>
<div class="container small">
    <h1>Game Ranking System</h1>
    <form method="post" action="<%=request.getContextPath()%>/login" class="card">
        <label>Player ID</label>
        <input type="text" name="playerId" value="<%=request.getAttribute("playerId") == null ? "" : request.getAttribute("playerId")%>" required>

        <label>Password</label>
        <input type="password" name="password" required>

        <button type="submit">Login</button>
    </form>
    <% if (request.getAttribute("error") != null) { %>
    <p class="error"><%=request.getAttribute("error")%></p>
    <% } %>
    <p class="hint">Default admin account in seed.sql: player_id=1, password=admin123</p>
    <p class="hint">Default player account in seed.sql (alice): player_id=2, password=alice123</p>
</div>
</body>
</html>
