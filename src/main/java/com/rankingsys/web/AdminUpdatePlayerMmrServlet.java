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

@WebServlet("/admin/update-player-mmr")
public class AdminUpdatePlayerMmrServlet extends HttpServlet {
    private final AdminDao adminDao = new AdminDao();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = getSessionUser(req);
        if (user == null || !user.isAdmin()) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        int playerId;
        int gameId;
        int mmr;
        try {
            playerId = Integer.parseInt(req.getParameter("playerId"));
            gameId = Integer.parseInt(req.getParameter("gameId"));
            mmr = Integer.parseInt(req.getParameter("mmr"));
            if (mmr < 0) {
                throw new IllegalArgumentException("MMR cannot be negative.");
            }
        } catch (Exception e) {
            req.getSession().setAttribute("flashPlayerError", "Invalid player/game/MMR input.");
            resp.sendRedirect(req.getContextPath() + "/player/dashboard?playerId=" + req.getParameter("playerId"));
            return;
        }

        try {
            adminDao.updatePlayerMmr(playerId, gameId, mmr);
            req.getSession().setAttribute("flashPlayerOk", "MMR updated successfully.");
        } catch (Exception e) {
            req.getSession().setAttribute("flashPlayerError", extractRootMessage(e));
        }
        resp.sendRedirect(req.getContextPath() + "/player/dashboard?playerId=" + playerId);
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
        return message == null ? "Update MMR failed." : message;
    }

    private User getSessionUser(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) {
            return null;
        }
        return (User) session.getAttribute("user");
    }
}
