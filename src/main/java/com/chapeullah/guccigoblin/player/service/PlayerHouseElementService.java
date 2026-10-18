package com.chapeullah.guccigoblin.player.service;

import com.chapeullah.guccigoblin.player.dto.PlayerHouseElementResponse;
import com.chapeullah.guccigoblin.player.model.PlayerHouseElement;
import com.chapeullah.guccigoblin.player.model.Player;
import com.chapeullah.guccigoblin.player.repository.PlayerHouseElementRepository;
import jakarta.transaction.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerHouseElementService {

    private final PlayerHouseElementRepository playerHouseElementRepository;

    @Transactional
    public List<PlayerHouseElement> syncPlayerHouseElements(
            @NonNull Player player,
            @NonNull List<PlayerHouseElementResponse> houseElementsResponse) {
        List<PlayerHouseElement> playerHouseElements = playerHouseElementRepository.findAllByPlayerTag(player.getTag());
        List<PlayerHouseElement> newPlayerHouseElements = toHouseElements(player, houseElementsResponse);
        Map<Integer, PlayerHouseElement> houseElementsMap = new HashMap<>();
        Set<Integer> houseElementsSet = new HashSet<>();
        playerHouseElements.forEach(item -> houseElementsMap.put(item.getElementId(), item));
        for (PlayerHouseElement playerHouseElement : newPlayerHouseElements) {
            Integer id = playerHouseElement.getElementId();
            PlayerHouseElement existingPlayerHouseElement = houseElementsMap.get(id);
            if (existingPlayerHouseElement != null) {
                existingPlayerHouseElement.updateFrom(playerHouseElement);
            } else {
                houseElementsMap.put(id, playerHouseElement);
            }
            houseElementsSet.add(id);
        }
        List<PlayerHouseElement> savedPlayerHouseElements = playerHouseElementRepository
                .saveAll(houseElementsSet.stream().map(houseElementsMap::get).toList());
        for (PlayerHouseElement playerHouseElement : houseElementsMap.values()) {
            if (!houseElementsSet.contains(playerHouseElement.getElementId())) {
                playerHouseElementRepository.delete(playerHouseElement);
            }
        }
        log.debug("Player house elements synchronized: playerName=\"{}\", playerTag=\"{}\", count={}",
                player.getName(),
                player.getTag(),
                savedPlayerHouseElements.size());
        return savedPlayerHouseElements;
    }

    private List<PlayerHouseElement> toHouseElements(
            Player player,
            List<PlayerHouseElementResponse> response) {
        return response
                .stream()
                .map(houseElement -> toHouseElement(player, houseElement))
                .toList();
    }

    private PlayerHouseElement toHouseElement(
            Player player,
            PlayerHouseElementResponse response) {
        return new PlayerHouseElement(
                player, response.id(), response.type());
    }

}
