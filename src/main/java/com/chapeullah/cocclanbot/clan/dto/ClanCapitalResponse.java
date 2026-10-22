package com.chapeullah.cocclanbot.clan.dto;

import java.util.List;

public record ClanCapitalResponse(
        Integer capitalHallLevel,
        Long clanGoldSinkTotal,
        List<ClanCapitalDistrictResponse> districts) {}
