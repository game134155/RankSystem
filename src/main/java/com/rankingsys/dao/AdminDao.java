package com.rankingsys.dao;

import com.rankingsys.util.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AdminDao {
    public List<Map<String, Object>> getAllPlayers() {
        String sql = "SELECT player_id, username, is_admin, reg_date FROM player ORDER BY player_id";
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new HashMap<String, Object>();
                row.put("playerId", rs.getInt("player_id"));
                row.put("username", rs.getString("username"));
                row.put("isAdmin", rs.getBoolean("is_admin"));
                row.put("regDate", rs.getTimestamp("reg_date"));
                rows.add(row);
            }
        } catch (Exception e) {
            throw new RuntimeException("Query all players failed.", e);
        }
        return rows;
    }

    public void createPlayer(String username, String passwordHash, boolean isAdmin) {
        String sql = "INSERT INTO player(username, password_hash, is_admin) VALUES(?, ?, ?)";
        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, username);
                ps.setString(2, passwordHash);
                ps.setBoolean(3, isAdmin);
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
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            throw buildPlayerManageException("Create player failed.", e);
        }
    }

    public void updatePlayer(int playerId, String username, String passwordHash, boolean isAdmin) {
        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (!playerExists(conn, playerId)) {
                    throw new IllegalArgumentException("Player not found.");
                }
                if (!isAdmin && queryIsAdmin(conn, playerId) && countAdmins(conn) <= 1) {
                    throw new IllegalArgumentException("At least one admin must remain.");
                }
                if (passwordHash == null) {
                    updatePlayerBasic(conn, playerId, username, isAdmin);
                } else {
                    updatePlayerWithPassword(conn, playerId, username, passwordHash, isAdmin);
                }
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            throw buildPlayerManageException("Update player failed.", e);
        }
    }

    public void deletePlayer(int playerId, int actingAdminId) {
        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (!playerExists(conn, playerId)) {
                    throw new IllegalArgumentException("Player not found.");
                }
                if (queryIsAdmin(conn, playerId) && countAdmins(conn) <= 1) {
                    throw new IllegalArgumentException("At least one admin must remain.");
                }

                String reassignMatchCreatorSql = "UPDATE match_history SET created_by = ? WHERE created_by = ?";
                try (PreparedStatement ps = conn.prepareStatement(reassignMatchCreatorSql)) {
                    ps.setInt(1, actingAdminId);
                    ps.setInt(2, playerId);
                    ps.executeUpdate();
                }

                String deleteMatchResultSql = "DELETE FROM match_player_result WHERE player_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(deleteMatchResultSql)) {
                    ps.setInt(1, playerId);
                    ps.executeUpdate();
                }

                String deleteStatsSql = "DELETE FROM player_stats WHERE player_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(deleteStatsSql)) {
                    ps.setInt(1, playerId);
                    ps.executeUpdate();
                }

                String deletePlayerSql = "DELETE FROM player WHERE player_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(deletePlayerSql)) {
                    ps.setInt(1, playerId);
                    ps.executeUpdate();
                }
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            throw buildPlayerManageException("Delete player failed.", e);
        }
    }

    public void updatePlayerMmr(int playerId, int gameId, int mmr) {
        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String lockStatsSql = "SELECT player_id FROM player_stats WHERE player_id = ? AND game_id = ? FOR UPDATE";
                try (PreparedStatement ps = conn.prepareStatement(lockStatsSql)) {
                    ps.setInt(1, playerId);
                    ps.setInt(2, gameId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalArgumentException("Player has no stats for this game.");
                        }
                    }
                }

                int tierId = queryTierIdByMmr(conn, gameId, mmr);
                String updateSql = "UPDATE player_stats SET mmr = ?, tier_id = ? WHERE player_id = ? AND game_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setInt(1, mmr);
                    ps.setInt(2, tierId);
                    ps.setInt(3, playerId);
                    ps.setInt(4, gameId);
                    ps.executeUpdate();
                }
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            String message = e.getMessage();
            if (message == null || message.trim().length() == 0) {
                message = "Update player MMR failed.";
            }
            throw new RuntimeException(message, e);
        }
    }

    public void createGameWithTiers(String gameName, int defaultMmr, List<String> tierNames, List<Integer> minMmrs) {
        if (gameName == null || gameName.trim().length() == 0) {
            throw new RuntimeException("Game name cannot be empty.");
        }
        if (defaultMmr < 0) {
            throw new RuntimeException("Default MMR cannot be negative.");
        }
        if (tierNames == null || minMmrs == null || tierNames.size() == 0 || tierNames.size() != minMmrs.size()) {
            throw new RuntimeException("Tier config is invalid.");
        }

        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int gameId = insertGame(conn, gameName.trim(), defaultMmr);
                insertRankTiers(conn, gameId, tierNames, minMmrs);

                int defaultTierId = queryTierIdByMmr(conn, gameId, defaultMmr);
                String initStatsSql = "INSERT INTO player_stats(player_id, game_id, tier_id, mmr, wins, losses) " +
                        "SELECT player_id, ?, ?, ?, 0, 0 FROM player";
                try (PreparedStatement ps = conn.prepareStatement(initStatsSql)) {
                    ps.setInt(1, gameId);
                    ps.setInt(2, defaultTierId);
                    ps.setInt(3, defaultMmr);
                    ps.executeUpdate();
                }
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            throw buildGameManageException("Create game failed.", e);
        }
    }

    public void addFiveVsFiveMatch(String gameName, List<String> winners, List<String> losers, int adminId) {
        if (winners.size() != 5 || losers.size() != 5) {
            throw new IllegalArgumentException("Winners and losers must each have 5 players.");
        }
        validateTeams(winners, losers);
        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int gameId = queryGameId(conn, gameName);

                List<Integer> winnerIds = queryPlayerIds(conn, winners);
                List<Integer> loserIds = queryPlayerIds(conn, losers);
                if (winnerIds.size() != 5 || loserIds.size() != 5) {
                    throw new IllegalArgumentException("Some usernames do not exist.");
                }

                Map<Integer, Integer> mmrByPlayer = queryMmr(conn, gameId, winnerIds, loserIds);
                int avgWin = average(mmrByPlayer, winnerIds);
                int avgLose = average(mmrByPlayer, loserIds);
                int delta = (avgLose - avgWin) / 10 + 10;
                int winnerChange = Math.max(3, delta);
                int loserChange = -Math.max(3, delta);

                int matchId = insertMatch(conn, gameId, adminId);
                for (int playerId : winnerIds) {
                    updatePlayerAfterMatch(conn, matchId, gameId, playerId, "WIN", winnerChange, mmrByPlayer.get(playerId));
                }
                for (int playerId : loserIds) {
                    updatePlayerAfterMatch(conn, matchId, gameId, playerId, "LOSE", loserChange, mmrByPlayer.get(playerId));
                }
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            String message = e.getMessage();
            if (message == null || message.trim().length() == 0) {
                message = "Add 5v5 match failed.";
            }
            throw new RuntimeException(message, e);
        }
    }

    private void validateTeams(List<String> winners, List<String> losers) {
        Set<String> allNames = new HashSet<String>();
        for (String name : winners) {
            if (name == null || name.trim().length() == 0) {
                throw new IllegalArgumentException("Winner usernames cannot be empty.");
            }
            allNames.add(name.trim().toLowerCase());
        }
        for (String name : losers) {
            if (name == null || name.trim().length() == 0) {
                throw new IllegalArgumentException("Loser usernames cannot be empty.");
            }
            allNames.add(name.trim().toLowerCase());
        }
        if (allNames.size() != 10) {
            throw new IllegalArgumentException("All 10 usernames must be unique.");
        }
    }

    private int queryGameId(Connection conn, String gameName) throws Exception {
        String sql = "SELECT game_id FROM game WHERE name = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, gameName);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("Game not found: " + gameName);
                }
                return rs.getInt("game_id");
            }
        }
    }

    private List<Integer> queryPlayerIds(Connection conn, List<String> usernames) throws Exception {
        String sql = "SELECT player_id FROM player WHERE username = ?";
        List<Integer> ids = new ArrayList<Integer>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (String username : usernames) {
                ps.setString(1, username.trim());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        ids.add(rs.getInt("player_id"));
                    }
                }
            }
        }
        return ids;
    }

    private Map<Integer, Integer> queryMmr(Connection conn, int gameId, List<Integer> winners, List<Integer> losers) throws Exception {
        Map<Integer, Integer> mmrByPlayer = new HashMap<Integer, Integer>();
        String sql = "SELECT player_id, mmr FROM player_stats WHERE game_id = ? AND player_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int id : winners) {
                ps.setInt(1, gameId);
                ps.setInt(2, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalArgumentException("Player " + id + " has not registered game.");
                    }
                    mmrByPlayer.put(id, rs.getInt("mmr"));
                }
            }
            for (int id : losers) {
                ps.setInt(1, gameId);
                ps.setInt(2, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalArgumentException("Player " + id + " has not registered game.");
                    }
                    mmrByPlayer.put(id, rs.getInt("mmr"));
                }
            }
        }
        return mmrByPlayer;
    }

    private int average(Map<Integer, Integer> mmrByPlayer, List<Integer> players) {
        int total = 0;
        for (int id : players) {
            total += mmrByPlayer.get(id);
        }
        return total / players.size();
    }

    private int insertMatch(Connection conn, int gameId, int adminId) throws Exception {
        String sql = "INSERT INTO match_history(game_id, match_type, created_by) VALUES(?, '5v5', ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, gameId);
            ps.setInt(2, adminId);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
                throw new RuntimeException("Cannot get generated match_id.");
            }
        }
    }

    private void updatePlayerAfterMatch(Connection conn, int matchId, int gameId, int playerId,
                                        String result, int mmrChange, int mmrBefore) throws Exception {
        int mmrAfter = mmrBefore + mmrChange;
        int newTierId = queryTierIdByMmr(conn, gameId, mmrAfter);

        String insertResultSql = "INSERT INTO match_player_result(match_id, player_id, result, mmr_change, mmr_before, mmr_after) " +
                "VALUES(?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(insertResultSql)) {
            ps.setInt(1, matchId);
            ps.setInt(2, playerId);
            ps.setString(3, result);
            ps.setInt(4, mmrChange);
            ps.setInt(5, mmrBefore);
            ps.setInt(6, mmrAfter);
            ps.executeUpdate();
        }

        String updateStatsSql = "UPDATE player_stats SET mmr = ?, wins = wins + ?, losses = losses + ?, tier_id = ? " +
                "WHERE player_id = ? AND game_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(updateStatsSql)) {
            ps.setInt(1, mmrAfter);
            ps.setInt(2, "WIN".equals(result) ? 1 : 0);
            ps.setInt(3, "LOSE".equals(result) ? 1 : 0);
            ps.setInt(4, newTierId);
            ps.setInt(5, playerId);
            ps.setInt(6, gameId);
            ps.executeUpdate();
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

    private int insertGame(Connection conn, String gameName, int defaultMmr) throws Exception {
        String sql = "INSERT INTO game(name, default_mmr) VALUES(?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, gameName);
            ps.setInt(2, defaultMmr);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (!rs.next()) {
                    throw new RuntimeException("Cannot get generated game_id.");
                }
                return rs.getInt(1);
            }
        }
    }

    private void insertRankTiers(Connection conn, int gameId, List<String> tierNames, List<Integer> minMmrs) throws Exception {
        Set<String> uniqueTierNames = new HashSet<String>();
        String sql = "INSERT INTO rank_tier(game_id, tier_name, min_mmr) VALUES(?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < tierNames.size(); i++) {
                String tierName = tierNames.get(i) == null ? "" : tierNames.get(i).trim();
                int minMmr = minMmrs.get(i);
                if (tierName.length() == 0) {
                    throw new RuntimeException("Tier name cannot be empty.");
                }
                if (minMmr < 0) {
                    throw new RuntimeException("Tier min_mmr cannot be negative.");
                }
                String lowerName = tierName.toLowerCase();
                if (uniqueTierNames.contains(lowerName)) {
                    throw new RuntimeException("Tier names must be unique.");
                }
                uniqueTierNames.add(lowerName);

                ps.setInt(1, gameId);
                ps.setString(2, tierName);
                ps.setInt(3, minMmr);
                ps.executeUpdate();
            }
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

    private void updatePlayerBasic(Connection conn, int playerId, String username, boolean isAdmin) throws Exception {
        String sql = "UPDATE player SET username = ?, is_admin = ? WHERE player_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setBoolean(2, isAdmin);
            ps.setInt(3, playerId);
            ps.executeUpdate();
        }
    }

    private void updatePlayerWithPassword(Connection conn, int playerId, String username,
                                          String passwordHash, boolean isAdmin) throws Exception {
        String sql = "UPDATE player SET username = ?, password_hash = ?, is_admin = ? WHERE player_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, passwordHash);
            ps.setBoolean(3, isAdmin);
            ps.setInt(4, playerId);
            ps.executeUpdate();
        }
    }

    private boolean playerExists(Connection conn, int playerId) throws Exception {
        String sql = "SELECT player_id FROM player WHERE player_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, playerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private boolean queryIsAdmin(Connection conn, int playerId) throws Exception {
        String sql = "SELECT is_admin FROM player WHERE player_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, playerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }
                return rs.getBoolean("is_admin");
            }
        }
    }

    private int countAdmins(Connection conn) throws Exception {
        String sql = "SELECT COUNT(*) AS total FROM player WHERE is_admin = 1";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                return 0;
            }
            return rs.getInt("total");
        }
    }

    private RuntimeException buildPlayerManageException(String fallbackMessage, Exception e) {
        String rootMessage = extractRootMessage(e);
        String lowerMessage = rootMessage == null ? "" : rootMessage.toLowerCase();
        if (lowerMessage.contains("duplicate") && lowerMessage.contains("username")) {
            return new RuntimeException("Username already exists.", e);
        }
        if (rootMessage == null || rootMessage.trim().length() == 0) {
            return new RuntimeException(fallbackMessage, e);
        }
        return new RuntimeException(rootMessage, e);
    }

    private RuntimeException buildGameManageException(String fallbackMessage, Exception e) {
        String rootMessage = extractRootMessage(e);
        String lowerMessage = rootMessage == null ? "" : rootMessage.toLowerCase();
        if (lowerMessage.contains("duplicate") && lowerMessage.contains("game")) {
            return new RuntimeException("Game name already exists.", e);
        }
        if (rootMessage == null || rootMessage.trim().length() == 0) {
            return new RuntimeException(fallbackMessage, e);
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
