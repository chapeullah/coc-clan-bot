package com.chapeullah.cocclanbot.rating;

import java.util.Optional;

public record MemberStatistic(
        String tag,
        String name,

        Optional<Double> totalRating,
        Optional<Double> warRating,
        Optional<Double> raidRating,

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
        Optional<Double> warAttackUsagePercentage,

        int raidSeasonCount,
        long totalRaidLoot,
        int missedRaidSeasonCount,
        int raidAttackCount,
        int missedRaidAttackCount,
        Optional<Double> averageRaidAttacksPerSeason,
        Optional<Double> averageRaidLootPerSeason,
        Optional<Double> averageRaidLootPerAttack,
        Optional<Double> averageBonusAttackLimitPerSeason,
        Optional<Double> raidAttackUsagePercentage) {}
