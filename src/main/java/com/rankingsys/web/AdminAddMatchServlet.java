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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        String matchMode = req.getParameter("matchMode");
        List<String> winners = new ArrayList<String>();
        List<String> losers = new ArrayList<String>();
        for (int i = 1; i <= 5; i++) {
            winners.add(req.getParameter("winner" + i));
            losers.add(req.getParameter("loser" + i));
        }

        try {
            adminDao.addFiveVsFiveMatch(gameName, winners, losers, user.getPlayerId(), matchMode);
            req.getSession().removeAttribute("flashForm");
            resp.sendRedirect(req.getContextPath() + "/admin/dashboard?ok=1");
        } catch (Exception e) {
            HttpSession session = req.getSession();
            session.setAttribute("flashError", extractRootMessage(e));
            session.setAttribute("flashForm", buildFlashForm(gameName, matchMode, winners, losers));
            resp.sendRedirect(req.getContextPath() + "/admin/dashboard");
        }
    }

    private Map<String, String> buildFlashForm(String gameName, String matchMode, List<String> winners, List<String> losers) {
        Map<String, String> form = new HashMap<String, String>();
        form.put("gameName", gameName == null ? "" : gameName);
        form.put("matchMode", matchMode == null || matchMode.trim().length() == 0 ? "NORMAL" : matchMode.trim());
        for (int i = 1; i <= 5; i++) {
            form.put("winner" + i, safeValue(winners.get(i - 1)));
            form.put("loser" + i, safeValue(losers.get(i - 1)));
        }
        return form;
    }

    private String safeValue(String value) {
        return value == null ? "" : value.trim();
    }

    private String extractRootMessage(Exception e) {
        Throwable cursor = e;
        String message = null;
        while (cursor != null) {
            if (cursor.getMessage() != null && cursor.getMessage().trim().length() > 0) {
                message = cursor.getMessage();
            }
            cursor = cursor.getCause();
        }
        return message == null ? "Add 5v5 match failed." : message;
    }

    private User getSessionUser(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) {
            return null;
        }
        return (User) session.getAttribute("user");
    }
}
