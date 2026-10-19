package com.chapeullah.guccigoblin.raidseason.service;

import com.chapeullah.guccigoblin.clan.model.Clan;
import com.chapeullah.guccigoblin.clan.service.ClanService;
import com.chapeullah.guccigoblin.member.MemberService;
import com.chapeullah.guccigoblin.member.model.Member;
import com.chapeullah.guccigoblin.raidseason.model.RaidSeason;
import com.chapeullah.guccigoblin.raidseason.model.RaidSeasonClanMemberSnapshot;
import com.chapeullah.guccigoblin.raidseason.model.RaidSeasonParticipant;
import com.chapeullah.guccigoblin.raidseason.repository.RaidSeasonClanMemberSnapshotRepository;
import com.chapeullah.guccigoblin.telegram.TelegramCommandService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RaidSeasonClanMemberSnapshotServiceTest {

    @Test
    void raidMissedExcludesOnlyConfirmedIneligibleMembers() {
        Fixture fixture = new Fixture("open");
        fixture.addMember("#LATE", "Late", "member", fixture.start.plusSeconds(60),
                fixture.start.plusSeconds(30));
        fixture.addMember("#OLD", "Old", "member", fixture.start.minusSeconds(60), null);
        fixture.addMember("#UNKNOWN", "Unknown", "member", fixture.start.plusSeconds(60), null);

        fixture.service.syncClanMemberSnapshots(fixture.season);
        String message = fixture.commands().getRaidMissedMessage();

        assertFalse(message.contains("#LATE"), message);
        assertTrue(message.contains("#OLD"), message);
        assertTrue(message.contains("#UNKNOWN"), message);
    }

    @ParameterizedTest
    @CsvSource({
            "admin, open",
            "coLeader, open",
            "leader, open",
            "member, inviteOnly",
            "member, closed"
    })
    void lateMembersWithPermissionRemainInRaidMissed(String role, String clanType) {
        Fixture fixture = new Fixture(clanType);
        fixture.addMember("#PLAYER", "Player", role, fixture.start.plusSeconds(60),
                fixture.start.plusSeconds(30));

        fixture.service.syncClanMemberSnapshots(fixture.season);

        assertEquals(Boolean.TRUE, fixture.snapshots.getFirst().getParticipationEligible());
        assertTrue(fixture.commands().getRaidMissedMessage().contains("#PLAYER"));
    }

    @Test
    void firstObservationDuringRaidDoesNotProveLateJoining() {
        Fixture fixture = new Fixture("open");
        fixture.addMember("#BOOTSTRAP", "Bootstrap", "member",
                fixture.start.plusSeconds(60), null);

        fixture.service.syncClanMemberSnapshots(fixture.season);

        assertNull(fixture.snapshots.getFirst().getParticipationEligible());
    }

    @Test
    void legacySnapshotWithoutJoiningDatesStaysUnknown() {
        Fixture fixture = new Fixture("open");
        fixture.addMember("#PLAYER", "Player", "member", fixture.start.plusSeconds(60),
                fixture.start.plusSeconds(30));
        RaidSeasonClanMemberSnapshot legacy = new RaidSeasonClanMemberSnapshot(
                fixture.season, "#PLAYER", "Player", null, null, null, null, null);
        fixture.snapshots.add(legacy);

        fixture.service.syncClanMemberSnapshots(fixture.season);

        assertNull(legacy.getJoinedAt());
        assertNull(legacy.getJoinedAfter());
        assertNull(legacy.getParticipationEligible());
        assertTrue(fixture.commands().getRaidMissedMessage().contains("#PLAYER"));
    }

    @Test
    void rosterIsRefreshedBeforeSnapshotCapture() {
        Fixture fixture = new Fixture("open");
        fixture.beforeClanSync = () -> fixture.addMember("#FRESH", "Fresh", "member",
                fixture.start.plusSeconds(60), fixture.start.plusSeconds(30));

        fixture.service.syncClanMemberSnapshots(fixture.season);

        assertEquals(1, fixture.snapshots.size());
        assertEquals("#FRESH", fixture.snapshots.getFirst().getTag());
        assertEquals(Boolean.FALSE, fixture.snapshots.getFirst().getParticipationEligible());
    }

    @Test
    void failedRosterRefreshDoesNotRewriteSnapshotUsingStaleData() {
        Fixture fixture = new Fixture("open");
        Member member = fixture.addMember("#PLAYER", "Player", "member",
                fixture.start.plusSeconds(60), fixture.start.plusSeconds(30));
        fixture.service.syncClanMemberSnapshots(fixture.season);
        ReflectionTestUtils.setField(member, "role", "admin");
        fixture.beforeClanSync = () -> {
            throw new IllegalStateException("Clan API unavailable");
        };

        assertThrows(IllegalStateException.class,
                () -> fixture.service.syncClanMemberSnapshots(fixture.season));

        assertEquals("member", fixture.snapshots.getFirst().getRole());
        assertEquals(Boolean.FALSE, fixture.snapshots.getFirst().getParticipationEligible());
    }

    @Test
    void pollThatStraddlesRaidStartDoesNotProveLateJoining() {
        Fixture fixture = new Fixture("open");
        fixture.addMember("#BOUNDARY", "Boundary", "member",
                fixture.start.plusSeconds(60), fixture.start.minusSeconds(60));

        fixture.service.syncClanMemberSnapshots(fixture.season);

        assertNull(fixture.snapshots.getFirst().getParticipationEligible());
        assertTrue(fixture.commands().getRaidMissedMessage().contains("#BOUNDARY"));
    }

    @Test
    void previousConfirmedAbsenceAtRaidStartProvesLateJoining() {
        Fixture fixture = new Fixture("open");
        fixture.addMember("#BOUNDARY", "Boundary", "member",
                fixture.start.plusSeconds(60), fixture.start);

        fixture.service.syncClanMemberSnapshots(fixture.season);

        assertEquals(Boolean.FALSE, fixture.snapshots.getFirst().getParticipationEligible());
    }

    @Test
    void memberObservedAtRaidStartIsEligible() {
        Fixture fixture = new Fixture("open");
        fixture.addMember("#EXISTING", "Existing", "member", fixture.start, null);

        fixture.service.syncClanMemberSnapshots(fixture.season);

        assertEquals(Boolean.TRUE, fixture.snapshots.getFirst().getParticipationEligible());
    }

    @Test
    void promotionUpdatesPermissionAndLaterDemotionKeepsIt() {
        Fixture fixture = new Fixture("open");
        Member member = fixture.addMember("#PLAYER", "Player", "member",
                fixture.start.plusSeconds(60), fixture.start.plusSeconds(30));
        fixture.service.syncClanMemberSnapshots(fixture.season);
        assertEquals(Boolean.FALSE, fixture.snapshots.getFirst().getParticipationEligible());

        ReflectionTestUtils.setField(member, "role", "admin");
        fixture.service.syncClanMemberSnapshots(fixture.season);
        assertEquals(Boolean.TRUE, fixture.snapshots.getFirst().getParticipationEligible());

        ReflectionTestUtils.setField(member, "role", "member");
        fixture.service.syncClanMemberSnapshots(fixture.season);

        assertEquals(1, fixture.snapshots.size());
        assertEquals(Boolean.TRUE, fixture.snapshots.getFirst().getParticipationEligible());
        assertTrue(fixture.commands().getRaidMissedMessage().contains("#PLAYER"));
    }

    @Test
    void observedAttackOverridesLateJoinRestriction() {
        Fixture fixture = new Fixture("open");
        fixture.addMember("#PLAYER", "Player", "member", fixture.start.plusSeconds(60),
                fixture.start.plusSeconds(30));
        fixture.service.syncClanMemberSnapshots(fixture.season);
        assertEquals(Boolean.FALSE, fixture.snapshots.getFirst().getParticipationEligible());

        fixture.season.setParticipants(List.of(new RaidSeasonParticipant(
                fixture.season, "#PLAYER", "Player", 1, 5, 1, 1000)));
        ReflectionTestUtils.setField(fixture.season, "totalAttacks", 1);
        fixture.service.syncClanMemberSnapshots(fixture.season);

        assertEquals(Boolean.TRUE, fixture.snapshots.getFirst().getParticipationEligible());
        assertFalse(fixture.commands().getRaidMissedMessage().contains("#PLAYER"));
    }

    @Test
    void completedSnapshotIsNotRewrittenFromCurrentMembership() {
        Fixture fixture = new Fixture("open");
        Member member = fixture.addMember("#PLAYER", "Player", "member",
                fixture.start.plusSeconds(60), fixture.start.plusSeconds(30));
        fixture.service.syncClanMemberSnapshots(fixture.season);
        RaidSeasonClanMemberSnapshot snapshot = fixture.snapshots.getFirst();
        Instant capturedJoinedAt = snapshot.getJoinedAt();
        Instant capturedJoinedAfter = snapshot.getJoinedAfter();

        ReflectionTestUtils.setField(fixture.season, "state", "ended");
        ReflectionTestUtils.setField(fixture.season, "endTime", Instant.now().minusSeconds(60));
        member.leave(Instant.now().minusSeconds(30));
        member.rejoin(Instant.now(), Instant.now().minusSeconds(10));
        ReflectionTestUtils.setField(member, "role", "admin");
        fixture.clan.setType("closed");

        fixture.service.syncClanMemberSnapshots(fixture.season);

        assertEquals(capturedJoinedAt, snapshot.getJoinedAt());
        assertEquals(capturedJoinedAfter, snapshot.getJoinedAfter());
        assertEquals("member", snapshot.getRole());
        assertEquals("open", snapshot.getClanType());
        assertEquals(Boolean.FALSE, snapshot.getParticipationEligible());
        assertEquals(1, fixture.snapshots.size());
    }

    @Test
    void completedSeasonWithoutSnapshotCannotUseCurrentRoster() {
        Fixture fixture = new Fixture("open");
        fixture.addMember("#PLAYER", "Player", "member", fixture.start.minusSeconds(60), null);
        ReflectionTestUtils.setField(fixture.season, "state", "ended");
        ReflectionTestUtils.setField(fixture.season, "endTime", Instant.now().minusSeconds(60));

        fixture.service.syncClanMemberSnapshots(fixture.season);

        assertTrue(fixture.snapshots.isEmpty());
    }

    private static class Fixture {

        private final Instant start = Instant.now().minus(1, ChronoUnit.HOURS);
        private final Clan clan;
        private final RaidSeason season;
        private final List<Member> members = new ArrayList<>();
        private final List<RaidSeasonClanMemberSnapshot> snapshots = new ArrayList<>();
        private final RaidSeasonClanMemberSnapshotService service;
        private Runnable beforeClanSync = () -> {};

        private Fixture(String clanType) {
            clan = new Clan("#CLAN", "Clan", clanType, null, null, null,
                    null, null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null);
            clan.setMemberListSyncedAt(Instant.now());
            season = new RaidSeason(clan, "ongoing", start,
                    Instant.now().plus(1, ChronoUnit.HOURS), 0, 0, 0, 0, 0, 0, "{}");

            ClanService clanService = new ClanService(
                    null, null, null, null, null, null, null, null, null, null, null) {
                @Override
                public Clan getClanByTag(String clanTag) {
                    return clan;
                }

                @Override
                public Clan syncClan(String clanTag) {
                    beforeClanSync.run();
                    return clan;
                }
            };
            MemberService memberService = new MemberService(null, null, null, null, null) {
                @Override
                public List<Member> getCurrentMembersByClanTag(String clanTag) {
                    return members;
                }

                @Override
                public List<Member> getAllMembersByClanTag(String clanTag) {
                    return members;
                }
            };
            RaidSeasonClanMemberSnapshotRepository repository =
                    (RaidSeasonClanMemberSnapshotRepository) Proxy.newProxyInstance(
                            RaidSeasonClanMemberSnapshotRepository.class.getClassLoader(),
                            new Class<?>[]{RaidSeasonClanMemberSnapshotRepository.class},
                            (proxy, method, args) -> switch (method.getName()) {
                                case "findAllByRaidSeason" -> new ArrayList<>(snapshots);
                                case "save" -> {
                                    RaidSeasonClanMemberSnapshot snapshot =
                                            (RaidSeasonClanMemberSnapshot) args[0];
                                    if (!snapshots.contains(snapshot)) {
                                        snapshots.add(snapshot);
                                    }
                                    yield snapshot;
                                }
                                default -> throw new UnsupportedOperationException(method.getName());
                            });
            service = new RaidSeasonClanMemberSnapshotService(clanService, memberService, repository);
        }

        private Member addMember(String tag, String name, String role,
                                 Instant joinedAt, Instant joinedAfter) {
            Member member = new Member(tag, clan, name, role, 16, 200,
                    null, 5000, 1000, members.size() + 1, null, 0, 0, null, joinedAfter);
            ReflectionTestUtils.setField(member, "joinedAt", joinedAt);
            members.add(member);
            return member;
        }

        private TelegramCommandService commands() {
            RaidSeasonService seasonService = new RaidSeasonService(
                    null, null, null, null, null, null) {
                @Override
                public Optional<RaidSeason> findLatestByClanTag(String clanTag) {
                    return Optional.of(season);
                }
            };
            RaidSeasonParticipantService participantService = new RaidSeasonParticipantService(null) {
                @Override
                public List<RaidSeasonParticipant> getByRaidSeason(RaidSeason raidSeason) {
                    return season.getParticipants();
                }
            };
            TelegramCommandService commands = new TelegramCommandService(
                    null, null, null, seasonService, participantService, service,
                    null, null, null, null);
            ReflectionTestUtils.setField(commands, "clanTag", clan.getTag());
            return commands;
        }
    }
}
