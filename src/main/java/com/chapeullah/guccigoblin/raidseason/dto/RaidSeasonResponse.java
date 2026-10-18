package com.chapeullah.guccigoblin.raidseason.dto;

import java.util.List;

public record RaidSeasonResponse(
        String state,
        String startTime,
        String endTime,
        Integer capitalTotalLoot,
        Integer raidsCompleted,
        Integer totalAttacks,
        Integer enemyDistrictsDestroyed,
        Integer offensiveReward,
        Integer defensiveReward,
        List<RaidSeasonMemberResponse> members,
        List<RaidSeasonAttackLogEntryResponse> attackLog,
        List<RaidSeasonDefenseLogEntryResponse> defenseLog) {}
