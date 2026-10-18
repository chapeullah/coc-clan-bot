package com.chapeullah.guccigoblin.player.service;

import com.chapeullah.guccigoblin.player.dto.PlayerHeroEquipmentResponse;
import com.chapeullah.guccigoblin.player.model.*;
import com.chapeullah.guccigoblin.player.repository.PlayerHeroEquipmentLinkRepository;
import jakarta.transaction.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerHeroEquipmentLinkService {
    
    private final PlayerHeroEquipmentLinkRepository playerHeroEquipmentLinkRepository;

    @Transactional
    public List<PlayerHeroEquipmentLink> syncHeroEquipmentLinks(
            @NonNull PlayerHero playerHero,
            @NonNull List<PlayerHeroEquipmentResponse> playerHeroEquipmentRespons) {
        List<PlayerHeroEquipmentLink> playerHeroEquipmentLinks = playerHeroEquipmentLinkRepository.findAllByPlayerHeroId(playerHero.getId());
        List<PlayerHeroEquipmentLink> newPlayerHeroEquipmentLinks = toHeroEquipmentLinks(playerHero, playerHeroEquipmentRespons);
        Map<String, PlayerHeroEquipmentLink> heroEquipmentLinkMap = new HashMap<>();
        Set<String> heroEquipmentLinkSet = new HashSet<>();
        playerHeroEquipmentLinks.forEach(item -> heroEquipmentLinkMap.put(item.getPlayerHeroEquipment().getName(), item));
        for (PlayerHeroEquipmentLink playerHeroEquipmentLink : newPlayerHeroEquipmentLinks) {
            String name = playerHeroEquipmentLink.getPlayerHeroEquipment().getName();
            PlayerHeroEquipmentLink existingPlayerHeroEquipmentLink = heroEquipmentLinkMap.get(name);
            if (existingPlayerHeroEquipmentLink == null) {
                heroEquipmentLinkMap.put(name, playerHeroEquipmentLink);
            }
            heroEquipmentLinkSet.add(name);
        }
        List<PlayerHeroEquipmentLink> savedPlayerHeroEquipmentLinks = playerHeroEquipmentLinkRepository
                .saveAll(heroEquipmentLinkSet.stream().map(heroEquipmentLinkMap::get).toList());
        for (PlayerHeroEquipmentLink playerHeroEquipmentLink : heroEquipmentLinkMap.values()) {
            if (!heroEquipmentLinkSet.contains(playerHeroEquipmentLink.getPlayerHeroEquipment().getName())) {
                playerHeroEquipmentLinkRepository.delete(playerHeroEquipmentLink);
            }
        }
        log.debug("Player hero equipment links synchronized: playerName=\"{}\", playerTag=\"{}\", heroName=\"{}\", count={}",
                playerHero.getPlayer().getName(),
                playerHero.getPlayer().getTag(),
                playerHero.getName(),
                savedPlayerHeroEquipmentLinks.size());
        return savedPlayerHeroEquipmentLinks;
    }
    
    private List<PlayerHeroEquipmentLink> toHeroEquipmentLinks(
            @NonNull PlayerHero playerHero,
            @NonNull List<PlayerHeroEquipmentResponse> playerHeroEquipmentRespons) {
        return playerHeroEquipmentRespons
                .stream()
                .map(heroEquipmentResponse -> {
                    PlayerHeroEquipment playerHeroEquipment = playerHero
                            .getPlayer()
                            .getHeroEquipment()
                            .stream()
                            .filter(item -> item.getName().equals(heroEquipmentResponse.name()))
                            .findFirst()
                            .orElseThrow(() -> new IllegalStateException(
                                    "Hero equipment not found: playerTag=" + playerHero.getPlayer().getTag()
                                            + ", hero=" + playerHero.getName()
                                            + ", equipment=" + heroEquipmentResponse.name()));
                    return new PlayerHeroEquipmentLink(playerHero, playerHeroEquipment);
                })
                .toList();
    }

}
