package com.chapeullah.cocclanbot.player.dto;

import com.chapeullah.cocclanbot.builderbaseleague.dto.BuilderBaseLeagueResponse;
import com.chapeullah.cocclanbot.label.dto.LabelResponse;
import com.chapeullah.cocclanbot.leaguetier.dto.LeagueTierResponse;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record PlayerResponse(
        String tag,
        String name,
        Integer townHallLevel,
        Integer townHallWeaponLevel,
        Integer expLevel,
        Integer trophies,
        Integer bestTrophies,
        Integer warStars,
        Integer attackWins,
        Integer defenseWins,
        Integer builderHallLevel,
        Integer builderBaseTrophies,
        Integer bestBuilderBaseTrophies,
        String role,
        String warPreference,
        Integer donations,
        Integer donationsReceived,
        Integer clanCapitalContributions,
        PlayerClanResponse clan,
        LeagueTierResponse leagueTier,
        BuilderBaseLeagueResponse builderBaseLeague,
        String currentLeagueGroupTag,
        Integer currentLeagueSeasonId,
        String previousLeagueGroupTag,
        Integer previousLeagueSeasonId,
        List<PlayerAchievementResponse> achievements,
        PlayerHouseResponse playerHouse,
        List<LabelResponse> labels,
        List<PlayerTroopResponse> troops,
        List<PlayerHeroResponse> heroes,
        @JsonProperty("heroEquipment")
        List<PlayerHeroEquipmentResponse> heroEquipments,
        List<PlayerSpellResponse> spells) {}
