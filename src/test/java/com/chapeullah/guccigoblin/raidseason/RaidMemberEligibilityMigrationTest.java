package com.chapeullah.guccigoblin.raidseason;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RaidMemberEligibilityMigrationTest {

    @Test
    void upgradesPopulatedTablesWithoutInventingHistoryAndCanRunTwice() throws Exception {
        try (var connection = DriverManager.getConnection(
                "jdbc:h2:mem:raid_eligibility_upgrade;MODE=PostgreSQL")) {
            try (var statement = connection.createStatement()) {
                statement.execute("CREATE TABLE clans (tag varchar(255) PRIMARY KEY)");
                statement.execute("CREATE TABLE members (id bigint PRIMARY KEY, joined_at timestamp with time zone)");
                statement.execute("CREATE TABLE raid_season_clan_member_snapshots (id bigint PRIMARY KEY, tag varchar(255))");
                statement.execute("INSERT INTO clans (tag) VALUES ('#CLAN')");
                statement.execute("INSERT INTO members (id, joined_at) VALUES (1, TIMESTAMP WITH TIME ZONE '2026-10-01 00:00:00+00')");
                statement.execute("INSERT INTO raid_season_clan_member_snapshots (id, tag) VALUES (1, '#PLAYER')");
            }

            var script = new FileSystemResource("sql/raid_member_eligibility.sql");
            ScriptUtils.executeSqlScript(connection, script);
            ScriptUtils.executeSqlScript(connection, script);

            try (var statement = connection.createStatement();
                 var rows = statement.executeQuery("""
                         SELECT c.member_list_synced_at, m.joined_after, m.joined_at,
                                s.tag, s.joined_at, s.joined_after, s.role,
                                s.clan_type, s.participation_eligible
                         FROM clans c CROSS JOIN members m
                         CROSS JOIN raid_season_clan_member_snapshots s
                         """)) {
                assertTrue(rows.next());
                assertNull(rows.getObject(1));
                assertNull(rows.getObject(2));
                assertEquals("2026-10-01T00:00Z", rows.getObject(3).toString());
                assertEquals("#PLAYER", rows.getString(4));
                for (int column = 5; column <= 9; column++) {
                    assertNull(rows.getObject(column));
                }
                assertFalse(rows.next());
            }
        }
    }
}
