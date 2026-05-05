package com.rankingsys.web;

import com.rankingsys.dao.AuthDao;
import com.rankingsys.model.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    private final AuthDao authDao = new AuthDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.getRequestDispatcher("/WEB-INF/jsp/login.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String playerIdRaw = req.getParameter("playerId");
        String password = req.getParameter("password");
        try {
            int playerId = Integer.parseInt(playerIdRaw);
            User user = authDao.login(playerId, password);
            if (user == null) {
                req.setAttribute("error", "Invalid player_id or password.");
                req.getRequestDispatcher("/WEB-INF/jsp/login.jsp").forward(req, resp);
                return;
            }
            HttpSession session = req.getSession(true);
            session.setAttribute("user", user);
            if (user.isAdmin()) {
                resp.sendRedirect(req.getContextPath() + "/admin/dashboard");
            } else {
                resp.sendRedirect(req.getContextPath() + "/player/dashboard");
            }
        } catch (NumberFormatException e) {
            req.setAttribute("error", "Player ID must be a number.");
            req.getRequestDispatcher("/WEB-INF/jsp/login.jsp").forward(req, resp);
        }
    }
}
