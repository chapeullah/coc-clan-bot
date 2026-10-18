package com.chapeullah.guccigoblin.player.dto;

public record PlayerAchievementResponse(
        String name,
        Integer stars,
        Integer value,
        Integer target,
        String info,
        String completionInfo,
        String village) {}
