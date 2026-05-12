USE ranking_system;

INSERT INTO player (username, password_hash, is_admin) VALUES
('admin', SHA2('admin123', 256), 1),
('alice', SHA2('alice123', 256), 0),
('bob', SHA2('bob123', 256), 0),
('cathy', SHA2('cathy123', 256), 0),
('david', SHA2('david123', 256), 0),
('emma', SHA2('emma123', 256), 0),
('frank', SHA2('frank123', 256), 0),
('grace', SHA2('grace123', 256), 0),
('henry', SHA2('henry123', 256), 0),
('iris', SHA2('iris123', 256), 0),
('jack', SHA2('jack123', 256), 0);

INSERT INTO game (name) VALUES
('League of Legends'),
('csgo');

INSERT INTO rank_tier (game_id, tier_name, min_mmr) VALUES
(1, 'Bronze', 0),
(1, 'Silver', 1000),
(1, 'Gold', 1200),
(1, 'Platinum', 1400),
(1, 'Diamond', 1700),
(2, 'Bronze', 0),
(2, 'Silver', 500),
(2, 'Gold', 700),
(2, 'Platinum', 1200),
(2, 'Diamond', 1500);

INSERT INTO player_stats (player_id, game_id, tier_id, mmr, wins, losses)
SELECT p.player_id, 1, rt.tier_id, 1000, 0, 0
FROM player p
JOIN rank_tier rt ON rt.game_id = 1 AND rt.tier_name = 'Silver'
WHERE p.username <> 'admin';

INSERT INTO player_stats (player_id, game_id, tier_id, mmr, wins, losses)
SELECT p.player_id, 2, rt.tier_id, 1000, 0, 0
FROM player p
JOIN rank_tier rt ON rt.game_id = 2 AND rt.tier_name = 'Silver'
WHERE p.username <> 'admin';
