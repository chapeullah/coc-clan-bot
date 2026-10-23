DROP SCHEMA public CASCADE;
CREATE SCHEMA public;

CREATE TABLE wars (
    id bigint PRIMARY KEY,
    clan_tag text NOT NULL,
    clan_name text NOT NULL DEFAULT 'GUCCI',
    opponent_clan_tag text NOT NULL,
    opponent_clan_name text NOT NULL DEFAULT 'Opponent',
    preparation_start_time timestamptz NOT NULL,
    start_time timestamptz NOT NULL,
    end_time timestamptz NOT NULL,
    state text NOT NULL DEFAULT 'warEnded',
    team_size integer NOT NULL DEFAULT 20,
    attacks_per_member integer NOT NULL DEFAULT 2,
    battle_modifier text NOT NULL DEFAULT 'none',
    clan_attacks integer NOT NULL DEFAULT 0,
    clan_stars integer NOT NULL DEFAULT 0,
    clan_destruction_percentage double precision NOT NULL DEFAULT 0,
    clan_level integer NOT NULL DEFAULT 10,
    opponent_clan_attacks integer NOT NULL DEFAULT 0,
    opponent_clan_stars integer NOT NULL DEFAULT 0,
    opponent_clan_destruction_percentage double precision NOT NULL DEFAULT 0,
    opponent_clan_level integer NOT NULL DEFAULT 28,
    clan_attacks_changed_at timestamptz,
    clan_stars_changed_at timestamptz,
    clan_destruction_percentage_changed_at timestamptz,
    clan_level_changed_at timestamptz,
    opponent_clan_stars_changed_at timestamptz,
    opponent_clan_destruction_percentage_changed_at timestamptz,
    opponent_clan_level_changed_at timestamptz,
    updated_at timestamptz,
    war_log_synced_at timestamptz,
    CONSTRAINT uk_wars_clans_start UNIQUE (clan_tag, opponent_clan_tag, start_time)
);
CREATE TABLE war_log (
    id bigint PRIMARY KEY,
    clan_tag text NOT NULL,
    opponent_clan_tag text,
    end_time timestamptz NOT NULL,
    result text,
    team_size integer,
    attacks_per_member integer,
    battle_modifier text,
    clan_attacks integer,
    clan_stars integer,
    clan_destruction_percentage double precision,
    clan_level integer,
    opponent_clan_stars integer,
    opponent_clan_destruction_percentage double precision,
    opponent_clan_level integer
);
CREATE TABLE war_participants (
    id bigint PRIMARY KEY,
    war_id bigint NOT NULL REFERENCES wars(id),
    player_tag text NOT NULL,
    clan_tag text NOT NULL,
    UNIQUE (war_id, player_tag)
);
CREATE TABLE war_attacks (
    id bigint PRIMARY KEY,
    war_id bigint NOT NULL REFERENCES wars(id),
    attacker_participant_id bigint NOT NULL REFERENCES war_participants(id),
    defender_participant_id bigint NOT NULL REFERENCES war_participants(id),
    attacker_tag text NOT NULL,
    defender_tag text NOT NULL,
    attacker_town_hall_level integer NOT NULL,
    defender_town_hall_level integer NOT NULL,
    stars integer NOT NULL,
    destruction_percentage integer NOT NULL,
    duration_seconds integer NOT NULL,
    attack_order integer NOT NULL,
    UNIQUE (war_id, attack_order)
);
CREATE TABLE telegram_war_notification (
    id bigint PRIMARY KEY,
    war_id bigint NOT NULL REFERENCES wars(id),
    type text NOT NULL,
    sent_at timestamptz,
    UNIQUE (war_id, type)
);
-- Legacy entity removed in f2ca919; its table can still exist on the VPS.
CREATE TABLE telegram_war_attack_notification (
    id bigint PRIMARY KEY,
    war_attack_id bigint NOT NULL UNIQUE REFERENCES war_attacks(id),
    chat_id bigint NOT NULL,
    thread_id integer,
    text text NOT NULL,
    sent_at timestamptz,
    created_at timestamptz NOT NULL
);

INSERT INTO wars (id, clan_tag, opponent_clan_tag, preparation_start_time, start_time, end_time)
VALUES
    (2, '#2C020L9P0', '#R8QQUQVG', '2026-10-19 08:03:23+00', '2026-10-20 07:03:23+00', '2026-10-21 07:03:23+00'),
    (3, '#2C020L9P0', '#R8QQUQVG', '2026-10-19 08:03:23+00', '2026-10-20 08:35:31+00', '2026-10-21 08:35:31+00'),
    (4, '#2C020L9P0', '#L9LJ92UC', '2026-10-21 10:06:05+00', '2026-10-22 09:06:05+00', '2026-10-23 09:06:05+00');
UPDATE wars SET clan_attacks = 32, clan_stars = 60, clan_destruction_percentage = 100,
    opponent_clan_attacks = 31, opponent_clan_stars = 60, opponent_clan_destruction_percentage = 100
WHERE id = 3;
UPDATE wars SET state = 'inWar' WHERE id = 4;
INSERT INTO war_log VALUES
    (61, '#2C020L9P0', '#R8QQUQVG', '2026-10-21 09:27:26+00', 'tie', 20, 2, 'none',
     33, 60, 100, 10, 60, 100, 28);

INSERT INTO war_participants (id, war_id, player_tag, clan_tag)
SELECT w.id * 100 + p.n, w.id, '#P' || p.n,
       CASE WHEN p.n <= 20 THEN '#2C020L9P0' ELSE '#R8QQUQVG' END
FROM (VALUES (2), (3)) AS w(id) CROSS JOIN generate_series(1, 40) AS p(n);

INSERT INTO war_attacks VALUES
    (501, 2, 201, 221, '#P1', '#P21', 10, 10, 3, 100, 120, 1),
    (502, 2, 202, 222, '#P2', '#P22', 10, 10, 2, 80, 130, 2),
    (601, 3, 301, 321, '#P1', '#P21', 10, 10, 3, 100, 120, 1);

INSERT INTO telegram_war_attack_notification VALUES
    (901, 501, -100123, 42, 'Sent duplicate attack', '2026-10-20 12:00:00+00', '2026-10-20 12:00:00+00'),
    (902, 601, -100123, 42, 'Pending duplicate attack', NULL, '2026-10-20 12:01:00+00'),
    (903, 502, -100123, NULL, 'Sent unique attack', '2026-10-20 13:00:00+00', '2026-10-20 13:00:00+00');

INSERT INTO telegram_war_notification VALUES
    (700, 2, 'HALF_TIME', '2026-10-20 19:03:23+00'),
    (701, 3, 'HALF_TIME', NULL),
    (702, 2, 'PREPARATION', NULL),
    (703, 3, 'PREPARATION', '2026-10-20 08:00:01+00'),
    (704, 2, 'ONE_HOUR_LEFT', '2026-10-21 06:03:23+00'),
    (705, 2, 'WAR_START', '2026-10-20 07:03:23+00'),
    (706, 3, 'WAR_START', '2026-10-20 08:35:31+00');
