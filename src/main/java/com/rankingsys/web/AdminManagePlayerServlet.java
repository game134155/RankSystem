package com.rankingsys.web;

import com.rankingsys.dao.AdminDao;
import com.rankingsys.model.User;
import com.rankingsys.util.PasswordUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

@WebServlet("/admin/manage-player")
public class AdminManagePlayerServlet extends HttpServlet {
    private final AdminDao adminDao = new AdminDao();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = getSessionUser(req);
        if (user == null || !user.isAdmin()) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        String action = trimValue(req.getParameter("action"));
        try {
            if ("create".equals(action)) {
                createPlayer(req);
                req.getSession().setAttribute("flashOk", "Player added successfully.");
            } else if ("update".equals(action)) {
                updatePlayer(req, user);
                req.getSession().setAttribute("flashOk", "Player updated successfully.");
            } else if ("delete".equals(action)) {
                deletePlayer(req, user);
                req.getSession().setAttribute("flashOk", "Player deleted successfully.");
            } else {
                throw new IllegalArgumentException("Unsupported action.");
            }
        } catch (Exception e) {
            req.getSession().setAttribute("flashError", extractRootMessage(e));
        }
        resp.sendRedirect(req.getContextPath() + "/admin/dashboard");
    }

    private void createPlayer(HttpServletRequest req) {
        String username = requireText(req.getParameter("username"), "Username cannot be empty.");
        String password = requireText(req.getParameter("password"), "Password cannot be empty.");
        boolean isAdmin = parseIsAdmin(req.getParameter("isAdmin"));
        adminDao.createPlayer(username, PasswordUtil.sha256(password), isAdmin);
    }

    private void updatePlayer(HttpServletRequest req, User currentUser) {
        int playerId = requirePlayerId(req.getParameter("playerId"));
        String username = requireText(req.getParameter("username"), "Username cannot be empty.");
        boolean isAdmin = parseIsAdmin(req.getParameter("isAdmin"));
        if (playerId == currentUser.getPlayerId() && !isAdmin) {
            throw new IllegalArgumentException("You cannot remove your own admin role.");
        }
        String password = trimValue(req.getParameter("password"));
        String passwordHash = password.length() == 0 ? null : PasswordUtil.sha256(password);
        adminDao.updatePlayer(playerId, username, passwordHash, isAdmin);
    }

    private void deletePlayer(HttpServletRequest req, User currentUser) {
        int playerId = requirePlayerId(req.getParameter("playerId"));
        if (playerId == currentUser.getPlayerId()) {
            throw new IllegalArgumentException("You cannot delete yourself.");
        }
        adminDao.deletePlayer(playerId, currentUser.getPlayerId());
    }

    private int requirePlayerId(String playerIdRaw) {
        try {
            return Integer.parseInt(playerIdRaw);
        } catch (Exception e) {
            throw new IllegalArgumentException("Player ID must be a number.");
        }
    }

    private boolean parseIsAdmin(String isAdminRaw) {
        return "1".equals(trimValue(isAdminRaw));
    }

    private String requireText(String raw, String message) {
        String value = trimValue(raw);
        if (value.length() == 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private String trimValue(String value) {
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
        return message == null ? "Manage player failed." : message;
    }

    private User getSessionUser(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) {
            return null;
        }
        return (User) session.getAttribute("user");
    }
}
