package com.chapeullah.cocclanbot.raidseason.dto;

import java.util.List;

public record RaidSeasonDefenseLogEntryResponse(
        RaidSeasonClanResponse attacker,
        Integer attackCount,
        Integer districtCount,
        Integer districtsDestroyed,
        List<RaidSeasonDistrictResponse> districts) {}
