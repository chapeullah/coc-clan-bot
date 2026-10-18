package com.chapeullah.guccigoblin.member.dto;

import com.chapeullah.guccigoblin.builderbaseleague.dto.BuilderBaseLeagueResponse;
import com.chapeullah.guccigoblin.clan.dto.ClanLeagueResponse;
import com.chapeullah.guccigoblin.leaguetier.dto.LeagueTierResponse;
import com.chapeullah.guccigoblin.player.dto.PlayerHouseResponse;

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
