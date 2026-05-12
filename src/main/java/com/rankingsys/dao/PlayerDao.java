package com.rankingsys.dao;

import com.rankingsys.model.MatchRecordView;
import com.rankingsys.model.PlayerRankView;
import com.rankingsys.util.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class PlayerDao {
    public List<PlayerRankView> getRankViewsByPlayerId(int playerId) {
        String sql = "SELECT g.game_id, g.name AS game_name, ps.mmr, ps.wins, ps.losses, rt.tier_name " +
                "FROM player_stats ps " +
                "JOIN game g ON g.game_id = ps.game_id " +
                "JOIN rank_tier rt ON rt.tier_id = ps.tier_id " +
                "WHERE ps.player_id = ? " +
                "ORDER BY g.name";

        List<PlayerRankView> list = new ArrayList<PlayerRankView>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, playerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PlayerRankView view = new PlayerRankView();
                    view.setGameId(rs.getInt("game_id"));
                    view.setGameName(rs.getString("game_name"));
                    view.setMmr(rs.getInt("mmr"));
                    view.setWins(rs.getInt("wins"));
                    view.setLosses(rs.getInt("losses"));
                    view.setTierName(rs.getString("tier_name"));
                    list.add(view);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Query rank view failed.", e);
        }
        return list;
    }

    public List<MatchRecordView> getMatchRecords(int playerId, String gameName) {
        StringBuilder sql = new StringBuilder(
                "SELECT mh.match_id, g.name AS game_name, mh.start_time, mpr.result, mpr.mmr_change, mpr.mmr_before, mpr.mmr_after " +
                        "FROM match_player_result mpr " +
                        "JOIN match_history mh ON mh.match_id = mpr.match_id " +
                        "JOIN game g ON g.game_id = mh.game_id " +
                        "WHERE mpr.player_id = ? ");
        if (gameName != null && gameName.trim().length() > 0) {
            sql.append("AND g.name = ? ");
        }
        sql.append("ORDER BY mh.start_time DESC");

        List<MatchRecordView> list = new ArrayList<MatchRecordView>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            ps.setInt(1, playerId);
            if (gameName != null && gameName.trim().length() > 0) {
                ps.setString(2, gameName.trim());
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MatchRecordView record = new MatchRecordView();
                    record.setMatchId(rs.getInt("match_id"));
                    record.setGameName(rs.getString("game_name"));
                    record.setStartTime(rs.getTimestamp("start_time"));
                    record.setResult(rs.getString("result"));
                    record.setMmrChange(rs.getInt("mmr_change"));
                    record.setMmrBefore(rs.getInt("mmr_before"));
                    record.setMmrAfter(rs.getInt("mmr_after"));
                    list.add(record);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Query match history failed.", e);
        }
        return list;
    }

    public List<String> getAllGameNames() {
        String sql = "SELECT name FROM game ORDER BY name";
        List<String> games = new ArrayList<String>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                games.add(rs.getString("name"));
            }
        } catch (Exception e) {
            throw new RuntimeException("Query games failed.", e);
        }
        return games;
    }
}
