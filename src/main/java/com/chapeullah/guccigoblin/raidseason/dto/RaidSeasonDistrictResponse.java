package com.chapeullah.guccigoblin.raidseason.dto;

import java.util.List;

public record RaidSeasonDistrictResponse(
        Integer id,
        String name,
        Integer districtHallLevel,
        Integer destructionPercent,
        Integer stars,
        Integer attackCount,
        Integer totalLooted,
        List<RaidSeasonAttackResponse> attacks) {}
