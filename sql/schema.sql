CREATE DATABASE IF NOT EXISTS ranking_system
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE ranking_system;

CREATE TABLE player (
    player_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    reg_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    password_hash VARCHAR(128) NOT NULL,
    is_admin TINYINT(1) NOT NULL DEFAULT 0
);

CREATE TABLE game (
    game_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE rank_tier (
    tier_id INT AUTO_INCREMENT PRIMARY KEY,
    game_id INT NOT NULL,
    tier_name VARCHAR(50) NOT NULL,
    min_mmr INT NOT NULL,
    CONSTRAINT fk_rank_tier_game FOREIGN KEY (game_id) REFERENCES game(game_id),
    CONSTRAINT uq_rank_tier UNIQUE (game_id, tier_name),
    CONSTRAINT uq_rank_tier_mmr UNIQUE (game_id, min_mmr)
);

CREATE TABLE player_stats (
    player_id INT NOT NULL,
    game_id INT NOT NULL,
    tier_id INT NOT NULL,
    mmr INT NOT NULL DEFAULT 1000,
    wins INT NOT NULL DEFAULT 0,
    losses INT NOT NULL DEFAULT 0,
    PRIMARY KEY (player_id, game_id),
    CONSTRAINT fk_player_stats_player FOREIGN KEY (player_id) REFERENCES player(player_id),
    CONSTRAINT fk_player_stats_game FOREIGN KEY (game_id) REFERENCES game(game_id),
    CONSTRAINT fk_player_stats_tier FOREIGN KEY (tier_id) REFERENCES rank_tier(tier_id)
);

CREATE TABLE match_history (
    match_id INT AUTO_INCREMENT PRIMARY KEY,
    game_id INT NOT NULL,
    match_type VARCHAR(20) NOT NULL DEFAULT '5v5',
    start_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by INT NOT NULL,
    CONSTRAINT fk_match_history_game FOREIGN KEY (game_id) REFERENCES game(game_id),
    CONSTRAINT fk_match_history_creator FOREIGN KEY (created_by) REFERENCES player(player_id)
);

CREATE TABLE match_player_result (
    match_id INT NOT NULL,
    player_id INT NOT NULL,
    result VARCHAR(4) NOT NULL,
    mmr_change INT NOT NULL,
    mmr_before INT NOT NULL,
    mmr_after INT NOT NULL,
    PRIMARY KEY (match_id, player_id),
    CONSTRAINT fk_mpr_match FOREIGN KEY (match_id) REFERENCES match_history(match_id),
    CONSTRAINT fk_mpr_player FOREIGN KEY (player_id) REFERENCES player(player_id),
    CONSTRAINT chk_mpr_result CHECK (result IN ('WIN', 'LOSE'))
);
