package com.chapeullah.cocclanbot.raidseason.dto;

public record RaidSeasonAttackResponse(
        RaidSeasonAttackerResponse attacker,
        Integer stars,
        Integer destructionPercent) {}
