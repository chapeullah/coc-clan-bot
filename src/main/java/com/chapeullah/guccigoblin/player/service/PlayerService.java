package com.chapeullah.guccigoblin.player.service;

import com.chapeullah.guccigoblin.ClashOfClansClient;
import com.chapeullah.guccigoblin.builderbaseleague.BuilderBaseLeague;
import com.chapeullah.guccigoblin.builderbaseleague.BuilderBaseLeagueService;
import com.chapeullah.guccigoblin.clan.model.Clan;
import com.chapeullah.guccigoblin.clan.service.ClanService;
import com.chapeullah.guccigoblin.player.exception.PlayerNotFoundException;
import com.chapeullah.guccigoblin.label.dto.LabelResponse;
import com.chapeullah.guccigoblin.label.player.PlayerLabelService;
import com.chapeullah.guccigoblin.leaguetier.LeagueTier;
import com.chapeullah.guccigoblin.leaguetier.LeagueTierService;
import com.chapeullah.guccigoblin.player.repository.PlayerRepository;
import com.chapeullah.guccigoblin.player.dto.*;
import com.chapeullah.guccigoblin.player.model.*;
import jakarta.transaction.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerService {

    private final ClashOfClansClient client;

    private final PlayerRepository playerRepository;

    private final LeagueTierService leagueTierService;
    private final BuilderBaseLeagueService builderBaseLeagueService;
    private final PlayerLabelService playerLabelService;
    private final PlayerAchievementService playerAchievementService;
    private final PlayerHouseElementService playerHouseElementService;
    private final PlayerTroopService playerTroopService;
    private final PlayerHeroEquipmentService playerHeroEquipmentService;
    private final PlayerHeroEquipmentLinkService playerHeroEquipmentLinkService;
    private final PlayerHeroService playerHeroService;
    private final PlayerSpellService playerSpellService;
    private final ClanService clanService;

    @Transactional
    public Player syncPlayer(@NonNull String playerTag) {
        PlayerResponse response;
        try {
            response = client.getPlayer(playerTag);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                throw new PlayerNotFoundException(playerTag, e);
            }
            throw e;
        }

        Player existingPlayer = playerRepository.findById(playerTag).orElse(null);

        Clan clan = response.clan() == null
                ? null
                : clanService.getOrSyncClanByTag(response.clan().tag());

        LeagueTier leagueTier = null;
        if (response.leagueTier() != null) {
            leagueTier = leagueTierService.syncAll()
                    .stream()
                    .filter(item -> item.getId().equals(response.leagueTier().id()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("League tier not found"));
        }

        BuilderBaseLeague builderBaseLeague = null;
        if (response.builderBaseLeague() != null) {
            builderBaseLeague = builderBaseLeagueService
                    .syncAllAndGetById(response.builderBaseLeague().id());
        }

        Player incomingPlayer = toPlayer(
                response,
                clan,
                leagueTier,
                builderBaseLeague,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>());

        Player player;
        if (existingPlayer == null) {
            player = playerRepository.save(incomingPlayer);
        } else {
            existingPlayer.updateFrom(incomingPlayer);
            player = existingPlayer;
        }

        List<PlayerAchievement> achievements = playerAchievementService.syncPlayerAchievements(
                player,
                response.achievements() == null ? List.of() : response.achievements());

        player.getAchievements().clear();
        player.getAchievements().addAll(achievements);

        List<PlayerHouseElementResponse> elements =
                response.playerHouse() == null || response.playerHouse().elements() == null
                        ? List.of()
                        : response.playerHouse().elements();

        List<PlayerHouseElement> houseElements =
                playerHouseElementService.syncPlayerHouseElements(player, elements);

        player.getHouseElements().clear();
        player.getHouseElements().addAll(houseElements);

        List<LabelResponse> labelsResponse =
                response.labels() == null ? List.of() : response.labels();

        Set<Integer> labelIds = new HashSet<>();
        labelsResponse.forEach(label -> labelIds.add(label.id()));

        player.getLabelLinks().removeIf(
                link -> !labelIds.contains(link.getLabel().getId()));

        if (!labelsResponse.isEmpty()) {
            var allLabels = playerLabelService.syncAll();
            Set<Integer> missingIds = new HashSet<>(labelIds);
            allLabels.forEach(label -> missingIds.remove(label.getId()));
            if (!missingIds.isEmpty()) {
                throw new IllegalStateException("Player labels not found: playerTag=\""
                        + player.getTag() + "\", labelIds=" + missingIds);
            }
            allLabels.stream()
                    .filter(label -> labelIds.contains(label.getId()))
                    .filter(label -> player.getLabelLinks().stream()
                            .noneMatch(link ->
                                    link.getLabel().getId().equals(label.getId())))
                    .forEach(label ->
                            player.getLabelLinks().add(new PlayerLabelLink(player, label)));
        }

        List<PlayerTroop> troops = playerTroopService.syncTroops(
                player,
                response.troops() == null ? List.of() : response.troops());

        player.getTroops().clear();
        player.getTroops().addAll(troops);

        List<PlayerHeroEquipmentResponse> equipmentResponse =
                response.heroEquipments() == null ? List.of() : response.heroEquipments();

        Set<String> equipmentNames = new HashSet<>();
        equipmentResponse.forEach(equipment -> equipmentNames.add(equipment.name()));

        boolean linksRemoved = false;
        for (PlayerHero playerHero : player.getHeroes()) {
            linksRemoved |= playerHero.getPlayerHeroEquipmentLinks().removeIf(
                    link -> !equipmentNames.contains(link.getHeroEquipment().getName()));
        }

        if (linksRemoved) {
            playerRepository.flush();
        }

        List<PlayerHeroEquipment> heroEquipment =
                playerHeroEquipmentService.syncHeroEquipments(player, equipmentResponse);

        player.getHeroEquipment().clear();
        player.getHeroEquipment().addAll(heroEquipment);

        List<PlayerHeroResponse> heroesResponse =
                response.heroes() == null ? List.of() : response.heroes();
        List<PlayerHero> heroes = playerHeroService.syncHeroes(player, heroesResponse);
        player.getHeroes().clear();
        player.getHeroes().addAll(heroes);
        for (PlayerHero playerHero : player.getHeroes()) {
            PlayerHeroResponse playerHeroResponse = heroesResponse.stream()
                    .filter(item -> item.name().equals(playerHero.getName()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "Hero response not found: " + playerHero.getName()));
            List<PlayerHeroEquipmentResponse> equipment =
                    playerHeroResponse.heroEquipmentsResponse() == null ? List.of()
                            : playerHeroResponse.heroEquipmentsResponse();
            List<PlayerHeroEquipmentLink> links =
                    playerHeroEquipmentLinkService.syncHeroEquipmentLinks(playerHero, equipment);
            playerHero.getPlayerHeroEquipmentLinks().clear();
            playerHero.getPlayerHeroEquipmentLinks().addAll(links);
        }

        List<PlayerSpell> spells = playerSpellService.syncPlayerSpells(
                player,
                response.spells() == null ? List.of() : response.spells());

        player.getSpells().clear();
        player.getSpells().addAll(spells);

        Player savedPlayer = playerRepository.save(player);

        log.info("Player synchronized: playerName=\"{}\", playerTag=\"{}\"",
                savedPlayer.getName(), savedPlayer.getTag());

        return savedPlayer;
    }

    private Player toPlayer(
            PlayerResponse response,
            Clan clan,
            LeagueTier leagueTier,
            BuilderBaseLeague builderBaseLeague,
            List<PlayerAchievement> achievements,
            List<PlayerHouseElement> houseElements,
            List<PlayerLabelLink> labelLinks,
            List<PlayerTroop> troops,
            List<PlayerHero> heroes,
            List<PlayerHeroEquipment> heroEquipment,
            List<PlayerSpell> spells) {
        return new Player(
                response.tag(),
                response.name(),
                response.townHallLevel(),
                response.townHallWeaponLevel(),
                response.expLevel(),
                response.trophies(),
                response.bestTrophies(),
                response.warStars(),
                response.attackWins(),
                response.defenseWins(),
                response.builderHallLevel(),
                response.builderBaseTrophies(),
                response.bestBuilderBaseTrophies(),
                response.role(),
                response.warPreference(),
                response.donations(),
                response.donationsReceived(),
                response.clanCapitalContributions(),
                clan,
                leagueTier,
                builderBaseLeague,
                response.currentLeagueGroupTag(),
                response.currentLeagueSeasonId(),
                response.previousLeagueGroupTag(),
                response.previousLeagueSeasonId(),
                achievements,
                houseElements,
                labelLinks,
                troops,
                heroes,
                heroEquipment,
                spells);
    }

}
