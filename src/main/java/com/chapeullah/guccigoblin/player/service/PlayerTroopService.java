package com.chapeullah.guccigoblin.player.service;

import com.chapeullah.guccigoblin.player.dto.PlayerTroopResponse;
import com.chapeullah.guccigoblin.player.model.Player;
import com.chapeullah.guccigoblin.player.model.PlayerTroop;
import com.chapeullah.guccigoblin.player.repository.PlayerTroopRepository;
import jakarta.transaction.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerTroopService {

    private final PlayerTroopRepository playerTroopRepository;

    private record TroopKey(String name, String village) {}

    @Transactional
    public List<PlayerTroop> syncTroops(
            @NonNull Player player,
            @NonNull List<PlayerTroopResponse> troopsResponse) {
        List<PlayerTroop> playerTroops = playerTroopRepository.findAllByPlayerTag(player.getTag());
        List<PlayerTroop> newPlayerTroops = toTroops(player, troopsResponse);
        Map<TroopKey, PlayerTroop> troopsMap = new HashMap<>();
        Set<TroopKey> troopsSet = new HashSet<>();
        playerTroops.forEach(item -> troopsMap
                .put(new TroopKey(item.getName(), item.getVillage()), item));
        for (PlayerTroop playerTroop : newPlayerTroops) {
            String name = playerTroop.getName();
            String village = playerTroop.getVillage();
            PlayerTroop existingPlayerTroop = troopsMap.get(new TroopKey(name, village));
            if (existingPlayerTroop != null) {
                existingPlayerTroop.updateFrom(playerTroop);
            } else {
                troopsMap.put(new TroopKey(name, village), playerTroop);
            }
            troopsSet.add(new TroopKey(name, village));
        }
        List<PlayerTroop> savedPlayerTroops = playerTroopRepository
                .saveAll(troopsSet.stream().map(troopsMap::get).toList());
        for (PlayerTroop playerTroop : troopsMap.values()) {
            if (!troopsSet.contains(new TroopKey(playerTroop.getName(), playerTroop.getVillage()))) {
                playerTroopRepository.delete(playerTroop);
            }
        }
        log.debug("Player troops synchronized: playerName=\"{}\", playerTag=\"{}\", count={}",
                player.getName(),
                player.getTag(),
                savedPlayerTroops.size());
        return savedPlayerTroops;
    }

    private List<PlayerTroop> toTroops(Player player, List<PlayerTroopResponse> response) {
        return response
                .stream()
                .map(troop -> toTroop(player, troop))
                .toList();
    }

    private PlayerTroop toTroop(Player player, PlayerTroopResponse response) {
        return new PlayerTroop(
                player,
                response.name(),
                response.level(),
                response.maxLevel(),
                response.village());
    }

}
