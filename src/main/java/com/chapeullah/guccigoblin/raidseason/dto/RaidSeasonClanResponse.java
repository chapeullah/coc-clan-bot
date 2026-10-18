package com.chapeullah.guccigoblin.raidseason.dto;

public record RaidSeasonClanResponse(
        String tag,
        String name,
        Integer level,
        RaidSeasonBadgeUrlsResponse badgeUrls) {}
