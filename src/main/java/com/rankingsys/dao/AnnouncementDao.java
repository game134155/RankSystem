package com.rankingsys.dao;

import com.rankingsys.model.AnnouncementView;
import com.rankingsys.util.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class AnnouncementDao {
    public List<AnnouncementView> getLatestAnnouncements(int limit) {
        int safeLimit = limit <= 0 ? 20 : Math.min(limit, 200);
        String sql = "SELECT a.announcement_id, a.message, a.created_at, a.created_by, p.username AS created_by_name " +
                "FROM announcement a " +
                "JOIN player p ON p.player_id = a.created_by " +
                "ORDER BY a.created_at DESC, a.announcement_id DESC " +
                "LIMIT ?";
        List<AnnouncementView> rows = new ArrayList<AnnouncementView>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, safeLimit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AnnouncementView item = new AnnouncementView();
                    item.setAnnouncementId(rs.getInt("announcement_id"));
                    item.setMessage(rs.getString("message"));
                    item.setCreatedAt(rs.getTimestamp("created_at"));
                    item.setCreatedBy(rs.getInt("created_by"));
                    item.setCreatedByName(rs.getString("created_by_name"));
                    rows.add(item);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Query announcements failed.", e);
        }
        return rows;
    }

    public void createAnnouncement(String message, int adminId) {
        String sql = "INSERT INTO announcement(message, created_by) VALUES(?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, message);
            ps.setInt(2, adminId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Create announcement failed.", e);
        }
    }
}
