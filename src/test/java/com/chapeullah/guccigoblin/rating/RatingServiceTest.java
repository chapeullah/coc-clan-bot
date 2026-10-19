package com.chapeullah.guccigoblin.rating;

import com.chapeullah.guccigoblin.clan.model.Clan;
import com.chapeullah.guccigoblin.member.model.Member;
import com.chapeullah.guccigoblin.raidseason.model.RaidSeason;
import com.chapeullah.guccigoblin.raidseason.model.RaidSeasonClanMemberSnapshot;
import com.chapeullah.guccigoblin.raidseason.model.RaidSeasonParticipant;
import com.chapeullah.guccigoblin.raidseason.service.RaidSeasonClanMemberSnapshotService;
import com.chapeullah.guccigoblin.raidseason.service.RaidSeasonParticipantService;
import com.chapeullah.guccigoblin.raidseason.service.RaidSeasonService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RatingServiceTest {

    private static final RaidAverages REFERENCE_AVERAGES = new RaidAverages(4.0, 1.0, 1000.0);

    @Test
    void ineligibleMembersAndSeasonsAreExcludedFromBothDenominators() {
        Fixture fixture = new Fixture();
        RaidSeason participated = fixture.addSeason();
        fixture.addSnapshot(participated, "#PLAYER", true);
        fixture.addSnapshot(participated, "#INELIGIBLE", false);
        fixture.addParticipant(participated, "#PLAYER");
        RaidSeason joinedLate = fixture.addSeason();
        fixture.addSnapshot(joinedLate, "#PLAYER", false);

        assertEquals(REFERENCE_AVERAGES,
                fixture.service.calculateRaidAverages(fixture.seasons).orElseThrow());
        assertEquals(1.0, fixture.service.calculateMemberRaidRating(
                REFERENCE_AVERAGES, fixture.member).orElseThrow(), 0.000001);
    }

    @Test
    void legacyUnknownEligibilityStillCountsAsAnEligibleMissedSeason() {
        Fixture fixture = new Fixture();
        RaidSeason participated = fixture.addSeason();
        fixture.addSnapshot(participated, "#PLAYER", true);
        fixture.addParticipant(participated, "#PLAYER");
        RaidSeason legacy = fixture.addSeason();
        fixture.addSnapshot(legacy, "#PLAYER", null);

        assertEquals(new RaidAverages(2.0, 0.5, 500.0),
                fixture.service.calculateRaidAverages(fixture.seasons).orElseThrow());
        assertEquals(0.5, fixture.service.calculateMemberRaidRating(
                REFERENCE_AVERAGES, fixture.member).orElseThrow(), 0.000001);
    }

    @Test
    void actualAttacksOverrideIneligibleSnapshotForBothCalculations() {
        Fixture fixture = new Fixture();
        RaidSeason season = fixture.addSeason();
        fixture.addSnapshot(season, "#PLAYER", false);
        fixture.addParticipant(season, "#PLAYER");

        assertEquals(REFERENCE_AVERAGES,
                fixture.service.calculateRaidAverages(fixture.seasons).orElseThrow());
        assertEquals(1.0, fixture.service.calculateMemberRaidRating(
                REFERENCE_AVERAGES, fixture.member).orElseThrow(), 0.000001);
    }

    @Test
    void allIneligibleReturnsEmptyInsteadOfDividingByZero() {
        Fixture fixture = new Fixture();
        RaidSeason season = fixture.addSeason();
        fixture.addSnapshot(season, "#PLAYER", false);
        fixture.addSnapshot(season, "#OTHER", false);

        assertTrue(fixture.service.calculateRaidAverages(fixture.seasons).isEmpty());
        assertTrue(fixture.service.calculateMemberRaidRating(
                REFERENCE_AVERAGES, fixture.member).isEmpty());
    }

    private static class Fixture {

        private final Clan clan = new Clan("#CLAN", "Clan", "open", null, null, null,
                null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null);
        private final Member member = new Member("#PLAYER", clan, "Player", "member", 16, 200,
                null, 5000, 1000, 1, null, 0, 0, null, null);
        private final List<RaidSeason> seasons = new ArrayList<>();
        private final RatingService service;

        private Fixture() {
            RaidSeasonService seasonService = new RaidSeasonService(
                    null, null, null, null, null, null) {
                @Override
                public List<RaidSeason> getRecentCompletedByClanTag(String clanTag, int limit) {
                    return seasons;
                }
            };
            RaidSeasonClanMemberSnapshotService snapshotService =
                    new RaidSeasonClanMemberSnapshotService(null, null, null) {
                        @Override
                        public List<RaidSeasonClanMemberSnapshot> getByRaidSeason(RaidSeason season) {
                            return season.getMemberSnapshots();
                        }
                    };
            RaidSeasonParticipantService participantService = new RaidSeasonParticipantService(null) {
                @Override
                public List<RaidSeasonParticipant> getByRaidSeason(RaidSeason season) {
                    return season.getParticipants();
                }
            };
            service = new RatingService(seasonService, null, snapshotService, participantService, null);
        }

        private RaidSeason addSeason() {
            Instant start = Instant.parse("2026-09-04T07:00:00Z")
                    .plus(seasons.size() * 7L, ChronoUnit.DAYS);
            RaidSeason season = new RaidSeason(clan, "ended", start,
                    start.plus(3, ChronoUnit.DAYS), 0, 0, 0, 0, 0, 0, "{}");
            seasons.add(season);
            return season;
        }

        private void addSnapshot(RaidSeason season, String tag, Boolean participationEligible) {
            season.getMemberSnapshots().add(new RaidSeasonClanMemberSnapshot(
                    season, tag, tag, null, null, "member", "open", participationEligible));
        }

        private void addParticipant(RaidSeason season, String tag) {
            season.getParticipants().add(new RaidSeasonParticipant(
                    season, tag, tag, 4, 5, 1, 1000));
        }
    }
}
