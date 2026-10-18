package com.chapeullah.guccigoblin.raidseason.dto;

public record RaidSeasonMemberResponse(
        String tag,
        String name,
        Integer attacks,
        Integer attackLimit,
        Integer bonusAttackLimit,
        Integer capitalResourcesLooted) {}
