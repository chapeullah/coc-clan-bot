package com.chapeullah.guccigoblin.clan.dto;

import java.util.List;

public record ClanCapitalResponse(
        Integer capitalHallLevel,
        Long clanGoldSinkTotal,
        List<ClanCapitalDistrictResponse> districts) {}
