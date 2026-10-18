package com.chapeullah.guccigoblin.war.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record WarMemberResponse(
        String tag,
        String name,
        @JsonProperty("townhallLevel")
        Integer townHallLevel,
        Integer mapPosition,
        List<WarAttackResponse> attacks,
        Integer opponentAttacks,
        WarAttackResponse bestOpponentAttack) {}
