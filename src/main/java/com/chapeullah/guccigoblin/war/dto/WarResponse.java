package com.chapeullah.guccigoblin.war.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WarResponse(
        String state,
        Integer teamSize,
        Integer attacksPerMember,
        String battleModifier,
        String preparationStartTime,
        String startTime,
        String endTime,
        WarClanResponse clan,
        WarClanResponse opponent) {}