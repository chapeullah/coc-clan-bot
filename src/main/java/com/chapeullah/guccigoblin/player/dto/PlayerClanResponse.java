package com.chapeullah.guccigoblin.player.dto;

import com.chapeullah.guccigoblin.clan.dto.ClanBadgeUrlsResponse;

public record PlayerClanResponse(
        String tag,
        String name,
        Integer clanLevel,
        ClanBadgeUrlsResponse badgeUrls) {}
