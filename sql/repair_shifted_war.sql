-- PostgreSQL 16+. One-time repair for the inspected VPS rows: wars 2 -> 3, war_log 61.
-- Stop the application and take a fresh pg_dump before running with ON_ERROR_STOP=1.
-- Run BEFORE deploying the new preparation-time unique constraint.
-- Conflicting attacks, unexpected relationships or a different war abort the transaction.
BEGIN;
SET LOCAL lock_timeout = '5s';
SET LOCAL search_path = public, pg_catalog;
LOCK TABLE wars, war_log, war_participants, war_attacks, telegram_war_notification
    IN SHARE ROW EXCLUSIVE MODE;

DO $repair$
DECLARE
    source_war wars%ROWTYPE;
    target_war wars%ROWTYPE;
    final_entry war_log%ROWTYPE;
    repaired_at timestamptz := transaction_timestamp();
    unexpected_foreign_keys text;
BEGIN
    IF to_regclass('telegram_war_attack_notification') IS NOT NULL THEN
        EXECUTE 'LOCK TABLE telegram_war_attack_notification IN SHARE ROW EXCLUSIVE MODE';
    END IF;

    SELECT * INTO STRICT target_war FROM wars WHERE id = 3;
    SELECT * INTO STRICT final_entry FROM war_log WHERE id = 61;

    IF target_war.clan_tag IS DISTINCT FROM '#2C020L9P0'
        OR target_war.opponent_clan_tag IS DISTINCT FROM '#R8QQUQVG'
        OR target_war.preparation_start_time IS DISTINCT FROM TIMESTAMPTZ '2026-10-19 08:03:23+00'
        OR target_war.state IS DISTINCT FROM 'warEnded'
        OR final_entry.clan_tag IS DISTINCT FROM target_war.clan_tag
        OR final_entry.opponent_clan_tag IS DISTINCT FROM target_war.opponent_clan_tag
        OR final_entry.end_time IS DISTINCT FROM TIMESTAMPTZ '2026-10-21 09:27:26+00'
        OR final_entry.team_size IS DISTINCT FROM target_war.team_size
        OR (final_entry.attacks_per_member IS NOT NULL
            AND final_entry.attacks_per_member IS DISTINCT FROM target_war.attacks_per_member)
        OR (final_entry.battle_modifier IS NOT NULL
            AND final_entry.battle_modifier IS DISTINCT FROM target_war.battle_modifier)
        OR final_entry.clan_attacks IS NULL OR final_entry.clan_stars IS NULL
        OR final_entry.clan_destruction_percentage IS NULL
        OR final_entry.opponent_clan_stars IS NULL
        OR final_entry.opponent_clan_destruction_percentage IS NULL
        OR final_entry.end_time <= target_war.start_time THEN
        RAISE EXCEPTION 'War 3 or war_log 61 does not match the inspected war; nothing was repaired';
    END IF;

    IF EXISTS (SELECT 1 FROM wars
               WHERE clan_tag = target_war.clan_tag
                 AND preparation_start_time > target_war.preparation_start_time
                 AND preparation_start_time < final_entry.end_time)
        OR EXISTS (SELECT 1 FROM wars
                   WHERE id <> 3 AND clan_tag = target_war.clan_tag
                     AND end_time = final_entry.end_time AND war_log_synced_at IS NOT NULL) THEN
        RAISE EXCEPTION 'Final entry overlaps another war or is already assigned';
    END IF;

    SELECT * INTO source_war FROM wars WHERE id = 2;
    IF NOT FOUND THEN
        IF target_war.end_time IS DISTINCT FROM final_entry.end_time
            OR target_war.war_log_synced_at IS NULL
            OR target_war.clan_attacks IS DISTINCT FROM final_entry.clan_attacks
            OR target_war.clan_stars IS DISTINCT FROM final_entry.clan_stars
            OR target_war.clan_destruction_percentage IS DISTINCT FROM final_entry.clan_destruction_percentage
            OR target_war.opponent_clan_stars IS DISTINCT FROM final_entry.opponent_clan_stars
            OR target_war.opponent_clan_destruction_percentage
                IS DISTINCT FROM final_entry.opponent_clan_destruction_percentage THEN
            RAISE EXCEPTION 'War 2 is absent but war 3 is not fully repaired';
        END IF;
        RAISE NOTICE 'War data was already repaired';
        RETURN;
    END IF;

    IF (source_war.clan_tag, source_war.opponent_clan_tag, source_war.preparation_start_time,
        source_war.team_size, source_war.attacks_per_member, source_war.battle_modifier, source_war.state)
        IS DISTINCT FROM
       (target_war.clan_tag, target_war.opponent_clan_tag, target_war.preparation_start_time,
        target_war.team_size, target_war.attacks_per_member, target_war.battle_modifier, target_war.state)
        OR source_war.war_log_synced_at IS NOT NULL OR target_war.war_log_synced_at IS NOT NULL THEN
        RAISE EXCEPTION 'Wars 2 and 3 are not the expected unconfirmed duplicate';
    END IF;

    -- Refuse to delete data referenced by tables outside the inspected entity graph.
    SELECT string_agg(format('%s (%I): %s', c.conrelid::regclass, c.conname,
                             pg_get_constraintdef(c.oid)), E'\n' ORDER BY c.conname)
    INTO unexpected_foreign_keys
    FROM pg_constraint c
        WHERE c.contype = 'f'
          AND c.confrelid IN ('wars'::regclass, 'war_participants'::regclass,
                            'war_attacks'::regclass, 'telegram_war_notification'::regclass,
                            to_regclass('telegram_war_attack_notification'))
          AND NOT (
              cardinality(c.conkey) = 1 AND cardinality(c.confkey) = 1
              AND (SELECT attname FROM pg_attribute
                   WHERE attrelid = c.confrelid AND attnum = c.confkey[1]) = 'id'
              AND (
                  (c.confrelid = 'wars'::regclass
                   AND c.conrelid IN ('war_participants'::regclass, 'war_attacks'::regclass,
                                     'telegram_war_notification'::regclass)
                   AND (SELECT attname FROM pg_attribute
                        WHERE attrelid = c.conrelid AND attnum = c.conkey[1]) = 'war_id')
                  OR (c.confrelid = 'war_participants'::regclass AND c.conrelid = 'war_attacks'::regclass
                      AND (SELECT attname FROM pg_attribute
                           WHERE attrelid = c.conrelid AND attnum = c.conkey[1])
                          IN ('attacker_participant_id', 'defender_participant_id'))
                  OR (c.confrelid = 'war_attacks'::regclass
                      AND c.conrelid IS NOT DISTINCT FROM to_regclass('telegram_war_attack_notification')
                      AND (SELECT attname FROM pg_attribute
                           WHERE attrelid = c.conrelid AND attnum = c.conkey[1]) = 'war_attack_id')
              )
          );
    IF unexpected_foreign_keys IS NOT NULL THEN
        RAISE EXCEPTION 'Unexpected foreign keys reference war data; inspect them before merging'
            USING DETAIL = unexpected_foreign_keys;
    END IF;

    IF EXISTS (
        SELECT 1 FROM war_attacks a
        JOIN war_participants attacker ON attacker.id = a.attacker_participant_id
        JOIN war_participants defender ON defender.id = a.defender_participant_id
        WHERE a.war_id IN (2, 3)
          AND (attacker.war_id <> a.war_id OR defender.war_id <> a.war_id
               OR attacker.player_tag <> a.attacker_tag OR defender.player_tag <> a.defender_tag)
    ) OR EXISTS (
        SELECT 1 FROM war_participants s
        JOIN war_participants t ON t.war_id = 3 AND t.player_tag = s.player_tag
        WHERE s.war_id = 2 AND s.clan_tag IS DISTINCT FROM t.clan_tag
    ) THEN
        RAISE EXCEPTION 'Participant/attack relationships are inconsistent';
    END IF;

    IF EXISTS (
        SELECT 1 FROM war_attacks s
        JOIN war_attacks t ON t.war_id = 3 AND t.attack_order = s.attack_order
        WHERE s.war_id = 2 AND
            (s.attacker_tag, s.defender_tag, s.attacker_town_hall_level, s.defender_town_hall_level,
             s.stars, s.destruction_percentage, s.duration_seconds)
            IS DISTINCT FROM
            (t.attacker_tag, t.defender_tag, t.attacker_town_hall_level, t.defender_town_hall_level,
             t.stars, t.destruction_percentage, t.duration_seconds)
    ) THEN
        RAISE EXCEPTION 'The same attack order has conflicting payloads; nothing was merged';
    END IF;

    UPDATE war_participants s SET war_id = 3
    WHERE s.war_id = 2 AND NOT EXISTS (
        SELECT 1 FROM war_participants t WHERE t.war_id = 3 AND t.player_tag = s.player_tag);

    IF EXISTS (SELECT 1 FROM war_participants
               WHERE war_id = 3 AND clan_tag NOT IN (target_war.clan_tag, target_war.opponent_clan_tag))
        OR EXISTS (SELECT 1 FROM war_participants WHERE war_id = 3
                   GROUP BY clan_tag HAVING count(*) > target_war.team_size) THEN
        RAISE EXCEPTION 'Merged participants exceed the known roster';
    END IF;

    UPDATE war_attacks a SET attacker_participant_id = t.id
    FROM war_participants s JOIN war_participants t
        ON t.war_id = 3 AND t.player_tag = s.player_tag
    WHERE s.war_id = 2 AND a.attacker_participant_id = s.id;

    UPDATE war_attacks a SET defender_participant_id = t.id
    FROM war_participants s JOIN war_participants t
        ON t.war_id = 3 AND t.player_tag = s.player_tag
    WHERE s.war_id = 2 AND a.defender_participant_id = s.id;

    UPDATE war_attacks s SET war_id = 3
    WHERE s.war_id = 2 AND NOT EXISTS (
        SELECT 1 FROM war_attacks t WHERE t.war_id = 3 AND t.attack_order = s.attack_order);

    -- This legacy table can remain after its Java entity was removed. Unique attacks
    -- retain their IDs; notifications of duplicate attacks must follow the retained ID.
    IF to_regclass('telegram_war_attack_notification') IS NOT NULL THEN
        IF EXISTS (
            SELECT 1 FROM telegram_war_attack_notification n
            JOIN war_attacks a ON a.id = n.war_attack_id
            WHERE a.war_id IN (2, 3)
            GROUP BY n.war_attack_id HAVING count(*) > 1
        ) THEN
            RAISE EXCEPTION 'Multiple legacy notifications reference one attack; inspect them before merging';
        END IF;

        IF EXISTS (
            SELECT 1 FROM war_attacks s
            JOIN war_attacks t ON t.war_id = 3 AND t.attack_order = s.attack_order
            JOIN telegram_war_attack_notification sn ON sn.war_attack_id = s.id
            JOIN telegram_war_attack_notification tn ON tn.war_attack_id = t.id
            WHERE s.war_id = 2
              AND (sn.chat_id, sn.thread_id) IS DISTINCT FROM (tn.chat_id, tn.thread_id)
        ) THEN
            RAISE EXCEPTION 'Duplicate attack notifications have different destinations; inspect them before merging';
        END IF;

        -- Prefer a sent notification over an unsent one; otherwise retain the target.
        DELETE FROM telegram_war_attack_notification tn
        USING war_attacks s, war_attacks t, telegram_war_attack_notification sn
        WHERE s.war_id = 2 AND t.war_id = 3 AND t.attack_order = s.attack_order
          AND sn.war_attack_id = s.id AND tn.war_attack_id = t.id
          AND tn.sent_at IS NULL AND sn.sent_at IS NOT NULL;

        UPDATE telegram_war_attack_notification sn SET war_attack_id = t.id
        FROM war_attacks s JOIN war_attacks t
            ON t.war_id = 3 AND t.attack_order = s.attack_order
        WHERE s.war_id = 2 AND sn.war_attack_id = s.id
          AND NOT EXISTS (SELECT 1 FROM telegram_war_attack_notification tn
                          WHERE tn.war_attack_id = t.id);

        DELETE FROM telegram_war_attack_notification n
        USING war_attacks s
        WHERE s.war_id = 2 AND n.war_attack_id = s.id;
        RAISE NOTICE 'Preserved legacy attack-notification links and sent markers';
    END IF;

    DELETE FROM war_attacks WHERE war_id = 2;
    DELETE FROM war_participants WHERE war_id = 2;

    -- Retain a sent notification if either duplicate was sent, preventing a resend.
    DELETE FROM telegram_war_notification t USING telegram_war_notification s
    WHERE t.war_id = 3 AND s.war_id = 2 AND t.type = s.type
      AND t.sent_at IS NULL AND s.sent_at IS NOT NULL;
    UPDATE telegram_war_notification s SET war_id = 3
    WHERE s.war_id = 2 AND NOT EXISTS (
        SELECT 1 FROM telegram_war_notification t WHERE t.war_id = 3 AND t.type = s.type);
    DELETE FROM telegram_war_notification WHERE war_id = 2;

    UPDATE wars SET
        end_time = final_entry.end_time,
        clan_attacks_changed_at = CASE WHEN clan_attacks IS DISTINCT FROM final_entry.clan_attacks
            THEN repaired_at ELSE clan_attacks_changed_at END,
        clan_stars_changed_at = CASE WHEN clan_stars IS DISTINCT FROM final_entry.clan_stars
            THEN repaired_at ELSE clan_stars_changed_at END,
        clan_destruction_percentage_changed_at = CASE
            WHEN clan_destruction_percentage IS DISTINCT FROM final_entry.clan_destruction_percentage
            THEN repaired_at ELSE clan_destruction_percentage_changed_at END,
        clan_level_changed_at = CASE WHEN final_entry.clan_level IS NOT NULL
            AND clan_level IS DISTINCT FROM final_entry.clan_level THEN repaired_at ELSE clan_level_changed_at END,
        opponent_clan_stars_changed_at = CASE WHEN opponent_clan_stars IS DISTINCT FROM final_entry.opponent_clan_stars
            THEN repaired_at ELSE opponent_clan_stars_changed_at END,
        opponent_clan_destruction_percentage_changed_at = CASE
            WHEN opponent_clan_destruction_percentage IS DISTINCT FROM final_entry.opponent_clan_destruction_percentage
            THEN repaired_at ELSE opponent_clan_destruction_percentage_changed_at END,
        opponent_clan_level_changed_at = CASE WHEN final_entry.opponent_clan_level IS NOT NULL
            AND opponent_clan_level IS DISTINCT FROM final_entry.opponent_clan_level
            THEN repaired_at ELSE opponent_clan_level_changed_at END,
        clan_attacks = final_entry.clan_attacks,
        clan_stars = final_entry.clan_stars,
        clan_destruction_percentage = final_entry.clan_destruction_percentage,
        clan_level = coalesce(final_entry.clan_level, clan_level),
        opponent_clan_stars = final_entry.opponent_clan_stars,
        opponent_clan_destruction_percentage = final_entry.opponent_clan_destruction_percentage,
        opponent_clan_level = coalesce(final_entry.opponent_clan_level, opponent_clan_level),
        war_log_synced_at = repaired_at, updated_at = repaired_at
    WHERE id = 3;
    DELETE FROM wars WHERE id = 2;
    RAISE NOTICE 'Merged war 2 into war 3 and applied final war_log 61';
END;
$repair$;

ALTER TABLE wars DROP CONSTRAINT IF EXISTS uk_wars_clans_start;
DO $constraint$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_constraint
               WHERE conrelid = 'wars'::regclass AND conname = 'uk_wars_clans_preparation') THEN
        IF NOT EXISTS (SELECT 1 FROM pg_constraint
                       WHERE conrelid = 'wars'::regclass AND conname = 'uk_wars_clans_preparation'
                         AND contype = 'u' AND pg_get_constraintdef(oid)
                             = 'UNIQUE (clan_tag, opponent_clan_tag, preparation_start_time)') THEN
            RAISE EXCEPTION 'The preparation constraint exists with an unexpected definition';
        END IF;
    ELSE
        ALTER TABLE wars ADD CONSTRAINT uk_wars_clans_preparation
            UNIQUE (clan_tag, opponent_clan_tag, preparation_start_time);
    END IF;
END;
$constraint$;
COMMIT;

SELECT id, clan_name, opponent_clan_name, end_time,
       clan_attacks, clan_stars, opponent_clan_stars, war_log_synced_at
FROM wars ORDER BY id;
