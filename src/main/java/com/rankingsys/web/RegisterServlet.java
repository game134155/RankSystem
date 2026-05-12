package com.rankingsys.web;

import com.rankingsys.dao.AuthDao;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {
    private final AuthDao authDao = new AuthDao();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String username = trimValue(req.getParameter("username"));
        try {
            String password = requireText(req.getParameter("password"), "Password cannot be empty.");
            username = requireText(username, "Username cannot be empty.");
            int playerId = authDao.registerPlayer(username, password);
            req.setAttribute("registerOk", "Register successful. Your player_id is: " + playerId);
            req.setAttribute("playerId", String.valueOf(playerId));
        } catch (Exception e) {
            req.setAttribute("registerError", extractRootMessage(e));
            req.setAttribute("registerUsername", username);
        }
        req.getRequestDispatcher("/WEB-INF/jsp/login.jsp").forward(req, resp);
    }

    private String requireText(String raw, String message) {
        String value = trimValue(raw);
        if (value.length() == 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private String trimValue(String raw) {
        return raw == null ? "" : raw.trim();
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
        return message == null ? "Register failed." : message;
    }
}
