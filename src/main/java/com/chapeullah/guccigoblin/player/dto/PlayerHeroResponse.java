package com.chapeullah.guccigoblin.player.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record PlayerHeroResponse(
        String name,
        Integer level,
        Integer maxLevel,
        @JsonProperty("equipment")
        List<PlayerHeroEquipmentResponse> heroEquipmentsResponse,
        String village) {}
