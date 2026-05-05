package com.rankingsys.web;

import com.rankingsys.dao.PlayerDao;
import com.rankingsys.model.MatchRecordView;
import com.rankingsys.model.PlayerRankView;
import com.rankingsys.model.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;

@WebServlet("/player/dashboard")
public class PlayerDashboardServlet extends HttpServlet {
    private final PlayerDao playerDao = new PlayerDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = getSessionUser(req);
        if (user == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        int viewPlayerId = user.getPlayerId();
        if (user.isAdmin()) {
            String playerIdRaw = req.getParameter("playerId");
            if (playerIdRaw != null && playerIdRaw.trim().length() > 0) {
                try {
                    viewPlayerId = Integer.parseInt(playerIdRaw);
                } catch (NumberFormatException ignored) {
                    viewPlayerId = user.getPlayerId();
                }
            }
        }

        String gameFilter = req.getParameter("game");
        List<PlayerRankView> rankViews = playerDao.getRankViewsByPlayerId(viewPlayerId);
        List<MatchRecordView> matchRecords = playerDao.getMatchRecords(viewPlayerId, gameFilter);
        List<String> games = playerDao.getAllGameNames();

        req.setAttribute("viewPlayerId", viewPlayerId);
        req.setAttribute("gameFilter", gameFilter);
        req.setAttribute("rankViews", rankViews);
        req.setAttribute("matchRecords", matchRecords);
        req.setAttribute("games", games);
        req.getRequestDispatcher("/WEB-INF/jsp/player-dashboard.jsp").forward(req, resp);
    }

    private User getSessionUser(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) {
            return null;
        }
        return (User) session.getAttribute("user");
    }
}
