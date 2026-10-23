package com.chapeullah.cocclanbot.war.repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named = "war.repair.jdbcUrl", matches = "jdbc:postgresql:.*")
class WarRepairPostgresTest {

    private Connection connection;

    @BeforeEach
    void resetIsolatedDatabase() throws SQLException, IOException {
        connection = DriverManager.getConnection(System.getProperty("war.repair.jdbcUrl"), "postgres", "");
        assertEquals("codex_war_repair_test", scalar("SELECT current_database()"),
                "Refuse to replace any schema outside the dedicated repair test database");
        try (Statement statement = connection.createStatement()) {
            statement.execute(Files.readString(Path.of("src/test/resources/sql/shifted-war-fixture.sql"),
                    StandardCharsets.UTF_8));
        }
    }

    @AfterEach
    void closeConnection() throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }

    @Test
    void repairPreservesRelationshipsAndSentNotificationsAndCanBeRepeated() throws Exception {
        repair();

        assertEquals("0", scalar("SELECT count(*) FROM wars WHERE id = 2"));
        assertEquals("40", scalar("SELECT count(*) FROM war_participants WHERE war_id = 3"));
        assertEquals("2", scalar("SELECT count(*) FROM war_attacks WHERE war_id = 3"));
        assertEquals("0", scalar("""
                SELECT count(*) FROM war_attacks a
                JOIN war_participants p ON p.id = a.attacker_participant_id
                JOIN war_participants d ON d.id = a.defender_participant_id
                WHERE p.war_id <> a.war_id OR d.war_id <> a.war_id
                """));
        assertEquals("33", scalar("SELECT clan_attacks FROM wars WHERE id = 3"));
        assertEquals("true", scalar("SELECT end_time = TIMESTAMPTZ '2026-10-21 09:27:26+00' FROM wars WHERE id = 3"));
        assertEquals("true", scalar("SELECT war_log_synced_at IS NOT NULL FROM wars WHERE id = 3"));
        assertEquals("4", scalar("SELECT count(*) FROM telegram_war_notification WHERE war_id = 3"));
        assertEquals("true", scalar("SELECT sent_at IS NOT NULL FROM telegram_war_notification WHERE id = 700"));
        assertEquals("0", scalar("SELECT count(*) FROM telegram_war_notification WHERE id IN (701, 702, 705)"));
        assertEquals("2", scalar("SELECT count(*) FROM telegram_war_attack_notification"));
        assertEquals("601", scalar("SELECT war_attack_id FROM telegram_war_attack_notification WHERE id = 901"));
        assertEquals("true", scalar("SELECT sent_at IS NOT NULL FROM telegram_war_attack_notification WHERE id = 901"));
        assertEquals("Sent duplicate attack", scalar("SELECT text FROM telegram_war_attack_notification WHERE id = 901"));
        assertEquals("502", scalar("SELECT war_attack_id FROM telegram_war_attack_notification WHERE id = 903"));
        assertEquals("0", scalar("SELECT count(*) FROM telegram_war_attack_notification WHERE id = 902"));
        assertEquals("1", scalar("""
                SELECT count(*) FROM pg_constraint
                WHERE conrelid = 'wars'::regclass AND conname = 'uk_wars_clans_preparation'
                """));
        assertEquals("0", scalar("""
                SELECT count(*) FROM pg_constraint
                WHERE conrelid = 'wars'::regclass AND conname = 'uk_wars_clans_start'
                """));
        String confirmedAt = scalar("SELECT war_log_synced_at FROM wars WHERE id = 3");

        repair();

        assertEquals(confirmedAt, scalar("SELECT war_log_synced_at FROM wars WHERE id = 3"));
        assertEquals("2", scalar("SELECT count(*) FROM war_attacks"));
        assertEquals("2", scalar("SELECT count(*) FROM telegram_war_attack_notification"));
    }

    @ParameterizedTest
    @CsvSource({"false,true", "true,true", "false,false"})
    void legacyNotificationKeepsTargetUnlessOnlySourceWasSent(boolean sourceSent, boolean targetSent) throws Exception {
        execute("UPDATE telegram_war_attack_notification SET sent_at = "
                + (sourceSent ? "created_at" : "NULL") + " WHERE id = 901");
        execute("UPDATE telegram_war_attack_notification SET sent_at = "
                + (targetSent ? "created_at" : "NULL") + " WHERE id = 902");

        repair();

        assertEquals("0", scalar("SELECT count(*) FROM telegram_war_attack_notification WHERE id = 901"));
        assertEquals("601", scalar("SELECT war_attack_id FROM telegram_war_attack_notification WHERE id = 902"));
        assertEquals(Boolean.toString(targetSent), scalar("SELECT sent_at IS NOT NULL FROM telegram_war_attack_notification WHERE id = 902"));
    }

    @Test
    void legacyNotificationWithoutTargetCounterpartFollowsTheRetainedAttack() throws Exception {
        execute("DELETE FROM telegram_war_attack_notification WHERE id = 902");

        repair();

        assertEquals("601", scalar("SELECT war_attack_id FROM telegram_war_attack_notification WHERE id = 901"));
        assertEquals("true", scalar("SELECT sent_at IS NOT NULL FROM telegram_war_attack_notification WHERE id = 901"));
        assertEquals("2", scalar("SELECT count(*) FROM telegram_war_attack_notification"));
    }

    @Test
    void absentLegacyTableDoesNotPreventTheRepair() throws Exception {
        execute("DROP TABLE telegram_war_attack_notification");

        repair();

        assertEquals("0", scalar("SELECT count(*) FROM wars WHERE id = 2"));
        assertEquals("2", scalar("SELECT count(*) FROM war_attacks WHERE war_id = 3"));
    }

    @Test
    void differentLegacyNotificationDestinationsRollBackTheRepair() throws Exception {
        execute("UPDATE telegram_war_attack_notification SET thread_id = 43 WHERE id = 902");

        SQLException failure = assertThrows(SQLException.class, this::repair);
        execute("ROLLBACK");

        assertTrue(failure.getMessage().contains("different destinations"));
        assertOriginalDataPresent();
    }

    @Test
    void aForeignKeyIntoLegacyNotificationsIsAlsoRejectedWithItsDefinition() throws Exception {
        execute("CREATE TABLE unexpected_notification_refs (notification_id bigint REFERENCES telegram_war_attack_notification(id) ON DELETE CASCADE)");
        execute("INSERT INTO unexpected_notification_refs VALUES (901)");

        SQLException failure = assertThrows(SQLException.class, this::repair);
        execute("ROLLBACK");

        assertTrue(failure.getMessage().contains("Unexpected foreign keys"));
        assertTrue(failure.getMessage().contains("unexpected_notification_refs"));
        assertTrue(failure.getMessage().contains("FOREIGN KEY (notification_id)"));
        assertOriginalDataPresent();
        assertEquals("1", scalar("SELECT count(*) FROM unexpected_notification_refs"));
    }

    @Test
    void conflictingAttackRollsBackTheWholeRepair() throws Exception {
        execute("UPDATE war_attacks SET stars = 2 WHERE id = 501");

        SQLException failure = assertThrows(SQLException.class, this::repair);
        execute("ROLLBACK");

        assertTrue(failure.getMessage().contains("conflicting payloads"));
        assertOriginalDataPresent();
    }

    @Test
    void incompleteFinalEntryCannotConfirmOrDeleteTheDuplicate() throws Exception {
        execute("UPDATE war_log SET clan_attacks = NULL WHERE id = 61");

        assertThrows(SQLException.class, this::repair);
        execute("ROLLBACK");

        assertOriginalDataPresent();
    }

    @Test
    void unknownCascadingForeignKeyPreventsAnyDeletion() throws Exception {
        execute("CREATE TABLE unexpected_war_refs (war_id bigint REFERENCES wars(id) ON DELETE CASCADE)");
        execute("INSERT INTO unexpected_war_refs VALUES (2)");

        SQLException failure = assertThrows(SQLException.class, this::repair);
        execute("ROLLBACK");

        assertTrue(failure.getMessage().contains("Unexpected foreign keys"));
        assertOriginalDataPresent();
        assertEquals("1", scalar("SELECT count(*) FROM unexpected_war_refs"));
    }

    @Test
    void anAdditionalForeignKeyInAKnownTableIsAlsoRejected() throws Exception {
        execute("ALTER TABLE war_attacks ADD COLUMN original_war_id bigint REFERENCES wars(id) ON DELETE CASCADE");
        execute("UPDATE war_attacks SET original_war_id = 2 WHERE id = 501");

        SQLException failure = assertThrows(SQLException.class, this::repair);
        execute("ROLLBACK");

        assertTrue(failure.getMessage().contains("Unexpected foreign keys"));
        assertOriginalDataPresent();
    }

    @Test
    void anIncorrectExistingConstraintRollsBackTheRepair() throws Exception {
        execute("ALTER TABLE wars ADD CONSTRAINT uk_wars_clans_preparation UNIQUE (id, clan_tag)");

        SQLException failure = assertThrows(SQLException.class, this::repair);
        execute("ROLLBACK");

        assertTrue(failure.getMessage().contains("unexpected definition"));
        assertOriginalDataPresent();
    }

    private void assertOriginalDataPresent() throws SQLException {
        assertEquals("2", scalar("SELECT count(*) FROM wars WHERE id IN (2, 3)"));
        assertEquals("80", scalar("SELECT count(*) FROM war_participants"));
        assertEquals("3", scalar("SELECT count(*) FROM war_attacks"));
        assertEquals("7", scalar("SELECT count(*) FROM telegram_war_notification"));
        assertEquals("3", scalar("SELECT count(*) FROM telegram_war_attack_notification"));
        assertEquals("501", scalar("SELECT war_attack_id FROM telegram_war_attack_notification WHERE id = 901"));
        assertEquals("true", scalar("SELECT war_log_synced_at IS NULL FROM wars WHERE id = 3"));
        assertEquals("1", scalar("""
                SELECT count(*) FROM pg_constraint
                WHERE conrelid = 'wars'::regclass AND conname = 'uk_wars_clans_start'
                """));
    }

    private void repair() throws IOException, SQLException {
        execute(Files.readString(Path.of("sql/repair_shifted_war.sql"), StandardCharsets.UTF_8));
    }

    private void execute(String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private String scalar(String sql) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery(sql)) {
            assertTrue(rows.next());
            Object value = rows.getObject(1);
            return value instanceof Boolean ? value.toString() : rows.getString(1);
        }
    }
}
