package com.rankingsys.web;

import com.rankingsys.dao.AdminDao;
import com.rankingsys.dao.PlayerDao;
import com.rankingsys.model.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@WebServlet("/admin/dashboard")
public class AdminDashboardServlet extends HttpServlet {
    private final AdminDao adminDao = new AdminDao();
    private final PlayerDao playerDao = new PlayerDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = getSessionUser(req);
        if (user == null || !user.isAdmin()) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        List<Map<String, Object>> players = adminDao.getAllPlayers();
        List<String> games = playerDao.getAllGameNames();
        req.setAttribute("players", players);
        req.setAttribute("games", games);
        req.getRequestDispatcher("/WEB-INF/jsp/admin-dashboard.jsp").forward(req, resp);
    }

    private User getSessionUser(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) {
            return null;
        }
        return (User) session.getAttribute("user");
    }
}
