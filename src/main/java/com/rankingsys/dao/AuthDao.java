package com.rankingsys.dao;

import com.rankingsys.model.User;
import com.rankingsys.util.DBUtil;
import com.rankingsys.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AuthDao {
    public User login(int playerId, String rawPassword) {
        String sql = "SELECT player_id, username, is_admin, password_hash FROM player WHERE player_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, playerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                String expectedHash = rs.getString("password_hash");
                String actualHash = PasswordUtil.sha256(rawPassword);
                if (!expectedHash.equalsIgnoreCase(actualHash)) {
                    return null;
                }
                User user = new User();
                user.setPlayerId(rs.getInt("player_id"));
                user.setUsername(rs.getString("username"));
                user.setAdmin(rs.getBoolean("is_admin"));
                return user;
            }
        } catch (Exception e) {
            throw new RuntimeException("Login failed.", e);
        }
    }
}
