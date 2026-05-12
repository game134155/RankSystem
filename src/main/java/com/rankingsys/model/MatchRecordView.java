package com.rankingsys.model;

import java.sql.Timestamp;

public class MatchRecordView {
    private int matchId;
    private String gameName;
    private Timestamp startTime;
    private String result;
    private int mmrChange;
    private int mmrBefore;
    private int mmrAfter;
    /** CASUAL / NORMAL / PEAK (or legacy values from older DB). */
    private String matchType;

    public int getMatchId() {
        return matchId;
    }

    public void setMatchId(int matchId) {
        this.matchId = matchId;
    }

    public String getGameName() {
        return gameName;
    }

    public void setGameName(String gameName) {
        this.gameName = gameName;
    }

    public Timestamp getStartTime() {
        return startTime;
    }

    public void setStartTime(Timestamp startTime) {
        this.startTime = startTime;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public int getMmrChange() {
        return mmrChange;
    }

    public void setMmrChange(int mmrChange) {
        this.mmrChange = mmrChange;
    }

    public int getMmrBefore() {
        return mmrBefore;
    }

    public void setMmrBefore(int mmrBefore) {
        this.mmrBefore = mmrBefore;
    }

    public int getMmrAfter() {
        return mmrAfter;
    }

    public void setMmrAfter(int mmrAfter) {
        this.mmrAfter = mmrAfter;
    }

    public String getMatchType() {
        return matchType;
    }

    public void setMatchType(String matchType) {
        this.matchType = matchType;
    }
}
