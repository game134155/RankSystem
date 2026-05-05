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
            throw new RuntimeException("Add 5v5 match failed.", e);
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
}
