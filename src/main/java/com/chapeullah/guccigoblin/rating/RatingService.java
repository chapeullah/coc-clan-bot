package com.chapeullah.guccigoblin.rating;

import com.chapeullah.guccigoblin.member.MemberService;
import com.chapeullah.guccigoblin.member.model.Member;
import com.chapeullah.guccigoblin.raidseason.model.RaidSeason;
import com.chapeullah.guccigoblin.raidseason.model.RaidSeasonClanMemberSnapshot;
import com.chapeullah.guccigoblin.raidseason.model.RaidSeasonParticipant;
import com.chapeullah.guccigoblin.raidseason.service.RaidSeasonClanMemberSnapshotService;
import com.chapeullah.guccigoblin.raidseason.service.RaidSeasonParticipantService;
import com.chapeullah.guccigoblin.raidseason.service.RaidSeasonService;
import com.chapeullah.guccigoblin.war.model.War;
import com.chapeullah.guccigoblin.war.model.WarAttack;
import com.chapeullah.guccigoblin.war.model.WarParticipant;
import com.chapeullah.guccigoblin.war.service.WarService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RatingService {

    private static final int RAID_SEASON_LIMIT = 4;
    private static final int WAR_LIMIT = 16;

    private final RaidSeasonService raidSeasonService;
    private final WarService warService;
    private final RaidSeasonClanMemberSnapshotService raidSeasonClanMemberSnapshotService;
    private final RaidSeasonParticipantService raidSeasonParticipantService;
    private final MemberService memberService;

    @Transactional(readOnly = true)
    public Map<String, Double> calculateClanMemberRatings(@NonNull String clanTag) {
        List<Member> currentMembers = memberService
                .getCurrentMembersByClanTag(clanTag);

        List<RaidSeason> recentCompletedRaidSeasons = raidSeasonService
                .getRecentCompletedByClanTag(clanTag, RAID_SEASON_LIMIT);

        Optional<RaidAverages> raidAverages =
                calculateRaidAverages(recentCompletedRaidSeasons);

        Map<String, Double> ratingsByTag = new HashMap<>();

        for (var member : currentMembers) {
            Optional<Double> raidRating = raidAverages
                    .flatMap(averages ->
                            calculateMemberRaidRating(averages, member));

            Optional<Double> warRating = calculateMemberWarRating(member);

            double ratingSum = 0.0;
            int ratingCount = 0;

            if (raidRating.isPresent()) {
                ratingSum += raidRating.get();
                ++ratingCount;
            }

            if (warRating.isPresent()) {
                ratingSum += warRating.get();
                ++ratingCount;
            }

            if (ratingCount > 0) {
                ratingsByTag.put(
                        member.getTag(),
                        ratingSum / ratingCount);
            }
        }

        return ratingsByTag;
    }

    @Transactional(readOnly = true)
    public Optional<Double> calculateMemberRaidRating(
            @NonNull RaidAverages raidAverages,
            @NonNull Member member) {

        List<RaidSeason> raidSeasons = raidSeasonService
                .getRecentCompletedByClanTag(
                        member.getClan().getTag(), RAID_SEASON_LIMIT);

        int eligibleRaidSeasonCount = 0;
        int attackCountSum = 0;
        int bonusAttackLimitSum = 0;
        int resourcesLootedSum = 0;

        for (var raidSeason : raidSeasons) {
            boolean wasInClan = raidSeason.getMemberSnapshots().stream()
                    .anyMatch(snapshot ->
                            Objects.equals(snapshot.getTag(), member.getTag()));
            if (!wasInClan) {
                continue;
            }

            ++eligibleRaidSeasonCount;

            RaidSeasonParticipant participant = raidSeason.getParticipants().stream()
                    .filter(p -> Objects.equals(p.getTag(), member.getTag()))
                    .findFirst()
                    .orElse(null);
            if (participant == null) {
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

            attackCountSum += attackCount;
            bonusAttackLimitSum += bonusAttackLimit;
            resourcesLootedSum += resourcesLooted;
        }

        if (eligibleRaidSeasonCount == 0) {
            return Optional.empty();
        }

        RaidAverages memberAverages = new RaidAverages(
                (double) attackCountSum / eligibleRaidSeasonCount,
                (double) bonusAttackLimitSum / eligibleRaidSeasonCount,
                (double) resourcesLootedSum / eligibleRaidSeasonCount);

        double ratingSum = 0.0;
        int metricCount = 0;

        if (raidAverages.attackCount() > 0.0) {
            ratingSum += memberAverages.attackCount() / raidAverages.attackCount();
            ++metricCount;
        }

        if (raidAverages.bonusAttackLimit() > 0.0) {
            ratingSum += memberAverages.bonusAttackLimit() / raidAverages.bonusAttackLimit();
            ++metricCount;
        }

        if (raidAverages.resourcesLooted() > 0.0) {
            ratingSum += memberAverages.resourcesLooted() / raidAverages.resourcesLooted();
            ++metricCount;
        }

        if (metricCount == 0) {
            return Optional.empty();
        }

        return Optional.of(ratingSum / metricCount);
    }

    public Optional<RaidAverages> calculateRaidAverages(@NonNull List<RaidSeason> raidSeasons) {
        if (raidSeasons.isEmpty()) {
            return Optional.empty();
        }

        int attackCountSum = 0;
        int bonusAttackLimitSum = 0;
        int resourceLootedSum = 0;
        int memberSnapshotCount = 0;
        for (var raidSeason : raidSeasons) {
            List<RaidSeasonClanMemberSnapshot> clanMemberSnapshot =
                    raidSeasonClanMemberSnapshotService.getByRaidSeason(raidSeason);

            if (clanMemberSnapshot.isEmpty()) {
                throw new IllegalStateException(
                        "Clan member snapshot is empty: raidSeasonId=%s, clanTag=\"%s\""
                                .formatted(raidSeason.getId(), raidSeason.getClan().getTag()));
            }

            memberSnapshotCount += clanMemberSnapshot.size();

            Map<String, RaidSeasonParticipant> participantsByTag =
                    raidSeasonParticipantService.getByRaidSeason(raidSeason)
                            .stream()
                            .collect(Collectors.toMap(
                                    RaidSeasonParticipant::getTag,
                                    Function.identity()));

            for (var memberSnapshot : clanMemberSnapshot) {
                var participant = participantsByTag.get(memberSnapshot.getTag());
                if (participant == null) {
                    continue;
                }

                Integer attackCount = participant.getAttackCount();
                if (attackCount == null) {
                    throw new IllegalStateException(
                            "Raid season participant attack count is missing: raidSeasonId=%s, tag=\"%s\""
                                    .formatted(raidSeason.getId(), memberSnapshot.getTag()));
                }
                attackCountSum += attackCount;

                Integer bonusAttackLimit = participant.getBonusAttackLimit();
                if (bonusAttackLimit == null) {
                    throw new IllegalStateException(
                            "Raid season participant bonus attack limit is missing: raidSeasonId=%s, tag=\"%s\""
                                    .formatted(raidSeason.getId(), memberSnapshot.getTag()));
                }
                bonusAttackLimitSum += bonusAttackLimit;

                Integer capitalResourcesLooted = participant.getCapitalResourcesLooted();
                if (capitalResourcesLooted == null) {
                    throw new IllegalStateException(
                            "Raid season participant capital resources looted are missing: raidSeasonId=%s, tag=\"%s\""
                                    .formatted(raidSeason.getId(), memberSnapshot.getTag()));
                }
                resourceLootedSum += capitalResourcesLooted;
            }
        }

        return Optional.of(new RaidAverages(
                (double) attackCountSum / memberSnapshotCount,
                (double) bonusAttackLimitSum / memberSnapshotCount,
                (double) resourceLootedSum / memberSnapshotCount));
    }

    public RaidAverages calculateRaidAverages(@NonNull RaidSeason raidSeason) {
        return calculateRaidAverages(List.of(raidSeason))
                .orElseThrow(() -> new IllegalStateException(
                        "Raid averages are missing: raidSeasonId=%s".formatted(raidSeason.getId())));
    }

    @Transactional(readOnly = true)
    public Optional<Double> calculateMemberWarRating(@NonNull Member member) {
        List<War> wars = warService
                .getCompletedByClanTagAndMemberTag(
                        member.getClan().getTag(), member.getTag())
                .stream()
                .filter(this::isWarReadyForRating)
                .limit(WAR_LIMIT)
                .toList();

        if (wars.isEmpty()) {
            return Optional.empty();
        }

        WarAverages warAverages = calculateWarAverages(wars).orElseThrow();

        int attackCount = 0;
        int starsSum = 0;
        double destructionPercentageSum = 0.0;
        int durationSecondsSum = 0;

        for (var war : wars) {
            List<WarAttack> attacks = war.getAttacks().stream()
                    .filter(attack ->
                            Objects.equals(attack.getAttackerTag(), member.getTag()))
                    .toList();

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
                ++attackCount;
            }
        }

        WarAverages memberAverages = new WarAverages(
                (double) starsSum / wars.size(),
                destructionPercentageSum / wars.size(),
                attackCount == 0
                        ? Optional.empty()
                        : Optional.of((double) durationSecondsSum / attackCount));

        double ratingSum = 0.0;
        int metricCount = 0;

        if (warAverages.stars() > 0.0) {
            ratingSum += memberAverages.stars() / warAverages.stars();
            ++metricCount;
        }

        if (warAverages.destructionPercentage() > 0.0) {
            ratingSum += memberAverages.destructionPercentage()
                    / warAverages.destructionPercentage();
            ++metricCount;
        }

        double clanDuration = warAverages.durationSeconds().orElse(0.0);
        double memberDuration = memberAverages.durationSeconds().orElse(0.0);

        if (clanDuration > 0.0) {
            if (memberDuration > 0.0) {
                ratingSum += clanDuration / memberDuration;
            }
            ++metricCount;
        }

        if (metricCount == 0) {
            return Optional.empty();
        }

        return Optional.of(ratingSum / metricCount);
    }

    public Optional<WarAverages> calculateWarAverages(@NonNull List<War> wars) {
        if (wars.isEmpty()) {
            return Optional.empty();
        }

        int participantCount = 0;
        int attackCount = 0;
        int starsSum = 0;
        double destructionPercentageSum = 0.0d;
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

            participantCount += participants.size();

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
        }

        Optional<Double> averageDurationSeconds = attackCount == 0
                ? Optional.empty()
                : Optional.of((double) durationSecondsSum / attackCount);

        return Optional.of(new WarAverages(
                (double) starsSum / participantCount,
                destructionPercentageSum / participantCount,
                averageDurationSeconds));
    }

    public WarAverages calculateWarAverages(@NonNull War war) {
        return calculateWarAverages(List.of(war))
                .orElseThrow(() -> new IllegalStateException(
                        "War averages are missing: warId=%s".formatted(war.getId())));
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
