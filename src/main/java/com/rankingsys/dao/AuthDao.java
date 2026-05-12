package com.rankingsys.dao;

import com.rankingsys.model.User;
import com.rankingsys.util.DBUtil;
import com.rankingsys.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

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

    public int registerPlayer(String username, String rawPassword) {
        String insertPlayerSql = "INSERT INTO player(username, password_hash, is_admin) VALUES(?, ?, 0)";
        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(insertPlayerSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, username);
                ps.setString(2, PasswordUtil.sha256(rawPassword));
                ps.executeUpdate();

                int playerId;
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (!rs.next()) {
                        throw new RuntimeException("Cannot get generated player_id.");
                    }
                    playerId = rs.getInt(1);
                }
                initializePlayerStatsForAllGames(conn, playerId);
                conn.commit();
                return playerId;
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            throw buildRegisterException(e);
        }
    }

    private void initializePlayerStatsForAllGames(Connection conn, int playerId) throws Exception {
        String sql = "SELECT game_id, default_mmr FROM game";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int gameId = rs.getInt("game_id");
                int defaultMmr = rs.getInt("default_mmr");
                int tierId = queryTierIdByMmr(conn, gameId, defaultMmr);

                String insertStatsSql = "INSERT INTO player_stats(player_id, game_id, tier_id, mmr, wins, losses) " +
                        "VALUES(?, ?, ?, ?, 0, 0)";
                try (PreparedStatement insertPs = conn.prepareStatement(insertStatsSql)) {
                    insertPs.setInt(1, playerId);
                    insertPs.setInt(2, gameId);
                    insertPs.setInt(3, tierId);
                    insertPs.setInt(4, defaultMmr);
                    insertPs.executeUpdate();
                }
            }
        }
    }

    private int queryTierIdByMmr(Connection conn, int gameId, int mmr) throws Exception {
        String sql = "SELECT tier_id FROM rank_tier WHERE game_id = ? AND min_mmr <= ? ORDER BY min_mmr DESC LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, gameId);
            ps.setInt(2, mmr);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new RuntimeException("No rank tier configured for game: " + gameId);
                }
                return rs.getInt("tier_id");
            }
        }
    }

    private RuntimeException buildRegisterException(Exception e) {
        String rootMessage = extractRootMessage(e);
        String lowerMessage = rootMessage == null ? "" : rootMessage.toLowerCase();
        if (lowerMessage.contains("duplicate") && lowerMessage.contains("username")) {
            return new RuntimeException("Username already exists.", e);
        }
        if (rootMessage == null || rootMessage.trim().length() == 0) {
            return new RuntimeException("Register failed.", e);
        }
        return new RuntimeException(rootMessage, e);
    }

    private String extractRootMessage(Throwable e) {
        Throwable cursor = e;
        String message = null;
        while (cursor != null) {
            if (cursor.getMessage() != null && cursor.getMessage().trim().length() > 0) {
                message = cursor.getMessage();
            }
            cursor = cursor.getCause();
        }
        return message;
    }
}
