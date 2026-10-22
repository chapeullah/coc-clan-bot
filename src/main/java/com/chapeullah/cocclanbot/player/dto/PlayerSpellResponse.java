package com.chapeullah.cocclanbot.player.dto;

public record PlayerSpellResponse(
        String name,
        Integer level,
        Integer maxLevel,
        String village) {}
