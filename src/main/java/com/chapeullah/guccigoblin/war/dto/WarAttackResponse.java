package com.chapeullah.guccigoblin.war.dto;

public record WarAttackResponse(
        String attackerTag,
        String defenderTag,
        Integer stars,
        Double destructionPercentage,
        Integer order,
        Integer duration) {}