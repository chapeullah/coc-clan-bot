package com.chapeullah.guccigoblin.clan.dto;

import com.chapeullah.guccigoblin.builderbaseleague.dto.BuilderBaseLeagueResponse;
import com.chapeullah.guccigoblin.leaguetier.dto.LeagueTierResponse;
import com.chapeullah.guccigoblin.member.dto.MemberHouseElementResponses;

public record ClanMemberResponse(
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
        MemberHouseElementResponses playerHouse,
        BuilderBaseLeagueResponse builderBaseLeague) {
}
