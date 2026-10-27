package com.chapeullah.cocclanbot.rating;

import com.chapeullah.cocclanbot.member.MemberService;
import com.chapeullah.cocclanbot.member.model.Member;
import com.chapeullah.cocclanbot.raidseason.model.RaidSeason;
import com.chapeullah.cocclanbot.raidseason.model.RaidSeasonClanMemberSnapshot;
import com.chapeullah.cocclanbot.raidseason.model.RaidSeasonParticipant;
import com.chapeullah.cocclanbot.raidseason.service.RaidSeasonClanMemberSnapshotService;
import com.chapeullah.cocclanbot.raidseason.service.RaidSeasonParticipantService;
import com.chapeullah.cocclanbot.raidseason.service.RaidSeasonService;
import com.chapeullah.cocclanbot.war.model.War;
import com.chapeullah.cocclanbot.war.model.WarAttack;
import com.chapeullah.cocclanbot.war.model.WarParticipant;
import com.chapeullah.cocclanbot.war.service.WarService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RatingService {

    private static final int RAID_SEASON_LIMIT = 4;
    private static final int RAID_BASE_ATTACK_LIMIT = 5;

    private final RaidSeasonService raidSeasonService;
    private final RaidSeasonClanMemberSnapshotService raidSeasonClanMemberSnapshotService;
    private final RaidSeasonParticipantService raidSeasonParticipantService;
    private final MemberService memberService;
    private final WarRatingService warRatingService;

    @Transactional(readOnly = true)
    public Map<String, MemberStatistic> calculateClanMemberStatistics(@NonNull String clanTag) {
        List<Member> currentMembers =
                memberService.getCurrentMembersByClanTag(clanTag);

        Map<RaidSeason, RaidAverages> raidAveragesBySeason = calculateRecentRaidAverages(clanTag);

        Map<String, MemberStatistic> statisticsByTag = new HashMap<>();

        for (var member : currentMembers) {
            MemberWarStatistic warStatistic = warRatingService.calculateMemberWarStatistic(member);

            MemberRaidStatistic raidStatistic = calculateMemberRaidStatistic(raidAveragesBySeason, member);

            Optional<Double> warRating = warStatistic.warRating();
            Optional<Double> raidRating = raidStatistic.raidRating();

            double ratingSum = 0.0;
            int ratingCount = 0;

            if (warRating.isPresent()) {
                ratingSum += warRating.get();
                ++ratingCount;
            }

            if (raidRating.isPresent()) {
                ratingSum += raidRating.get();
                ++ratingCount;
            }

            Optional<Double> totalRating = ratingCount == 0
                    ? Optional.empty()
                    : Optional.of(ratingSum / ratingCount);

            statisticsByTag.put(
                    member.getTag(),
                    new MemberStatistic(
                            member.getTag(),
                            member.getName(),

                            totalRating,
                            warRating,
                            raidRating,

                            warStatistic.warCount(),
                            warStatistic.totalWarStars(),
                            warStatistic.warAttackCount(),
                            warStatistic.missedWarAttackCount(),
                            warStatistic.threeStarWarAttackCount(),
                            warStatistic.averageStarsPerAttack(),
                            warStatistic.averageDestructionPercentagePerAttack(),
                            warStatistic.averageAttackDurationSecondsPerAttack(),
                            warStatistic.averageDefenderTownHallLevelPerAttack(),
                            warStatistic.averageTownHallLevelDifferencePerAttack(),
                            warStatistic.averageMapPositionDifferencePerAttack(),
                            warStatistic.threeStarWarAttackPercentage(),
                            warStatistic.warAttackUsagePercentage(),

                            raidStatistic.raidSeasonCount(),
                            raidStatistic.totalRaidLoot(),
                            raidStatistic.missedRaidSeasonCount(),
                            raidStatistic.raidAttackCount(),
                            raidStatistic.missedRaidAttackCount(),
                            raidStatistic.averageRaidAttacksPerSeason(),
                            raidStatistic.averageRaidLootPerSeason(),
                            raidStatistic.averageRaidLootPerAttack(),
                            raidStatistic.averageBonusAttackLimitPerSeason(),
                            raidStatistic.raidAttackUsagePercentage()));
        }
        return statisticsByTag;
    }



    @Transactional(readOnly = true)
    public Map<String, MemberRaidStatistic> calculateClanMemberRaidStatistics(@NonNull String clanTag) {
        List<Member> currentMembers = memberService.getCurrentMembersByClanTag(clanTag);
        Map<RaidSeason, RaidAverages> raidAveragesBySeason = calculateRecentRaidAverages(clanTag);
        Map<String, MemberRaidStatistic> statisticsByTag = new HashMap<>();
        for (var member : currentMembers) {
            statisticsByTag.put(member.getTag(), calculateMemberRaidStatistic(raidAveragesBySeason, member));
        }
        return statisticsByTag;
    }

    private Map<RaidSeason, RaidAverages> calculateRecentRaidAverages(String clanTag) {
        List<RaidSeason> recentCompletedRaidSeasons = raidSeasonService
                .getRecentCompletedByClanTag(clanTag, RAID_SEASON_LIMIT)
                .stream()
                .filter(raidSeason -> !raidSeason.getMemberSnapshots().isEmpty()
                        || raidSeason.getParticipants().stream().anyMatch(this::hasRaidAttacks))
                .toList();

        Map<RaidSeason, RaidAverages> raidAveragesBySeason = new LinkedHashMap<>();
        for (var raidSeason : recentCompletedRaidSeasons) {
            calculateRaidAverages(List.of(raidSeason))
                    .ifPresent(averages -> raidAveragesBySeason.put(raidSeason, averages));
        }
        return raidAveragesBySeason;
    }

    @Transactional(readOnly = true)
    public MemberRaidStatistic calculateMemberRaidStatistic(
            @NonNull Map<RaidSeason, RaidAverages> raidAveragesBySeason,
            @NonNull Member member) {
        int eligibleRaidSeasonCount = 0;
        int missedRaidSeasonCount = 0;
        int attackCountSum = 0;
        int missedRaidAttackCount = 0;
        int bonusAttackLimitSum = 0;
        long resourcesLootedSum = 0L;
        double clanAverageAttackCountSum = 0.0;
        double clanAverageBonusAttackLimitSum = 0.0;
        double clanAverageResourcesLootedSum = 0.0;
        for (var entry : raidAveragesBySeason.entrySet()) {
            RaidSeason raidSeason = entry.getKey();
            RaidSeasonClanMemberSnapshot memberSnapshot = raidSeason.getMemberSnapshots().stream()
                    .filter(snapshot -> Objects.equals(snapshot.getTag(), member.getTag()))
                    .findFirst()
                    .orElse(null);
            RaidSeasonParticipant participant = raidSeason.getParticipants().stream()
                    .filter(p -> Objects.equals(p.getTag(), member.getTag()))
                    .findFirst()
                    .orElse(null);
            if (!isRaidParticipantIncluded(memberSnapshot, participant)) {
                continue;
            }

            ++eligibleRaidSeasonCount;

            RaidAverages seasonAverages = entry.getValue();
            clanAverageAttackCountSum += seasonAverages.attackCount();
            clanAverageBonusAttackLimitSum += seasonAverages.bonusAttackLimit();
            clanAverageResourcesLootedSum += seasonAverages.resourcesLooted();

            if (participant == null) {
                ++missedRaidSeasonCount;
                missedRaidAttackCount += RAID_BASE_ATTACK_LIMIT;
                continue;
            }

            Integer attackCount = participant.getAttackCount();
            if (attackCount == null) {
                throw new IllegalStateException(
                        "Raid season participant attack count is missing: raidSeasonId=%s, tag=\"%s\""
                                .formatted(raidSeason.getId(), participant.getTag()));
            }

            Integer bonusAttackLimit = participant.getBonusAttackLimit();
            if (bonusAttackLimit == null) {
                throw new IllegalStateException(
                        "Raid season participant bonus attack limit is missing: raidSeasonId=%s, tag=\"%s\""
                                .formatted(raidSeason.getId(), participant.getTag()));
            }
            Integer resourcesLooted = participant.getCapitalResourcesLooted();
            if (resourcesLooted == null) {
                throw new IllegalStateException(
                        "Raid season participant resources looted is missing: raidSeasonId=%s, tag=\"%s\""
                                .formatted(raidSeason.getId(), participant.getTag()));
            }

            Integer attackLimit = participant.getAttackLimit();
            if (attackLimit == null
                    || attackLimit < 0
                    || bonusAttackLimit < 0
                    || attackCount < 0
                    || attackCount > attackLimit + bonusAttackLimit) {
                throw new IllegalStateException(
                        ("Invalid raid attack limits: raidSeasonId=%s, tag=\"%s\", "
                                + "attackLimit=%s, bonusAttackLimit=%s, attackCount=%s")
                                .formatted(
                                        raidSeason.getId(),
                                        participant.getTag(),
                                        attackLimit,
                                        bonusAttackLimit,
                                        attackCount));
            }

            if (attackCount == 0) {
                ++missedRaidSeasonCount;
            }

            missedRaidAttackCount += attackLimit + bonusAttackLimit - attackCount;

            attackCountSum += attackCount;
            bonusAttackLimitSum += bonusAttackLimit;
            resourcesLootedSum += resourcesLooted;
        }

        if (eligibleRaidSeasonCount == 0) {
            return MemberRaidStatistic.empty(member.getTag(), member.getName());
        }

        RaidAverages memberAverages = new RaidAverages(
                (double) attackCountSum / eligibleRaidSeasonCount,
                (double) bonusAttackLimitSum / eligibleRaidSeasonCount,
                (double) resourcesLootedSum / eligibleRaidSeasonCount);
        RaidAverages raidAverages = new RaidAverages(
                clanAverageAttackCountSum / eligibleRaidSeasonCount,
                clanAverageBonusAttackLimitSum / eligibleRaidSeasonCount,
                clanAverageResourcesLootedSum / eligibleRaidSeasonCount);

        double ratingSum = 0.0;
        int metricCount = 0;

        if (raidAverages.attackCount() > 0.0) {
            ratingSum += memberAverages.attackCount() / raidAverages.attackCount();
            ++metricCount;
        }

        if (raidAverages.resourcesLooted() > 0.0) {
            ratingSum += memberAverages.resourcesLooted() / raidAverages.resourcesLooted();
            ++metricCount;
        }

        Optional<Double> raidRating = Optional.of(
                metricCount == 0 ? 0.0 : ratingSum / metricCount);

        int availableAttackCount = attackCountSum + missedRaidAttackCount;

        return new MemberRaidStatistic(
                member.getTag(),
                member.getName(),
                raidRating,
                eligibleRaidSeasonCount,
                resourcesLootedSum,
                missedRaidSeasonCount,
                attackCountSum,
                missedRaidAttackCount,
                Optional.of(memberAverages.attackCount()),
                Optional.of(memberAverages.resourcesLooted()),
                attackCountSum == 0
                        ? Optional.empty()
                        : Optional.of((double) resourcesLootedSum / attackCountSum),
                Optional.of(memberAverages.bonusAttackLimit()),
                availableAttackCount == 0
                        ? Optional.empty()
                        : Optional.of(100.0 * attackCountSum / availableAttackCount));
    }

    public Optional<RaidAverages> calculateRaidAverages(@NonNull List<RaidSeason> raidSeasons) {
        if (raidSeasons.isEmpty()) {
            return Optional.empty();
        }

        double averageAttackCountSum = 0.0;
        double averageBonusAttackLimitSum = 0.0;
        double averageResourcesLootedSum = 0.0;
        int eligibleRaidSeasonCount = 0;
        for (var raidSeason : raidSeasons) {
            List<RaidSeasonClanMemberSnapshot> clanMemberSnapshot =
                    raidSeasonClanMemberSnapshotService.getByRaidSeason(raidSeason);

            Map<String, RaidSeasonParticipant> participantsByTag =
                    raidSeasonParticipantService.getByRaidSeason(raidSeason)
                            .stream()
                            .collect(Collectors.toMap(
                                    RaidSeasonParticipant::getTag,
                                    Function.identity()));

            Set<String> includedMemberTags = new HashSet<>();
            for (var memberSnapshot : clanMemberSnapshot) {
                if (isRaidParticipantIncluded(
                        memberSnapshot, participantsByTag.get(memberSnapshot.getTag()))) {
                    includedMemberTags.add(memberSnapshot.getTag());
                }
            }
            participantsByTag.values().stream()
                    .filter(this::hasRaidAttacks)
                    .map(RaidSeasonParticipant::getTag)
                    .forEach(includedMemberTags::add);

            if (includedMemberTags.isEmpty()) {
                continue;
            }

            int attackCountSum = 0;
            int bonusAttackLimitSum = 0;
            long resourcesLootedSum = 0L;
            for (var memberTag : includedMemberTags) {
                var participant = participantsByTag.get(memberTag);
                if (participant == null) {
                    continue;
                }

                Integer attackCount = participant.getAttackCount();
                if (attackCount == null) {
                    throw new IllegalStateException(
                            "Raid season participant attack count is missing: raidSeasonId=%s, tag=\"%s\""
                                    .formatted(raidSeason.getId(), memberTag));
                }
                attackCountSum += attackCount;

                Integer bonusAttackLimit = participant.getBonusAttackLimit();
                if (bonusAttackLimit == null) {
                    throw new IllegalStateException(
                            "Raid season participant bonus attack limit is missing: raidSeasonId=%s, tag=\"%s\""
                                    .formatted(raidSeason.getId(), memberTag));
                }
                bonusAttackLimitSum += bonusAttackLimit;

                Integer capitalResourcesLooted = participant.getCapitalResourcesLooted();
                if (capitalResourcesLooted == null) {
                    throw new IllegalStateException(
                            "Raid season participant capital resources looted are missing: raidSeasonId=%s, tag=\"%s\""
                                    .formatted(raidSeason.getId(), memberTag));
                }
                resourcesLootedSum += capitalResourcesLooted;
            }

            averageAttackCountSum += (double) attackCountSum / includedMemberTags.size();
            averageBonusAttackLimitSum += (double) bonusAttackLimitSum / includedMemberTags.size();
            averageResourcesLootedSum += (double) resourcesLootedSum / includedMemberTags.size();
            ++eligibleRaidSeasonCount;
        }

        if (eligibleRaidSeasonCount == 0) {
            return Optional.empty();
        }

        return Optional.of(new RaidAverages(
                averageAttackCountSum / eligibleRaidSeasonCount,
                averageBonusAttackLimitSum / eligibleRaidSeasonCount,
                averageResourcesLootedSum / eligibleRaidSeasonCount));
    }

    public RaidAverages calculateRaidAverages(@NonNull RaidSeason raidSeason) {
        return calculateRaidAverages(List.of(raidSeason))
                .orElseThrow(() -> new IllegalStateException(
                        "Raid averages are missing: raidSeasonId=%s".formatted(raidSeason.getId())));
    }

    private boolean isRaidParticipantIncluded(
            RaidSeasonClanMemberSnapshot memberSnapshot,
            RaidSeasonParticipant participant) {
        return hasRaidAttacks(participant)
                || (memberSnapshot != null
                && !Boolean.FALSE.equals(memberSnapshot.getParticipationEligible()));
    }

    private boolean hasRaidAttacks(RaidSeasonParticipant participant) {
        return participant != null
                && participant.getAttackCount() != null
                && participant.getAttackCount() > 0;
    }

}
