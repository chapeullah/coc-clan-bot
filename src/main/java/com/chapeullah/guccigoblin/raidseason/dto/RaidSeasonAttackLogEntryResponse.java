package com.chapeullah.guccigoblin.raidseason.dto;

import java.util.List;

public record RaidSeasonAttackLogEntryResponse(
        RaidSeasonClanResponse defender,
        Integer attackCount,
        Integer districtCount,
        Integer districtsDestroyed,
        List<RaidSeasonDistrictResponse> districts) {}
