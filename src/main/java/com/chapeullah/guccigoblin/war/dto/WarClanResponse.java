package com.chapeullah.guccigoblin.war.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WarClanResponse(
        String tag,
        String name,
        WarClanBadgeUrlsResponse badgeUrls,
        Integer clanLevel,
        Integer attacks,
        Integer stars,
        Double destructionPercentage,
        List<WarMemberResponse> members) {}
