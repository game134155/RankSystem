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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

@WebServlet("/admin/add-random-matches")
public class AdminAddRandomMatchesServlet extends HttpServlet {
    private static final int MATCH_COUNT = 10;
    private static final int TEAM_SIZE = 5;
    private static final String[] MATCH_MODES = new String[] {"CASUAL", "NORMAL", "PEAK"};

    private final AdminDao adminDao = new AdminDao();
    private final PlayerDao playerDao = new PlayerDao();
    private final Random random = new Random();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = getSessionUser(req);
        if (user == null || !user.isAdmin()) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        try {
            List<String> usernames = queryAllUsernames();
            if (usernames.size() < TEAM_SIZE * 2) {
                throw new IllegalArgumentException("Need at least 10 players to generate 5v5 matches.");
            }

            List<String> games = playerDao.getAllGameNames();
            if (games.size() == 0) {
                throw new IllegalArgumentException("No game found. Please add a game first.");
            }

            for (int i = 0; i < MATCH_COUNT; i++) {
                Collections.shuffle(usernames, random);
                List<String> winners = new ArrayList<String>(usernames.subList(0, TEAM_SIZE));
                List<String> losers = new ArrayList<String>(usernames.subList(TEAM_SIZE, TEAM_SIZE * 2));
                String gameName = games.get(random.nextInt(games.size()));
                String matchMode = MATCH_MODES[random.nextInt(MATCH_MODES.length)];
                adminDao.addFiveVsFiveMatch(gameName, winners, losers, user.getPlayerId(), matchMode);
            }

            req.getSession().setAttribute("flashOk", "Added 10 random 5v5 matches.");
        } catch (Exception e) {
            req.getSession().setAttribute("flashError", extractRootMessage(e));
        }
        resp.sendRedirect(req.getContextPath() + "/admin/dashboard");
    }

    private List<String> queryAllUsernames() {
        List<Map<String, Object>> players = adminDao.getAllPlayers();
        List<String> usernames = new ArrayList<String>();
        for (Map<String, Object> player : players) {
            Object username = player.get("username");
            if (username != null) {
                String value = username.toString().trim();
                if (value.length() > 0) {
                    usernames.add(value);
                }
            }
        }
        return usernames;
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
        return message == null ? "Add random matches failed." : message;
    }

    private User getSessionUser(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) {
            return null;
        }
        return (User) session.getAttribute("user");
    }
}
