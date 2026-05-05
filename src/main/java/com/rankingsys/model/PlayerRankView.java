package com.rankingsys.model;

public class PlayerRankView {
    private String gameName;
    private int mmr;
    private int wins;
    private int losses;
    private String tierName;

    public String getGameName() {
        return gameName;
    }

    public void setGameName(String gameName) {
        this.gameName = gameName;
    }

    public int getMmr() {
        return mmr;
    }

    public void setMmr(int mmr) {
        this.mmr = mmr;
    }

    public int getWins() {
        return wins;
    }

    public void setWins(int wins) {
        this.wins = wins;
    }

    public int getLosses() {
        return losses;
    }

    public void setLosses(int losses) {
        this.losses = losses;
    }

    public String getTierName() {
        return tierName;
    }

    public void setTierName(String tierName) {
        this.tierName = tierName;
    }
}
