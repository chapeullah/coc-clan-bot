package com.chapeullah.guccigoblin.player.service;

import com.chapeullah.guccigoblin.player.dto.PlayerHeroEquipmentResponse;
import com.chapeullah.guccigoblin.player.model.PlayerHeroEquipment;
import com.chapeullah.guccigoblin.player.model.Player;
import com.chapeullah.guccigoblin.player.repository.PlayerHeroEquipmentRepository;
import jakarta.transaction.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerHeroEquipmentService {

    private final PlayerHeroEquipmentRepository playerHeroEquipmentRepository;

    @Transactional
    public List<PlayerHeroEquipment> syncHeroEquipments(
            @NonNull Player player,
            @NonNull List<PlayerHeroEquipmentResponse> heroEquipmentsResponse) {
        List<PlayerHeroEquipment> playerHeroEquipments = playerHeroEquipmentRepository.findAllByPlayerTag(player.getTag());
        List<PlayerHeroEquipment> newPlayerHeroEquipments = toHeroEquipments(player, heroEquipmentsResponse);
        Map<String, PlayerHeroEquipment> heroEquipmentsMap = new HashMap<>();
        Set<String> heroEquipmentsSet = new HashSet<>();
        playerHeroEquipments.forEach(item -> heroEquipmentsMap.put(item.getName(), item));
        for (PlayerHeroEquipment playerHeroEquipment : newPlayerHeroEquipments) {
            String name = playerHeroEquipment.getName();
            PlayerHeroEquipment existingPlayerHeroEquipment = heroEquipmentsMap.get(name);
            if (existingPlayerHeroEquipment != null) {
                existingPlayerHeroEquipment.updateFrom(playerHeroEquipment);
            } else {
                heroEquipmentsMap.put(name, playerHeroEquipment);
            }
            heroEquipmentsSet.add(name);
        }
        List<PlayerHeroEquipment> savedPlayerHeroEquipments = playerHeroEquipmentRepository
                .saveAll(heroEquipmentsSet.stream().map(heroEquipmentsMap::get).toList());
        for (PlayerHeroEquipment playerHeroEquipment : heroEquipmentsMap.values()) {
            if (!heroEquipmentsSet.contains(playerHeroEquipment.getName())) {
                playerHeroEquipmentRepository.delete(playerHeroEquipment);
            }
        }
        log.debug("Player hero equipment synchronized: playerName=\"{}\", playerTag=\"{}\", count={}",
                player.getName(),
                player.getTag(),
                savedPlayerHeroEquipments.size());
        return savedPlayerHeroEquipments;
    }

    private List<PlayerHeroEquipment> toHeroEquipments(
            Player player,
            List<PlayerHeroEquipmentResponse> playerHeroEquipmentRespons) {
        return playerHeroEquipmentRespons
                .stream()
                .map(heroEquipment -> toHeroEquipment(player, heroEquipment))
                .toList();
    }

    private PlayerHeroEquipment toHeroEquipment(
            Player player,
            PlayerHeroEquipmentResponse playerHeroEquipmentResponse) {
        return new PlayerHeroEquipment(
                player,
                playerHeroEquipmentResponse.name(),
                playerHeroEquipmentResponse.level(),
                playerHeroEquipmentResponse.maxLevel(),
                playerHeroEquipmentResponse.village());
    }

}
