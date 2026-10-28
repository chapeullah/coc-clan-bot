package com.chapeullah.cocclanbot.telegram.command;

import com.chapeullah.cocclanbot.clan.service.ClanService;
import com.chapeullah.cocclanbot.member.MemberService;
import com.chapeullah.cocclanbot.raidseason.model.RaidSeason;
import com.chapeullah.cocclanbot.raidseason.model.RaidSeasonClanMemberSnapshot;
import com.chapeullah.cocclanbot.raidseason.model.RaidSeasonParticipant;
import com.chapeullah.cocclanbot.raidseason.service.RaidSeasonClanMemberSnapshotService;
import com.chapeullah.cocclanbot.raidseason.service.RaidSeasonParticipantService;
import com.chapeullah.cocclanbot.raidseason.service.RaidSeasonService;
import com.chapeullah.cocclanbot.rating.MemberRaidStatistic;
import com.chapeullah.cocclanbot.rating.RatingService;
import com.chapeullah.cocclanbot.rating.WarRatingService;
import com.chapeullah.cocclanbot.telegram.TelegramAccessService;
import com.chapeullah.cocclanbot.telegram.dto.TelegramChat;
import com.chapeullah.cocclanbot.telegram.dto.TelegramMessage;
import com.chapeullah.cocclanbot.telegram.dto.TelegramUser;
import com.chapeullah.cocclanbot.telegram.groupchatid.TelegramGroupChatIdService;
import com.chapeullah.cocclanbot.telegram.groupchatthreads.TelegramGroupChatThreadsService;
import com.chapeullah.cocclanbot.war.service.WarAttackService;
import com.chapeullah.cocclanbot.war.service.WarParticipantService;
import com.chapeullah.cocclanbot.war.service.WarService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static com.chapeullah.cocclanbot.telegram.command.BotMessages.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BotCommandMessageServiceTest {

    private static final String CLAN_TAG = "#CLAN";

    @Mock private ClanService clanService;
    @Mock private MemberService memberService;
    @Mock private WarService warService;
    @Mock private WarParticipantService warParticipantService;
    @Mock private WarAttackService warAttackService;
    @Mock private RaidSeasonService raidSeasonService;
    @Mock private RaidSeasonParticipantService participantService;
    @Mock private RaidSeasonClanMemberSnapshotService snapshotService;
    @Mock private RatingService ratingService;
    @Mock private WarRatingService warRatingService;
    @Mock private TelegramGroupChatThreadsService threadsService;
    @Mock private TelegramGroupChatIdService groupChatService;
    @Mock private TelegramAccessService accessService;
    @Mock private ObjectProvider<BotCommandService> commandService;
    @InjectMocks private BotCommandMessageService service;

    static Stream<Arguments> invalidMemberRequests() {
        return Stream.of(
                Arguments.of(null, MEMBER_TAG_REQUIRED),
                Arguments.of("", MEMBER_TAG_REQUIRED),
                Arguments.of("  ", MEMBER_TAG_REQUIRED),
                Arguments.of("#", INVALID_MEMBER_TAG_FORMAT),
                Arguments.of("##ABC", INVALID_MEMBER_TAG_FORMAT),
                Arguments.of("bad tag", INVALID_MEMBER_TAG_FORMAT));
    }

    @ParameterizedTest
    @MethodSource("invalidMemberRequests")
    void invalidMemberRequestReturnsBeforeQueryingData(String args, String expected) {
        assertThat(service.getMemberMessage(CLAN_TAG, args)).isEqualTo(expected);
        verifyNoInteractions(memberService);
    }

    @Test
    void memberTagIsNormalizedBeforeLookup() {
        when(memberService.findByTagAndClanTag("#AB12", CLAN_TAG)).thenReturn(Optional.empty());

        assertThat(service.getMemberMessage(CLAN_TAG, " ab12 ")).isEqualTo(MEMBER_NOT_FOUND);
        verify(memberService).findByTagAndClanTag("#AB12", CLAN_TAG);
    }

    @Test
    void emptyRosterDoesNotCalculateRatings() {
        when(memberService.getAllMembersByClanTag(CLAN_TAG)).thenReturn(List.of());

        assertThat(service.getMembersMessage(CLAN_TAG)).isEqualTo(NO_MEMBERS);
        verifyNoInteractions(ratingService);
    }

    @Test
    void missingRaidDoesNotLoadParticipantsOrSnapshots() {
        when(raidSeasonService.findLatestByClanTag(CLAN_TAG)).thenReturn(Optional.empty());

        assertThat(service.getRaidMessage(CLAN_TAG)).isEqualTo(NO_RAID_SEASONS);
        assertThat(service.getRaidMissedMessage(CLAN_TAG)).isEqualTo(RAID_NOT_FOUND);
        verifyNoInteractions(participantService, snapshotService);
    }

    @Test
    void inconsistentRaidDoesNotLoadRosterSnapshot() {
        RaidSeason season = mock(RaidSeason.class);
        RaidSeasonParticipant participant = mock(RaidSeasonParticipant.class);
        when(raidSeasonService.findLatestByClanTag(CLAN_TAG)).thenReturn(Optional.of(season));
        when(season.getTotalAttacks()).thenReturn(3);
        when(participant.getAttackCount()).thenReturn(2);
        when(participantService.getByRaidSeason(season)).thenReturn(List.of(participant));

        assertThat(service.getRaidMissedMessage(CLAN_TAG)).isEqualTo(RAID_ATTACK_DATA_INCONSISTENT);
        verifyNoInteractions(snapshotService);
    }

    @Test
    void missingSnapshotHasItsOwnResponse() {
        RaidSeason season = mock(RaidSeason.class);
        when(raidSeasonService.findLatestByClanTag(CLAN_TAG)).thenReturn(Optional.of(season));
        when(season.getTotalAttacks()).thenReturn(0);
        when(participantService.getByRaidSeason(season)).thenReturn(List.of());
        when(snapshotService.getByRaidSeason(season)).thenReturn(List.of());

        assertThat(service.getRaidMissedMessage(CLAN_TAG)).isEqualTo(RAID_SNAPSHOT_NOT_FOUND);
    }

    @Test
    void missedRaidKeepsUnknownEligibilityAndExcludesAttackedOrIneligibleMembers() {
        RaidSeason season = mock(RaidSeason.class);
        RaidSeasonParticipant participant = mock(RaidSeasonParticipant.class);
        when(raidSeasonService.findLatestByClanTag(CLAN_TAG)).thenReturn(Optional.of(season));
        when(season.getTotalAttacks()).thenReturn(1);
        when(season.getState()).thenReturn("ended");
        when(season.getStartTime()).thenReturn(Instant.parse("2026-10-23T07:00:00Z"));
        when(season.getEndTime()).thenReturn(Instant.parse("2026-10-26T07:00:00Z"));
        when(participant.getAttackCount()).thenReturn(1);
        when(participant.getTag()).thenReturn("#ATTACKED");
        when(participantService.getByRaidSeason(season)).thenReturn(List.of(participant));
        when(snapshotService.getByRaidSeason(season)).thenReturn(List.of(
                snapshot(season, "#ATTACKED", "Attacked", true),
                snapshot(season, "#EXCLUDED", "Excluded", false),
                snapshot(season, "#UNKNOWN", "Unknown <&> 😀", null),
                snapshot(season, "#ELIGIBLE", "Eligible", true)));

        String result = service.getRaidMissedMessage(CLAN_TAG);

        assertThat(result)
                .contains("01. Unknown &lt;&amp;&gt; □", "02. Eligible",
                        "23.10.2026 10:00 — 26.10.2026 10:00", "Статус: Завершён")
                .doesNotContain("#ATTACKED", "#EXCLUDED", "<&>");
    }

    @Test
    void incompleteRaidCountRemainsPending() {
        RaidSeason season = mock(RaidSeason.class);
        when(raidSeasonService.findLatestByClanTag(CLAN_TAG)).thenReturn(Optional.of(season));
        when(season.getTotalAttacks()).thenReturn(2);
        when(season.getStartTime()).thenReturn(Instant.parse("2026-10-23T07:00:00Z"));
        when(season.getEndTime()).thenReturn(Instant.parse("2026-10-26T07:00:00Z"));
        when(participantService.getByRaidSeason(season)).thenReturn(List.of());

        assertThat(service.getRaidMessage(CLAN_TAG)).contains("Участников с атаками: Уточняется");
    }

    @ParameterizedTest
    @ValueSource(strings = {"setGroup", "setWarThread", "setMainThread",
            "resetGroup", "resetWarThread", "resetMainThread"})
    void settingsRejectNonAdministratorBeforeReadingOrChangingSettings(String operation) {
        TelegramMessage message = message(-100, "supergroup", 42, true);
        String result = switch (operation) {
            case "setGroup" -> service.getSetGroupMessage(message);
            case "setWarThread" -> service.getSetWarThreadMessage(message);
            case "setMainThread" -> service.getSetMainThreadMessage(message);
            case "resetGroup" -> service.getResetGroupMessage(message);
            case "resetWarThread" -> service.getResetWarThreadMessage(message);
            case "resetMainThread" -> service.getResetMainThreadMessage(message);
            default -> throw new IllegalArgumentException(operation);
        };

        assertThat(result).isEqualTo(ACCESS_DENIED);
        verifyNoInteractions(groupChatService, threadsService);
    }

    @Test
    void assigningSameGroupPreservesThreadSettings() {
        when(accessService.isAdmin(7L)).thenReturn(true);
        when(groupChatService.getChatId()).thenReturn(Optional.of(-100L));

        assertThat(service.getSetGroupMessage(message(-100, "supergroup", null, false))).isEqualTo(GROUP_SET);
        verify(groupChatService, never()).upsertChatId(anyLong());
        verifyNoInteractions(threadsService);
    }

    @Test
    void changingGroupResetsThreadSettings() {
        when(accessService.isAdmin(7L)).thenReturn(true);
        when(groupChatService.getChatId()).thenReturn(Optional.of(-200L));

        assertThat(service.getSetGroupMessage(message(-100, "supergroup", null, false))).isEqualTo(GROUP_SET);
        var order = inOrder(groupChatService, threadsService);
        order.verify(groupChatService).upsertChatId(-100L);
        order.verify(threadsService).resetThreadIds();
    }

    @Test
    void threadCommandRejectsAnotherGroupWithoutChangingSettings() {
        when(accessService.isAdmin(7L)).thenReturn(true);
        when(groupChatService.getChatId()).thenReturn(Optional.of(-200L));

        assertThat(service.getSetWarThreadMessage(message(-100, "supergroup", 42, true))).isEqualTo(WRONG_GROUP);
        verifyNoInteractions(threadsService);
    }

    @Test
    void warThreadRequiresAnActualForumTopic() {
        when(accessService.isAdmin(7L)).thenReturn(true);
        when(groupChatService.getChatId()).thenReturn(Optional.of(-100L));

        assertThat(service.getSetWarThreadMessage(message(-100, "supergroup", 42, false))).isEqualTo(INVALID_THREAD);
        verifyNoInteractions(threadsService);
    }

    @Test
    void mainThreadCanBeTheRootChat() {
        when(accessService.isAdmin(7L)).thenReturn(true);
        when(groupChatService.getChatId()).thenReturn(Optional.of(-100L));

        assertThat(service.getSetMainThreadMessage(message(-100, "supergroup", null, false)))
                .isEqualTo(MAIN_THREAD_SET);
        verify(threadsService).upsertMainThreadId(null);
    }

    @Test
    void helpWithoutSenderFailsBeforeLookingUpCommands() {
        TelegramMessage message = new TelegramMessage(1, 0,
                new TelegramChat(-100, "supergroup", null, null, null, null),
                null, null, "/help", null, null, null, false);

        assertThatThrownBy(() -> service.getHelpMessage(message))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Command sender is missing");
        verifyNoInteractions(accessService, commandService);
    }

    @Test
    void raidRatingPreservesSortingAndPlacesMissingRatingsLast() {
        when(ratingService.calculateClanMemberRaidStatistics(CLAN_TAG)).thenReturn(Map.of(
                "#B", raidStatistic("#B", "beta", 1.0),
                "#A", raidStatistic("#A", "alpha", 1.0),
                "#H", raidStatistic("#H", "High", 2.0),
                "#N", raidStatistic("#N", "none", null)));

        assertThat(service.getRaidRatingMessage(CLAN_TAG)).contains(
                "<b>01. High</b> — 2.00",
                "<b>02. alpha</b> — 1.00",
                "<b>03. beta</b> — 1.00",
                "<b>04. none</b> — Нет данных");
    }

    private static RaidSeasonClanMemberSnapshot snapshot(
            RaidSeason season, String tag, String name, Boolean eligible) {
        return new RaidSeasonClanMemberSnapshot(season, tag, name, null, null, "member", "open", eligible);
    }

    private static MemberRaidStatistic raidStatistic(String tag, String name, Double rating) {
        return new MemberRaidStatistic(tag, name, Optional.ofNullable(rating), 0, 0L, 0, 0, 0,
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    private static TelegramMessage message(long chatId, String chatType, Integer threadId, boolean isTopic) {
        return new TelegramMessage(1, 0,
                new TelegramChat(chatId, chatType, null, null, null, null),
                new TelegramUser(7, null, null, null, false),
                threadId, null, null, null, null, isTopic);
    }
}
