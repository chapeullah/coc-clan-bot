package com.chapeullah.guccigoblin.war.dto;

public record WarLogClanResponse(
        String tag,
        String name,
        WarLogClanBadgeUrlsResponse badgeUrls,
        Integer clanLevel,
        Integer attacks,
        Integer stars,
        Double destructionPercentage,
        Integer expEarned) {}