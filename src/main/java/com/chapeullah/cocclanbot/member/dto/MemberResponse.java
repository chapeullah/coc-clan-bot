package com.chapeullah.cocclanbot.member.dto;

import com.chapeullah.cocclanbot.builderbaseleague.dto.BuilderBaseLeagueResponse;
import com.chapeullah.cocclanbot.clan.dto.ClanLeagueResponse;
import com.chapeullah.cocclanbot.leaguetier.dto.LeagueTierResponse;
import com.chapeullah.cocclanbot.player.dto.PlayerHouseResponse;

public record MemberResponse(
        String tag,
        String name,
        String role,
        Integer townHallLevel,
        Integer expLevel,
        ClanLeagueResponse league,
        LeagueTierResponse leagueTier,
        Integer trophies,
        Integer builderBaseTrophies,
        Integer clanRank,
        Integer previousClanRank,
        Integer donations,
        Integer donationsReceived,
        PlayerHouseResponse playerHouse,
        BuilderBaseLeagueResponse builderBaseLeague) {}
