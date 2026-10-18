package com.chapeullah.guccigoblin.clan.dto;

import com.chapeullah.guccigoblin.capitalleague.dto.CapitalLeagueResponse;
import com.chapeullah.guccigoblin.label.dto.LabelResponse;
import com.chapeullah.guccigoblin.leaguetier.dto.LeagueTierResponse;
import com.chapeullah.guccigoblin.location.dto.LocationResponse;
import com.chapeullah.guccigoblin.warleague.dto.WarLeagueResponse;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ClanResponse(
        String tag,
        String name,
        String type,
        String description,
        LocationResponse location,
        Boolean isFamilyFriendly,
        ClanBadgeUrlsResponse badgeUrls,
        Integer clanLevel,
        Integer clanPoints,
        Integer clanBuilderBasePoints,
        Integer clanCapitalPoints,
        CapitalLeagueResponse capitalLeague,
        Integer requiredTrophies,
        String warFrequency,
        Integer warWinStreak,
        Integer warWins,
        Integer warTies,
        Integer warLosses,
        Boolean isWarLogPublic,
        WarLeagueResponse warLeague,
        @JsonProperty("members") Integer memberCount,
        List<ClanMemberResponse> memberList,
        List<LabelResponse> labels,
        Integer requiredBuilderBaseTrophies,
        @JsonProperty("requiredTownhallLevel") Integer requiredTownHallLevel,
        LeagueTierResponse requiredLeagueTier,
        ClanCapitalResponse clanCapital,
        ClanChatLanguageResponse chatLanguage) {}