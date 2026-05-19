USE ranking_system;

INSERT INTO player (username, reg_date, password_hash, is_admin) VALUES
('admin', '2026-05-18 00:00:00', SHA2('admin123', 256), 1),
('pxk', '2026-05-04 00:00:00', SHA2('123456', 256), 0),
('duanchenyu', '2026-05-05 00:00:00', SHA2('654321', 256), 0),
('admin2', '2026-01-01 00:00:00', SHA2('admin123', 256), 1),
('lixiaoming', '2026-04-10 00:00:00', SHA2('lxm1234', 256), 0),
('wanghua', '2026-03-15 00:00:00', SHA2('wh78901', 256), 0),
('zhangwei', '2026-02-20 00:00:00', SHA2('zw34567', 256), 0),
('sunxiaochuan521', '2026-02-21 00:00:00', SHA2('nmsl114514', 256), 0),
('laoda', '2026-02-22 00:00:00', SHA2('man999', 256), 0),
('buchi', '2026-02-23 00:00:00', SHA2('ice123', 256), 0),
('chashao', '2026-02-24 00:00:00', SHA2('yami987', 256), 0),
('alice', '2026-05-18 00:00:00', SHA2('alice123', 256), 0),
('bob', '2026-05-18 00:00:00', SHA2('bob123', 256), 0),
('cathy', '2026-05-18 00:00:00', SHA2('cathy123', 256), 0),
('david', '2026-05-18 00:00:00', SHA2('david123', 256), 0),
('frank', '2026-05-18 00:00:00', SHA2('frank123', 256), 0),
('grace', '2026-05-18 00:00:00', SHA2('grace123', 256), 0),
('henry', '2026-05-18 00:00:00', SHA2('henry123', 256), 0),
('iris', '2026-05-18 00:00:00', SHA2('iris123', 256), 0),
('jack', '2026-05-18 00:00:00', SHA2('jack123', 256), 0);

-- Match type dictionary (subtype parent table)
INSERT INTO match_type (type_code, display_name, mmr_multiplier, description) VALUES
('CASUAL', 'Casual Custom', 0, 'MMR ×0 — win/loss counts but no rating change'),
('NORMAL', 'Normal Match',  1, 'MMR ×1 — standard ranked match'),
('PEAK',   'Peak Match',    3, 'MMR ×3 — high-stakes competitive match');

INSERT INTO game (name, default_mmr) VALUES
('League of Legends', 1000),
('csgo', 500),
('Valorant', 900),
('Dota 2', 400),
('Overwatch 2', 600),
('Apex Legends', 1000);

INSERT INTO rank_tier (game_id, tier_name, min_mmr) VALUES
((SELECT game_id FROM game WHERE name = 'League of Legends'), 'Bronze', 0),
((SELECT game_id FROM game WHERE name = 'League of Legends'), 'Silver', 1000),
((SELECT game_id FROM game WHERE name = 'League of Legends'), 'Gold', 1200),
((SELECT game_id FROM game WHERE name = 'League of Legends'), 'Platinum', 1400),
((SELECT game_id FROM game WHERE name = 'League of Legends'), 'Diamond', 1700),
((SELECT game_id FROM game WHERE name = 'csgo'), 'Bronze', 0),
((SELECT game_id FROM game WHERE name = 'csgo'), 'Silver', 500),
((SELECT game_id FROM game WHERE name = 'csgo'), 'Gold', 700),
((SELECT game_id FROM game WHERE name = 'csgo'), 'Platinum', 1200),
((SELECT game_id FROM game WHERE name = 'csgo'), 'Diamond', 1500),
((SELECT game_id FROM game WHERE name = 'Valorant'), 'Silver', 800),
((SELECT game_id FROM game WHERE name = 'Valorant'), 'Gold', 1500),
((SELECT game_id FROM game WHERE name = 'Valorant'), 'Master', 2500),
((SELECT game_id FROM game WHERE name = 'Dota 2'), 'Herald', 0),
((SELECT game_id FROM game WHERE name = 'Dota 2'), 'Guardian', 1000),
((SELECT game_id FROM game WHERE name = 'Dota 2'), 'Crusader', 2000),
((SELECT game_id FROM game WHERE name = 'Dota 2'), 'Archon', 3000),
((SELECT game_id FROM game WHERE name = 'Dota 2'), 'Legend', 4000),
((SELECT game_id FROM game WHERE name = 'Dota 2'), 'Ancient', 5000),
((SELECT game_id FROM game WHERE name = 'Dota 2'), 'Divine', 6000),
((SELECT game_id FROM game WHERE name = 'Dota 2'), 'Immortal', 7000),
((SELECT game_id FROM game WHERE name = 'Overwatch 2'), 'Bronze', 0),
((SELECT game_id FROM game WHERE name = 'Overwatch 2'), 'Silver', 1000),
((SELECT game_id FROM game WHERE name = 'Overwatch 2'), 'Gold', 2000),
((SELECT game_id FROM game WHERE name = 'Overwatch 2'), 'Platinum', 3000),
((SELECT game_id FROM game WHERE name = 'Overwatch 2'), 'Diamond', 4000),
((SELECT game_id FROM game WHERE name = 'Overwatch 2'), 'Master', 5000),
((SELECT game_id FROM game WHERE name = 'Overwatch 2'), 'Grandmaster', 6000),
((SELECT game_id FROM game WHERE name = 'Apex Legends'), 'Bronze', 0),
((SELECT game_id FROM game WHERE name = 'Apex Legends'), 'Silver', 1000),
((SELECT game_id FROM game WHERE name = 'Apex Legends'), 'Gold', 2000),
((SELECT game_id FROM game WHERE name = 'Apex Legends'), 'Platinum', 3000),
((SELECT game_id FROM game WHERE name = 'Apex Legends'), 'Diamond', 4000),
((SELECT game_id FROM game WHERE name = 'Apex Legends'), 'Master', 5000);

INSERT INTO player_stats (player_id, game_id, tier_id, mmr, wins, losses)
SELECT p.player_id, g.game_id, rt.tier_id, g.default_mmr, 0, 0
FROM player p
CROSS JOIN game g
JOIN rank_tier rt ON rt.game_id = g.game_id
    AND rt.min_mmr = (
        SELECT MAX(rt2.min_mmr)
        FROM rank_tier rt2
        WHERE rt2.game_id = g.game_id
          AND rt2.min_mmr <= g.default_mmr
    );

INSERT INTO announcement (message, created_by) VALUES
('Welcome to the ranking system! Check this page for updates and maintenance notices.',
 (SELECT player_id FROM player WHERE username = 'admin2')),
('You can now register your own account on the login page.',
 (SELECT player_id FROM player WHERE username = 'admin2'));
