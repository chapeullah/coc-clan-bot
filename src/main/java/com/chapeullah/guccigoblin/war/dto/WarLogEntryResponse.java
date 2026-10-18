package com.chapeullah.guccigoblin.war.dto;

public record WarLogEntryResponse(
        String result,
        String endTime,
        Integer teamSize,
        Integer attacksPerMember,
        String battleModifier,
        WarLogClanResponse clan,
        WarLogClanResponse opponent) {}