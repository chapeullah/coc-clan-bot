package com.chapeullah.cocclanbot.rating;

import java.util.Optional;

public record MemberWarStatistic(
        String tag,
        String name,

        Optional<Double> warRating,

        int warCount,
        int totalWarStars,
        int warAttackCount,
        int missedWarAttackCount,
        int threeStarWarAttackCount,
        Optional<Double> averageStarsPerAttack,
        Optional<Double> averageDestructionPercentagePerAttack,
        Optional<Double> averageAttackDurationSecondsPerAttack,
        Optional<Double> averageDefenderTownHallLevelPerAttack,
        Optional<Double> averageTownHallLevelDifferencePerAttack,
        Optional<Double> averageMapPositionDifferencePerAttack,
        Optional<Double> threeStarWarAttackPercentage,
        Optional<Double> warAttackUsagePercentage) {
    public static MemberWarStatistic empty(String tag, String name) {
        return new MemberWarStatistic(
                tag,
                name,
                Optional.empty(),
                0,
                0,
                0,
                0,
                0,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }
}
