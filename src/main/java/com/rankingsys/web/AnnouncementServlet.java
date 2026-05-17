package com.rankingsys.web;

import com.rankingsys.dao.AnnouncementDao;
import com.rankingsys.model.AnnouncementView;
import com.rankingsys.model.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;

@WebServlet("/announcement")
public class AnnouncementServlet extends HttpServlet {
    private final AnnouncementDao announcementDao = new AnnouncementDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        List<AnnouncementView> announcements = announcementDao.getLatestAnnouncements(50);
        req.setAttribute("announcements", announcements);
        req.getRequestDispatcher("/WEB-INF/jsp/announcement.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = getSessionUser(req);
        if (user == null || !user.isAdmin()) {
            req.getSession(true).setAttribute("flashAnnouncementError", "Only admin can publish announcements.");
            resp.sendRedirect(req.getContextPath() + "/announcement");
            return;
        }

        String message = trim(req.getParameter("message"));
        if (message.length() == 0) {
            req.getSession(true).setAttribute("flashAnnouncementError", "Announcement message cannot be empty.");
            resp.sendRedirect(req.getContextPath() + "/announcement");
            return;
        }
        if (message.length() > 2000) {
            req.getSession(true).setAttribute("flashAnnouncementError", "Announcement is too long (max 2000 chars).");
            resp.sendRedirect(req.getContextPath() + "/announcement");
            return;
        }

        announcementDao.createAnnouncement(message, user.getPlayerId());
        req.getSession(true).setAttribute("flashAnnouncementOk", "Announcement published.");
        resp.sendRedirect(req.getContextPath() + "/announcement");
    }

    private User getSessionUser(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) {
            return null;
        }
        return (User) session.getAttribute("user");
    }

    private String trim(String raw) {
        return raw == null ? "" : raw.trim();
    }
}
