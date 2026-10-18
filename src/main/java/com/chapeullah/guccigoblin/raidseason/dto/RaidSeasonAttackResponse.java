package com.chapeullah.guccigoblin.raidseason.dto;

public record RaidSeasonAttackResponse(
        RaidSeasonAttackerResponse attacker,
        Integer stars,
        Integer destructionPercent) {}
