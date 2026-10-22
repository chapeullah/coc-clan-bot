package com.chapeullah.cocclanbot.player.dto;

public record PlayerTroopResponse(
        String name,
        Integer level,
        Integer maxLevel,
        String village) {}
