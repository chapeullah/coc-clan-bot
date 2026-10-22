package com.chapeullah.cocclanbot.rating;

import java.util.Optional;

public record MemberRaidStatistic(
        String tag,
        String name,

        Optional<Double> raidRating,

        int raidSeasonCount,
        long totalRaidLoot,
        int missedRaidSeasonCount,
        int raidAttackCount,
        int missedRaidAttackCount,
        Optional<Double> averageRaidAttacksPerSeason,
        Optional<Double> averageRaidLootPerSeason,
        Optional<Double> averageRaidLootPerAttack,
        Optional<Double> averageBonusAttackLimitPerSeason,
        Optional<Double> raidAttackUsagePercentage) {
    public static MemberRaidStatistic empty(String tag, String name) {
        return new MemberRaidStatistic(
                tag,
                name,
                Optional.empty(),
                0,
                0L,
                0,
                0,
                0,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }
}
