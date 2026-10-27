package com.chapeullah.cocclanbot.rating;

import com.chapeullah.cocclanbot.member.MemberService;
import com.chapeullah.cocclanbot.member.model.Member;
import com.chapeullah.cocclanbot.war.model.War;
import com.chapeullah.cocclanbot.war.model.WarAttack;
import com.chapeullah.cocclanbot.war.model.WarParticipant;
import com.chapeullah.cocclanbot.war.service.WarService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@DependsOn("environmentVariablesService")
public class WarRatingService {

    private static final int WAR_LIMIT = 16;

    private final WarService warService;
    private final MemberService memberService;

    @Transactional(readOnly = true)
    public List<MemberWarStatistic> getMemberWarStatistics(@NonNull String clanTag) {
            return calculateMemberWarStatistic(clanTag)
                    .values().stream()
                    .sorted(Comparator
                            .comparingDouble((MemberWarStatistic statistic) ->
                                    statistic.warRating().orElse(Double.NEGATIVE_INFINITY))
                            .reversed()
                            .thenComparing(MemberWarStatistic::name, String.CASE_INSENSITIVE_ORDER)
                            .thenComparing(MemberWarStatistic::tag))
                    .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, MemberWarStatistic> calculateMemberWarStatistic(@NonNull String clanTag) {
        List<Member> currentMembers = memberService.getCurrentMembersByClanTag(clanTag);
        Map<String, MemberWarStatistic> statisticsByTag = new HashMap<>();
        for (var member : currentMembers) {
            statisticsByTag.put(member.getTag(), calculateMemberWarStatistic(member));
        }
        return statisticsByTag;
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
