<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Admin Dashboard</title>
    <link rel="stylesheet" href="<%=request.getContextPath()%>/css/style.css">
</head>
<body>
<%
    List<Map<String, Object>> players = (List<Map<String, Object>>) request.getAttribute("players");
    List<String> games = (List<String>) request.getAttribute("games");
    String flashError = (String) session.getAttribute("flashError");
    Map<String, String> flashForm = (Map<String, String>) session.getAttribute("flashForm");
    session.removeAttribute("flashError");
    session.removeAttribute("flashForm");
%>
<div class="container">
    <div class="topbar">
        <h2>Admin Dashboard</h2>
        <a href="<%=request.getContextPath()%>/logout">Logout</a>
    </div>

    <% if ("1".equals(request.getParameter("ok"))) { %>
    <p class="ok">Match added successfully.</p>
    <% } %>
    <% if (flashError != null) { %>
    <p class="error"><%=flashError%></p>
    <% } %>

    <div class="card">
        <h3>All Players</h3>
        <table>
            <tr><th>Player ID</th><th>Username</th><th>Role</th><th>Registered</th><th>Action</th></tr>
            <% for (Map<String, Object> row : players) { %>
            <tr>
                <td><%=row.get("playerId")%></td>
                <td><%=row.get("username")%></td>
                <td><%=((Boolean) row.get("isAdmin")) ? "Admin" : "Player"%></td>
                <td><%=row.get("regDate")%></td>
                <td>
                    <a href="<%=request.getContextPath()%>/player/dashboard?playerId=<%=row.get("playerId")%>">View</a>
                </td>
            </tr>
            <% } %>
        </table>
    </div>

    <div class="card">
        <h3>Add 5v5 Match</h3>
        <form method="post" action="<%=request.getContextPath()%>/admin/add-match">
            <label>Game</label>
            <select name="gameName" required>
                <% for (String game : games) { %>
                <option value="<%=game%>" <%= (flashForm != null && game.equals(flashForm.get("gameName"))) ? "selected" : "" %>><%=game%></option>
                <% } %>
            </select>

            <div class="grid">
                <div>
                    <h4>Winner Team (5 usernames)</h4>
                    <% for (int i = 1; i <= 5; i++) { %>
                    <input type="text" name="winner<%=i%>" placeholder="winner username <%=i%>" value="<%=flashForm == null ? "" : flashForm.get("winner" + i)%>" required>
                    <% } %>
                </div>
                <div>
                    <h4>Loser Team (5 usernames)</h4>
                    <% for (int i = 1; i <= 5; i++) { %>
                    <input type="text" name="loser<%=i%>" placeholder="loser username <%=i%>" value="<%=flashForm == null ? "" : flashForm.get("loser" + i)%>" required>
                    <% } %>
                </div>
            </div>
            <button type="submit">Create Match and Update MMR</button>
        </form>
    </div>
</div>
</body>
</html>
