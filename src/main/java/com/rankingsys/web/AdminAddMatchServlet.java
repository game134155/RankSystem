package com.rankingsys.web;

import com.rankingsys.dao.AdminDao;
import com.rankingsys.model.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/admin/add-match")
public class AdminAddMatchServlet extends HttpServlet {
    private final AdminDao adminDao = new AdminDao();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = getSessionUser(req);
        if (user == null || !user.isAdmin()) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        String gameName = req.getParameter("gameName");
        List<String> winners = new ArrayList<String>();
        List<String> losers = new ArrayList<String>();
        for (int i = 1; i <= 5; i++) {
            winners.add(req.getParameter("winner" + i));
            losers.add(req.getParameter("loser" + i));
        }

        try {
            adminDao.addFiveVsFiveMatch(gameName, winners, losers, user.getPlayerId());
            resp.sendRedirect(req.getContextPath() + "/admin/dashboard?ok=1");
        } catch (Exception e) {
            req.getSession().setAttribute("flashError", e.getMessage());
            resp.sendRedirect(req.getContextPath() + "/admin/dashboard");
        }
    }

    private User getSessionUser(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) {
            return null;
        }
        return (User) session.getAttribute("user");
    }
}
