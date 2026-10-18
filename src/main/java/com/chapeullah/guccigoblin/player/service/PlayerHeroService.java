package com.chapeullah.guccigoblin.player.service;

import com.chapeullah.guccigoblin.player.dto.PlayerHeroResponse;
import com.chapeullah.guccigoblin.player.model.*;
import com.chapeullah.guccigoblin.player.repository.PlayerHeroRepository;
import jakarta.transaction.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerHeroService {

    private final PlayerHeroRepository playerHeroRepository;

    @Transactional
    public List<PlayerHero> syncHeroes(
            @NonNull Player player,
            @NonNull List<PlayerHeroResponse> heroesResponse) {
        List<PlayerHero> playerHeroes = playerHeroRepository.findAllByPlayerTag(player.getTag());
        List<PlayerHero> newPlayerHeroes = toHeroes(player, heroesResponse);
        Map<String, PlayerHero> heroesMap = new HashMap<>();
        Set<String> heroesSet = new HashSet<>();
        playerHeroes.forEach(item -> heroesMap
                .put(item.getName(), item));
        for (PlayerHero playerHero : newPlayerHeroes) {
            String name = playerHero.getName();
            PlayerHero existingPlayerHero = heroesMap.get(name);
            if (existingPlayerHero != null) {
                existingPlayerHero.updateFrom(playerHero);
            } else {
                heroesMap.put(name, playerHero);
            }
            heroesSet.add(name);
        }
        List<PlayerHero> savedPlayerHeroes = playerHeroRepository
                .saveAll(heroesSet.stream().map(heroesMap::get).toList());
        for (PlayerHero playerHero : heroesMap.values()) {
            if (!heroesSet.contains(playerHero.getName())) {
                playerHeroRepository.delete(playerHero);
            }
        }
        log.debug("Player heroes synchronized: playerName=\"{}\", playerTag=\"{}\", count={}",
                player.getName(),
                player.getTag(),
                savedPlayerHeroes.size());
        return savedPlayerHeroes;
    }

    private List<PlayerHero> toHeroes(
            @NonNull Player player,
            @NonNull List<PlayerHeroResponse> heroesResponse) {
        return heroesResponse
                .stream()
                .map(hero -> toHero(player, hero))
                .toList();
    }

    private PlayerHero toHero(
            @NonNull Player player,
            @NonNull PlayerHeroResponse response) {
        return new PlayerHero(
                player,
                response.name(),
                response.level(),
                response.maxLevel(),
                new ArrayList<>(),
                response.village());
    }

}
