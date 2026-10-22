package com.chapeullah.cocclanbot.clan.dto;

import com.chapeullah.cocclanbot.builderbaseleague.dto.BuilderBaseLeagueResponse;
import com.chapeullah.cocclanbot.leaguetier.dto.LeagueTierResponse;
import com.chapeullah.cocclanbot.member.dto.MemberHouseElementResponses;

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
