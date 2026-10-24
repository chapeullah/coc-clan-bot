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
    private static final int WAR_LIMIT = 16;

    private final RaidSeasonService raidSeasonService;
    private final WarService warService;
    private final RaidSeasonClanMemberSnapshotService raidSeasonClanMemberSnapshotService;
    private final RaidSeasonParticipantService raidSeasonParticipantService;
    private final MemberService memberService;

    @Transactional(readOnly = true)
    public Map<String, MemberStatistic> calculateClanMemberStatistics(@NonNull String clanTag) {
        List<Member> currentMembers =
                memberService.getCurrentMembersByClanTag(clanTag);

        Map<RaidSeason, RaidAverages> raidAveragesBySeason = calculateRecentRaidAverages(clanTag);

        Map<String, MemberStatistic> statisticsByTag = new HashMap<>();

        for (var member : currentMembers) {
            MemberWarStatistic warStatistic =
                    calculateMemberWarStatistic(member);

            MemberRaidStatistic raidStatistic =
                    calculateMemberRaidStatistic(raidAveragesBySeason, member);

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
    public Map<String, MemberWarStatistic> calculateClanMemberWarStatistics(@NonNull String clanTag) {
        List<Member> currentMembers = memberService.getCurrentMembersByClanTag(clanTag);
        Map<String, MemberWarStatistic> statisticsByTag = new HashMap<>();
        for (var member : currentMembers) {
            statisticsByTag.put(member.getTag(), calculateMemberWarStatistic(member));
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

    @Transactional(readOnly = true)
    public MemberWarStatistic calculateMemberWarStatistic(@NonNull Member member) {
        List<War> wars = getRecentWarsReadyForRating(member);
        if (wars.isEmpty()) {
            return MemberWarStatistic.empty(member.getTag(), member.getName());
        }

        WarAverages warAverages = calculateWarAverages(wars)
                .orElseThrow(() -> new IllegalStateException(
                        "War averages are missing: warCount=%d".formatted(wars.size())));

        int attackCount = 0;
        int starsSum = 0;
        double destructionPercentageSum = 0.0;
        int durationSecondsSum = 0;
        double attackScoreSum = 0.0;
        int missedAttackCount = 0;
        int threeStarAttackCount = 0;
        double defenderTownHallLevelSum = 0.0;
        int defenderTownHallLevelCount = 0;
        double townHallLevelDifferenceSum = 0.0;
        int townHallLevelDifferenceCount = 0;
        double mapPositionDifferenceSum = 0.0;
        int mapPositionDifferenceCount = 0;
        for (var war : wars) {
            List<WarAttack> attacks = war.getAttacks().stream()
                    .filter(attack ->
                            Objects.equals(attack.getAttackerTag(), member.getTag()))
                    .toList();

            Integer attacksPerMember = war.getAttacksPerMember();
            if (attacksPerMember == null || attacksPerMember < attacks.size()) {
                throw new IllegalStateException(
                        "Invalid war attack limit: warId=%s, attacksPerMember=%s, attackCount=%d"
                                .formatted(war.getId(), attacksPerMember, attacks.size()));
            }
            missedAttackCount += attacksPerMember - attacks.size();

            for (var attack : attacks) {
                Integer stars = attack.getStars();
                if (stars == null) {
                    throw new IllegalStateException(
                            "War attack stars are missing: warId=%s, attackerTag=\"%s\", attackOrder=%s"
                                    .formatted(
                                            war.getId(),
                                            attack.getAttackerTag(),
                                            attack.getAttackOrder()));
                }

                Double destructionPercentage = attack.getDestructionPercentage();
                if (destructionPercentage == null) {
                    throw new IllegalStateException(
                            "War attack destruction percentage is missing: warId=%s, attackerTag=\"%s\", attackOrder=%s"
                                    .formatted(
                                            war.getId(),
                                            attack.getAttackerTag(),
                                            attack.getAttackOrder()));
                }

                Integer durationSeconds = attack.getDurationSeconds();
                if (durationSeconds == null) {
                    throw new IllegalStateException(
                            "War attack duration is missing: warId=%s, attackerTag=\"%s\", attackOrder=%s"
                                    .formatted(
                                            war.getId(),
                                            attack.getAttackerTag(),
                                            attack.getAttackOrder()));
                }

                starsSum += stars;
                destructionPercentageSum += destructionPercentage;
                durationSecondsSum += durationSeconds;
                attackScoreSum += calculateWarAttackScore(attack, stars, destructionPercentage);
                ++attackCount;

                Integer defenderTownHallLevel = attack.getDefenderTownHallLevel();
                Integer attackerTownHallLevel = attack.getAttackerTownHallLevel();
                if (defenderTownHallLevel != null && defenderTownHallLevel > 0) {
                    defenderTownHallLevelSum += defenderTownHallLevel;
                    ++defenderTownHallLevelCount;
                    if (attackerTownHallLevel != null && attackerTownHallLevel > 0) {
                        townHallLevelDifferenceSum +=
                                (double) defenderTownHallLevel - attackerTownHallLevel;
                        ++townHallLevelDifferenceCount;
                    }
                }

                WarParticipant attacker = attack.getAttacker();
                WarParticipant defender = attack.getDefender();
                Integer attackerMapPosition = attacker == null ? null : attacker.getMapPosition();
                Integer defenderMapPosition = defender == null ? null : defender.getMapPosition();
                if (attackerMapPosition != null && attackerMapPosition > 0
                        && defenderMapPosition != null && defenderMapPosition > 0) {
                    mapPositionDifferenceSum += (double) attackerMapPosition - defenderMapPosition;
                    ++mapPositionDifferenceCount;
                }

                if (stars == 3) {
                    ++threeStarAttackCount;
                }
            }
        }

        WarAverages memberAverages = new WarAverages(
                (double) starsSum / wars.size(),
                destructionPercentageSum / wars.size(),
                attackCount == 0
                        ? Optional.empty()
                        : Optional.of((double) durationSecondsSum / attackCount),
                attackScoreSum / wars.size());

        Optional<Double> warRating = Optional.of(
                warAverages.attackScore() > 0.0
                        ? memberAverages.attackScore() / warAverages.attackScore()
                        : 0.0);

        int availableAttackCount = attackCount + missedAttackCount;

        return new MemberWarStatistic(
                member.getTag(),
                member.getName(),
                warRating,
                wars.size(),
                starsSum,
                attackCount,
                missedAttackCount,
                threeStarAttackCount,
                attackCount == 0
                        ? Optional.empty()
                        : Optional.of((double) starsSum / attackCount),
                attackCount == 0
                        ? Optional.empty()
                        : Optional.of(destructionPercentageSum / attackCount),
                memberAverages.durationSeconds(),
                defenderTownHallLevelCount == 0
                        ? Optional.empty()
                        : Optional.of(defenderTownHallLevelSum / defenderTownHallLevelCount),
                townHallLevelDifferenceCount == 0
                        ? Optional.empty()
                        : Optional.of(townHallLevelDifferenceSum / townHallLevelDifferenceCount),
                mapPositionDifferenceCount == 0
                        ? Optional.empty()
                        : Optional.of(mapPositionDifferenceSum / mapPositionDifferenceCount),
                attackCount == 0
                        ? Optional.empty()
                        : Optional.of(100.0 * threeStarAttackCount / attackCount),
                availableAttackCount == 0
                        ? Optional.empty()
                        : Optional.of(100.0 * attackCount / availableAttackCount));
    }

    public Optional<WarAverages> calculateWarAverages(@NonNull List<War> wars) {
        if (wars.isEmpty()) {
            return Optional.empty();
        }

        int attackCount = 0;
        double averageStarsSum = 0.0;
        double averageDestructionPercentageSum = 0.0;
        double averageAttackScoreSum = 0.0;
        int durationSecondsSum = 0;
        for (var war : wars) {
            String clanTag = war.getClanTag();

            List<WarParticipant> participants = war.getParticipants()
                    .stream()
                    .filter(participant ->
                            Objects.equals(participant.getClanTag(), clanTag))
                    .toList();
            if (participants.isEmpty()) {
                throw new IllegalStateException(
                        "War participants are missing: warId=%s, clanTag=\"%s\""
                                .formatted(war.getId(), clanTag));
            }

            int starsSum = 0;
            double destructionPercentageSum = 0.0;
            double attackScoreSum = 0.0;

            Map<String, List<WarAttack>> attacksByTag = war.getAttacks()
                    .stream()
                    .collect(Collectors.groupingBy(WarAttack::getAttackerTag));

            for (var participant : participants) {
                for (var attack : attacksByTag.getOrDefault(
                        participant.getPlayerTag(), List.of())) {

                    Integer stars = attack.getStars();
                    if (stars == null) {
                        throw new IllegalStateException(
                                "War attack stars are missing: warId=%s, attackerTag=\"%s\", attackOrder=%s"
                                        .formatted(
                                                war.getId(),
                                                attack.getAttackerTag(),
                                                attack.getAttackOrder()));
                    }
                    starsSum += stars;

                    Double destructionPercentage = attack.getDestructionPercentage();
                    if (destructionPercentage == null) {
                        throw new IllegalStateException(
                                "War attack destruction percentage is missing: warId=%s, attackerTag=\"%s\", attackOrder=%s"
                                        .formatted(
                                                war.getId(),
                                                attack.getAttackerTag(),
                                                attack.getAttackOrder()));
                    }
                    destructionPercentageSum += destructionPercentage;
                    attackScoreSum += calculateWarAttackScore(attack, stars, destructionPercentage);

                    Integer durationSeconds = attack.getDurationSeconds();
                    if (durationSeconds == null) {
                        throw new IllegalStateException(
                                "War attack duration is missing: warId=%s, attackerTag=\"%s\", attackOrder=%s"
                                        .formatted(
                                                war.getId(),
                                                attack.getAttackerTag(),
                                                attack.getAttackOrder()));
                    }
                    durationSecondsSum += durationSeconds;
                    ++attackCount;
                }
            }

            averageStarsSum += (double) starsSum / participants.size();
            averageDestructionPercentageSum += destructionPercentageSum / participants.size();
            averageAttackScoreSum += attackScoreSum / participants.size();
        }

        Optional<Double> averageDurationSeconds = attackCount == 0
                ? Optional.empty()
                : Optional.of((double) durationSecondsSum / attackCount);

        return Optional.of(new WarAverages(
                averageStarsSum / wars.size(),
                averageDestructionPercentageSum / wars.size(),
                averageDurationSeconds,
                averageAttackScoreSum / wars.size()));
    }

    private double calculateWarAttackScore(WarAttack attack, int stars, double destructionPercentage) {
        if (stars < 0 || stars > 3
                || !Double.isFinite(destructionPercentage)
                || destructionPercentage < 0.0 || destructionPercentage > 100.0) {
            throw new IllegalStateException(
                    "Invalid war attack result: attackerTag=\"%s\", attackOrder=%s, stars=%s, destructionPercentage=%s"
                            .formatted(attack.getAttackerTag(), attack.getAttackOrder(), stars, destructionPercentage));
        }

        double coefficient = 1.0;
        Integer attackerTownHallLevel = attack.getAttackerTownHallLevel();
        Integer defenderTownHallLevel = attack.getDefenderTownHallLevel();
        if (attackerTownHallLevel != null && attackerTownHallLevel > 0
                && defenderTownHallLevel != null && defenderTownHallLevel > 0) {
            int difference = defenderTownHallLevel - attackerTownHallLevel;
            int levels = Math.min(Math.abs(difference), 2);
            if (difference > 0 && stars > 0) {
                coefficient += (stars == 1 ? 0.05 : 0.10) * levels;
            } else if (difference < 0) {
                coefficient -= (stars == 3 ? 0.10 : 0.20) * levels;
            }
        }

        return (stars + destructionPercentage / 100.0) / 4.0 * coefficient;
    }

    public WarAverages calculateWarAverages(@NonNull War war) {
        return calculateWarAverages(List.of(war))
                .orElseThrow(() -> new IllegalStateException(
                        "War averages are missing: warId=%s".formatted(war.getId())));
    }

    private List<War> getRecentWarsReadyForRating(Member member) {
        List<War> wars = new ArrayList<>(WAR_LIMIT);
        int pageNumber = 0;
        while (true) {
            Slice<War> page = warService.getCompletedByClanTagAndMemberTag(
                    member.getClan().getTag(), member.getTag(), pageNumber, WAR_LIMIT);
            for (War war : page) {
                if (isWarReadyForRating(war)) {
                    wars.add(war);
                    if (wars.size() == WAR_LIMIT) {
                        return wars;
                    }
                }
            }
            if (!page.hasNext()) {
                return wars;
            }
            ++pageNumber;
        }
    }

    public boolean isWarReadyForRating(@NonNull War war) {
        if (!"warEnded".equals(war.getState()) || war.getWarLogSyncedAt() == null) {
            return false;
        }

        String clanTag = war.getClanTag();
        long participantsCount = war.getParticipants().stream()
                .filter(participant -> Objects.equals(participant.getClanTag(), clanTag))
                .count();
        long attacksCount = war.getAttacks().stream()
                .filter(attack -> Objects.equals(attack.getAttacker().getClanTag(), clanTag))
                .count();

        return participantsCount == war.getTeamSize()
                && attacksCount == war.getClanAttacks();
    }

}
