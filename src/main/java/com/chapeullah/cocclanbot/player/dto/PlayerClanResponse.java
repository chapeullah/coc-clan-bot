package com.chapeullah.cocclanbot.player.dto;

import com.chapeullah.cocclanbot.clan.dto.ClanBadgeUrlsResponse;

public record PlayerClanResponse(
        String tag,
        String name,
        Integer clanLevel,
        ClanBadgeUrlsResponse badgeUrls) {}
