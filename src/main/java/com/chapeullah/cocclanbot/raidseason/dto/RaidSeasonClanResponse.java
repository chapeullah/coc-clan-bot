package com.chapeullah.cocclanbot.raidseason.dto;

public record RaidSeasonClanResponse(
        String tag,
        String name,
        Integer level,
        RaidSeasonBadgeUrlsResponse badgeUrls) {}
