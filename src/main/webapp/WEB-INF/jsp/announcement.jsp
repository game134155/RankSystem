<%@ page import="java.util.List" %>
<%@ page import="com.rankingsys.model.AnnouncementView" %>
<%@ page import="com.rankingsys.model.User" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Announcements</title>
    <link rel="stylesheet" href="<%=request.getContextPath()%>/css/style.css">
</head>
<body>
<%
    User user = (User) session.getAttribute("user");
    List<AnnouncementView> announcements = (List<AnnouncementView>) request.getAttribute("announcements");
    String flashAnnouncementError = (String) session.getAttribute("flashAnnouncementError");
    String flashAnnouncementOk = (String) session.getAttribute("flashAnnouncementOk");
    session.removeAttribute("flashAnnouncementError");
    session.removeAttribute("flashAnnouncementOk");
%>
<div class="container">
    <div class="topbar">
        <h2>Announcements</h2>
        <div class="topbar-links">
            <% if (user == null) { %>
            <a href="<%=request.getContextPath()%>/login">Login</a>
            <% } else { %>
            <a href="<%=request.getContextPath()%>/player/dashboard">Dashboard</a>
            <% if (user.isAdmin()) { %>
            <a href="<%=request.getContextPath()%>/admin/dashboard">Admin Dashboard</a>
            <% } %>
            <a href="<%=request.getContextPath()%>/logout">Logout</a>
            <% } %>
        </div>
    </div>

    <% if (flashAnnouncementOk != null) { %>
    <p class="ok"><%=flashAnnouncementOk%></p>
    <% } %>
    <% if (flashAnnouncementError != null) { %>
    <p class="error"><%=flashAnnouncementError%></p>
    <% } %>

    <% if (user != null && user.isAdmin()) { %>
    <div class="card">
        <h3>Publish Announcement</h3>
        <form method="post" action="<%=request.getContextPath()%>/announcement">
            <label>Message</label>
            <textarea name="message" rows="5" maxlength="2000" placeholder="Write your announcement here..." required></textarea>
            <button type="submit">Publish</button>
        </form>
    </div>
    <% } %>

    <div class="card">
        <h3>Latest Announcements</h3>
        <% if (announcements == null || announcements.isEmpty()) { %>
        <p class="hint">No announcements yet.</p>
        <% } else { %>
            <% for (AnnouncementView item : announcements) { %>
            <div class="announcement-item">
                <p class="announcement-meta">
                    #<%=item.getAnnouncementId()%> by <strong><%=item.getCreatedByName()%></strong>
                    (player_id=<%=item.getCreatedBy()%>) at <%=item.getCreatedAt()%>
                </p>
                <p class="announcement-message"><%=item.getMessage()%></p>
            </div>
            <% } %>
        <% } %>
    </div>
</div>
</body>
</html>
