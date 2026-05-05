<%@ page import="java.util.List" %>
<%@ page import="com.rankingsys.model.PlayerRankView" %>
<%@ page import="com.rankingsys.model.MatchRecordView" %>
<%@ page import="com.rankingsys.model.User" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Player Dashboard</title>
    <link rel="stylesheet" href="<%=request.getContextPath()%>/css/style.css">
</head>
<body>
<%
    User user = (User) session.getAttribute("user");
    Integer viewPlayerId = (Integer) request.getAttribute("viewPlayerId");
    List<PlayerRankView> rankViews = (List<PlayerRankView>) request.getAttribute("rankViews");
    List<MatchRecordView> matchRecords = (List<MatchRecordView>) request.getAttribute("matchRecords");
    List<String> games = (List<String>) request.getAttribute("games");
    String gameFilter = (String) request.getAttribute("gameFilter");
%>
<div class="container">
    <div class="topbar">
        <h2>Welcome, <%=user.getUsername()%> (<%=user.isAdmin() ? "Admin" : "Player"%>)</h2>
        <a href="<%=request.getContextPath()%>/logout">Logout</a>
    </div>

    <div class="card">
        <h3>Player Info</h3>
        <p>Viewing Player ID: <strong><%=viewPlayerId%></strong></p>
    </div>

    <div class="card">
        <h3>Current Ranking</h3>
        <table>
            <tr><th>Game</th><th>Tier</th><th>MMR</th><th>Wins</th><th>Losses</th></tr>
            <% for (PlayerRankView item : rankViews) { %>
            <tr>
                <td><%=item.getGameName()%></td>
                <td><%=item.getTierName()%></td>
                <td><%=item.getMmr()%></td>
                <td><%=item.getWins()%></td>
                <td><%=item.getLosses()%></td>
            </tr>
            <% } %>
        </table>
    </div>

    <div class="card">
        <h3>Match History</h3>
        <form method="get" action="<%=request.getContextPath()%>/player/dashboard" class="inline-form">
            <% if (user.isAdmin()) { %>
            <input type="hidden" name="playerId" value="<%=viewPlayerId%>">
            <% } %>
            <select name="game">
                <option value="">All Games</option>
                <% for (String game : games) { %>
                <option value="<%=game%>" <%=game.equals(gameFilter) ? "selected" : ""%>><%=game%></option>
                <% } %>
            </select>
            <button type="submit">Filter</button>
        </form>

        <table>
            <tr><th>Match ID</th><th>Game</th><th>Time</th><th>Result</th><th>MMR Change</th><th>Before</th><th>After</th></tr>
            <% for (MatchRecordView item : matchRecords) { %>
            <tr>
                <td><%=item.getMatchId()%></td>
                <td><%=item.getGameName()%></td>
                <td><%=item.getStartTime()%></td>
                <td><%=item.getResult()%></td>
                <td><%=item.getMmrChange()%></td>
                <td><%=item.getMmrBefore()%></td>
                <td><%=item.getMmrAfter()%></td>
            </tr>
            <% } %>
        </table>
    </div>

    <% if (!user.isAdmin()) { %>
    <div class="card">
        <h3>Add Match</h3>
        <button onclick="alert('No permission: only admin can add matches.')">Add 5v5 Match</button>
    </div>
    <% } %>

    <% if (user.isAdmin()) { %>
    <div class="card">
        <a href="<%=request.getContextPath()%>/admin/dashboard">Back to Admin Dashboard</a>
    </div>
    <% } %>
</div>
</body>
</html>
