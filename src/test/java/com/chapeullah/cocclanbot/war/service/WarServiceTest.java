package com.chapeullah.cocclanbot.war.service;

import com.chapeullah.cocclanbot.ClashOfClansClient;
import com.chapeullah.cocclanbot.war.dto.WarResponse;
import com.chapeullah.cocclanbot.war.model.War;
import com.chapeullah.cocclanbot.war.model.WarLogEntry;
import com.chapeullah.cocclanbot.war.repository.WarRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static com.chapeullah.cocclanbot.war.WarFixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WarServiceTest {

    @Mock private ClashOfClansClient client;
    @Mock private WarRepository repository;
    @Mock private WarParticipantService participants;
    @Mock private WarAttackService attacks;
    @Mock private WarLogService warLog;
    private WarService service;

    @BeforeEach
    void setUp() {
        service = serviceAt(NOW);
        lenient().when(repository.save(any(War.class))).thenAnswer(call -> call.getArgument(0));
        lenient().when(repository.saveAll(anyList())).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void changedStartUpdatesTheSameWarAndCorrectsPrematureEndedState() {
        War existing = war(ORIGINAL_START, ORIGINAL_END);
        identity(existing, List.of(existing));
        WarResponse incoming = response("inWar", PREPARATION, SHIFTED_START, SHIFTED_END);
        when(client.getCurrentWar(CLAN)).thenReturn(incoming);
        service = serviceAt(Instant.parse("2026-10-21T08:00:00Z"));

        assertSame(existing, service.syncCurrentWar(CLAN).orElseThrow());
        assertEquals(SHIFTED_START, existing.getStartTime());
        assertEquals(SHIFTED_END, existing.getEndTime());
        assertEquals("inWar", existing.getState());
        verify(repository).save(same(existing));
        verify(participants).syncWarParticipants(same(existing), same(incoming.clan()), same(incoming.opponent()));
        verify(attacks).syncAttacks(same(existing), same(incoming.clan()), same(incoming.opponent()));
    }

    @Test
    void anotherPreparationCreatesAnotherWarAgainstTheSameOpponent() {
        Instant preparation = NEXT_PREPARATION;
        when(client.getCurrentWar(CLAN)).thenReturn(response("inWar", preparation,
                Instant.parse("2026-10-22T09:06:05Z"), Instant.parse("2026-10-23T09:06:05Z")));

        War created = service.syncCurrentWar(CLAN).orElseThrow();

        assertEquals(preparation, created.getPreparationStartTime());
        verify(repository).findAllByClanTagAndOpponentClanTagAndPreparationStartTimeOrderByIdAsc(
                CLAN, OPPONENT, preparation);
        verify(repository).save(same(created));
    }

    @Test
    void existingDuplicatesRequireRepairInsteadOfChoosingOneArbitrarily() {
        War first = war(ORIGINAL_START, ORIGINAL_END);
        identity(first, List.of(first, war(SHIFTED_START, SHIFTED_END)));
        when(client.getCurrentWar(CLAN)).thenReturn(response("warEnded", PREPARATION, SHIFTED_START, SHIFTED_END));

        assertThrows(IllegalStateException.class, () -> service.syncCurrentWar(CLAN));
        verify(repository, never()).save(any());
        verifyNoInteractions(participants, attacks);
    }

    @Test
    void notInWarDoesNotRequireDatesOrClanDetails() {
        when(client.getCurrentWar(CLAN)).thenReturn(new WarResponse(
                "notInWar", null, null, null, null, null, null, null, null));

        assertTrue(service.syncCurrentWar(CLAN).isEmpty());
        verifyNoInteractions(repository, participants, attacks);
    }

    @Test
    void staleCurrentWarCannotOverwriteAConfirmedResultOrSchedule() {
        War existing = war(SHIFTED_START, FINAL_END);
        existing.update(33, 60, 100.0, 10, 31, 60, 100.0, 28, "warEnded");
        existing.markWarLogSynced(NOW);
        identity(existing, List.of(existing));
        when(client.getCurrentWar(CLAN)).thenReturn(response("inWar", PREPARATION, ORIGINAL_START, ORIGINAL_END));

        service.syncCurrentWar(CLAN);

        assertEquals(FINAL_END, existing.getEndTime());
        assertEquals(SHIFTED_START, existing.getStartTime());
        assertEquals(33, existing.getClanAttacks());
        assertEquals("warEnded", existing.getState());
        assertEquals(NOW, existing.getWarLogSyncedAt());
    }

    @Test
    void exactWarLogMatchConfirmsTheSummary() {
        War existing = ended(war(SHIFTED_START, FINAL_END));
        when(warLog.findByClanTagAndEndTime(CLAN, FINAL_END))
                .thenReturn(Optional.of(finalEntry(FINAL_END, 33, OPPONENT)));

        assertEquals(List.of(existing), service.syncEndedWarsFromWarLog(CLAN));
        assertEquals(33, existing.getClanAttacks());
        assertEquals(NOW, existing.getWarLogSyncedAt());
        verify(warLog, never()).findByClanAndOpponentAndEndTimeRange(any(), any(), any(), any());
    }

    @Test
    void shiftedFinalEndIsMatchedWithinTheNextPreparationBoundary() {
        War existing = ended(war(SHIFTED_START, SHIFTED_END));
        when(repository.findFirstByClanTagAndPreparationStartTimeGreaterThanOrderByPreparationStartTimeAsc(
                CLAN, PREPARATION)).thenReturn(Optional.of(nextWar()));
        when(warLog.findByClanAndOpponentAndEndTimeRange(CLAN, OPPONENT, SHIFTED_START, NEXT_PREPARATION))
                .thenReturn(List.of(finalEntry(FINAL_END, 33, OPPONENT)));

        assertEquals(List.of(existing), service.syncEndedWarsFromWarLog(CLAN));
        assertEquals(FINAL_END, existing.getEndTime());
        assertEquals(33, existing.getClanAttacks());
        assertEquals(60, existing.getClanStars());
        assertEquals(60, existing.getOpponentClanStars());
        assertEquals(NOW, existing.getWarLogSyncedAt());
        verifyNoInteractions(attacks, participants);
    }

    @Test
    void missingNextPreparationDoesNotPermitAnUnboundedHistoricalMatch() {
        War existing = ended(war(SHIFTED_START, SHIFTED_END));

        service.syncEndedWarsFromWarLog(CLAN);

        assertNull(existing.getWarLogSyncedAt());
        verify(warLog, never()).findByClanAndOpponentAndEndTimeRange(any(), any(), any(), any());
    }

    @Test
    void futurePreparationDoesNotAllowFutureWarLogResults() {
        War existing = ended(war(SHIFTED_START, SHIFTED_END));
        War future = new War(CLAN, "Clan", 0, 0, 0.0, 10, "#NEXT", "Other", 0, 0, 0.0, 10,
                "preparation", 20, 2, "none", NOW.plusSeconds(3600), NOW.plusSeconds(7200), NOW.plusSeconds(10800));
        when(repository.findFirstByClanTagAndPreparationStartTimeGreaterThanOrderByPreparationStartTimeAsc(
                CLAN, PREPARATION)).thenReturn(Optional.of(future));

        service.syncEndedWarsFromWarLog(CLAN);

        verify(warLog, never()).findByClanAndOpponentAndEndTimeRange(any(), any(), any(), any());
        assertNull(existing.getWarLogSyncedAt());
    }

    @Test
    void twoPlausibleEntriesRemainUnconfirmed() {
        War existing = ended(war(SHIFTED_START, SHIFTED_END));
        candidates(List.of(finalEntry(FINAL_END, 33, OPPONENT),
                finalEntry(FINAL_END.plusSeconds(60), 33, OPPONENT)));

        assertTrue(service.syncEndedWarsFromWarLog(CLAN).isEmpty());
        assertEquals(SHIFTED_END, existing.getEndTime());
        assertNull(existing.getWarLogSyncedAt());
    }

    @Test
    void incompleteCandidateDoesNotMakeAnotherCandidateUnambiguous() {
        War existing = ended(war(SHIFTED_START, SHIFTED_END));
        candidates(List.of(finalEntry(FINAL_END, null, OPPONENT),
                finalEntry(FINAL_END.plusSeconds(60), 33, OPPONENT)));

        assertTrue(service.syncEndedWarsFromWarLog(CLAN).isEmpty());
        assertNull(existing.getWarLogSyncedAt());
    }

    @Test
    void incompleteFinalEntryDoesNotAlterTheScheduleOrConfirmation() {
        War existing = ended(war(SHIFTED_START, SHIFTED_END));
        candidates(List.of(finalEntry(FINAL_END, null, OPPONENT)));

        assertTrue(service.syncEndedWarsFromWarLog(CLAN).isEmpty());
        assertEquals(SHIFTED_END, existing.getEndTime());
        assertNull(existing.getWarLogSyncedAt());
    }

    @Test
    void opponentMismatchIsNotAcceptedEvenForAnExactEndTime() {
        War existing = ended(war(SHIFTED_START, SHIFTED_END));
        when(warLog.findByClanTagAndEndTime(CLAN, SHIFTED_END))
                .thenReturn(Optional.of(finalEntry(SHIFTED_END, 33, "#OTHER")));

        assertTrue(service.syncEndedWarsFromWarLog(CLAN).isEmpty());
        assertNull(existing.getWarLogSyncedAt());
    }

    @Test
    void anAlreadyAssignedFinalEntryCannotConfirmAnotherWar() {
        War existing = ended(war(SHIFTED_START, SHIFTED_END));
        when(warLog.findByClanTagAndEndTime(CLAN, SHIFTED_END))
                .thenReturn(Optional.of(finalEntry(SHIFTED_END, 33, OPPONENT)));
        when(repository.existsByClanTagAndEndTimeAndWarLogSyncedAtIsNotNull(CLAN, SHIFTED_END)).thenReturn(true);

        assertTrue(service.syncEndedWarsFromWarLog(CLAN).isEmpty());
        assertNull(existing.getWarLogSyncedAt());
    }

    @Test
    void legacyDuplicatesAreNotBothAttachedToTheSameFinalEntry() {
        War first = war(ORIGINAL_START, ORIGINAL_END);
        War second = war(SHIFTED_START, SHIFTED_END);
        when(repository.findAllByClanTagAndEndTimeLessThanEqualAndWarLogSyncedAtIsNull(CLAN, NOW))
                .thenReturn(List.of(first, second));
        identity(first, List.of(first, second));

        assertTrue(service.syncEndedWarsFromWarLog(CLAN).isEmpty());
        assertNull(first.getWarLogSyncedAt());
        assertNull(second.getWarLogSyncedAt());
        verifyNoInteractions(warLog);
    }

    private WarService serviceAt(Instant now) {
        return new WarService(client, repository, Clock.fixed(now, ZoneOffset.UTC), participants, attacks, warLog);
    }

    private War ended(War existing) {
        when(repository.findAllByClanTagAndEndTimeLessThanEqualAndWarLogSyncedAtIsNull(CLAN, NOW))
                .thenReturn(List.of(existing));
        identity(existing, List.of(existing));
        return existing;
    }

    private void identity(War existing, List<War> matches) {
        when(repository.findAllByClanTagAndOpponentClanTagAndPreparationStartTimeOrderByIdAsc(
                existing.getClanTag(), existing.getOpponentClanTag(), existing.getPreparationStartTime()))
                .thenReturn(matches);
    }

    private void candidates(List<WarLogEntry> entries) {
        when(repository.findFirstByClanTagAndPreparationStartTimeGreaterThanOrderByPreparationStartTimeAsc(
                CLAN, PREPARATION)).thenReturn(Optional.of(nextWar()));
        when(warLog.findByClanAndOpponentAndEndTimeRange(CLAN, OPPONENT, SHIFTED_START, NEXT_PREPARATION))
                .thenReturn(entries);
    }
}
