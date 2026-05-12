<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Login - Ranking System</title>
    <link rel="stylesheet" href="<%=request.getContextPath()%>/css/style.css">
</head>
<body>
<div class="container small">
    <h1>Game Ranking System</h1>
    <p class="hint">Sign in to view rankings, match history and management features.</p>
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

    <form method="post" action="<%=request.getContextPath()%>/register" class="card">
        <h3>Create Player Account</h3>
        <label>Username</label>
        <input type="text" name="username" value="<%=request.getAttribute("registerUsername") == null ? "" : request.getAttribute("registerUsername")%>" required>

        <label>Password</label>
        <input type="password" name="password" required>

        <button type="submit">Register and Get Player ID</button>
    </form>
    <% if (request.getAttribute("registerOk") != null) { %>
    <p class="ok"><%=request.getAttribute("registerOk")%></p>
    <% } %>
    <% if (request.getAttribute("registerError") != null) { %>
    <p class="error"><%=request.getAttribute("registerError")%></p>
    <% } %>

    <div class="card">
        <p class="hint">Default admin account in `seed.sql`: player_id=1, password=admin123</p>
        <p class="hint">Default player account in `seed.sql` (alice): player_id=2, password=alice123</p>
    </div>
</div>
</body>
</html>
