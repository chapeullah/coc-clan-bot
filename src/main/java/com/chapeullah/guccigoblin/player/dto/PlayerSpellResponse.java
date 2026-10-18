package com.chapeullah.guccigoblin.player.dto;

public record PlayerSpellResponse(
        String name,
        Integer level,
        Integer maxLevel,
        String village) {}
