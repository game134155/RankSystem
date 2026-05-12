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

@WebServlet("/admin/add-game")
public class AdminAddGameServlet extends HttpServlet {
    private final AdminDao adminDao = new AdminDao();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = getSessionUser(req);
        if (user == null || !user.isAdmin()) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        try {
            String gameName = requireText(req.getParameter("gameName"), "Game name cannot be empty.");
            int defaultMmr = parseInt(req.getParameter("defaultMmr"), "Default MMR must be a number.");
            if (defaultMmr < 0) {
                throw new IllegalArgumentException("Default MMR cannot be negative.");
            }

            String tierLines = requireText(req.getParameter("tierLines"), "Tier config cannot be empty.");
            List<String> tierNames = new ArrayList<String>();
            List<Integer> minMmrs = new ArrayList<Integer>();
            parseTierLines(tierLines, tierNames, minMmrs);

            adminDao.createGameWithTiers(gameName, defaultMmr, tierNames, minMmrs);
            req.getSession().setAttribute("flashOk", "Game added successfully.");
        } catch (Exception e) {
            req.getSession().setAttribute("flashError", extractRootMessage(e));
        }
        resp.sendRedirect(req.getContextPath() + "/admin/dashboard");
    }

    private void parseTierLines(String tierLines, List<String> tierNames, List<Integer> minMmrs) {
        String[] lines = tierLines.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line == null ? "" : line.trim();
            if (trimmed.length() == 0) {
                continue;
            }

            String[] parts = trimmed.split(",");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Each tier line must be: tier_name,min_mmr");
            }

            String tierName = requireText(parts[0], "Tier name cannot be empty.");
            int minMmr = parseInt(parts[1], "Tier min_mmr must be a number.");
            if (minMmr < 0) {
                throw new IllegalArgumentException("Tier min_mmr cannot be negative.");
            }

            tierNames.add(tierName);
            minMmrs.add(minMmr);
        }

        if (tierNames.size() == 0) {
            throw new IllegalArgumentException("Tier config cannot be empty.");
        }
    }

    private int parseInt(String value, String message) {
        try {
            return Integer.parseInt(requireText(value, message));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(message);
        }
    }

    private String requireText(String value, String message) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.length() == 0) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
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
        return message == null ? "Add game failed." : message;
    }

    private User getSessionUser(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) {
            return null;
        }
        return (User) session.getAttribute("user");
    }
}
