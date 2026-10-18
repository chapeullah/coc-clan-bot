package com.chapeullah.guccigoblin.player.service;

import com.chapeullah.guccigoblin.player.dto.PlayerAchievementResponse;
import com.chapeullah.guccigoblin.player.model.PlayerAchievement;
import com.chapeullah.guccigoblin.player.model.Player;
import com.chapeullah.guccigoblin.player.repository.PlayerAchievementRepository;
import jakarta.transaction.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerAchievementService {

    private final PlayerAchievementRepository playerAchievementRepository;

    @Transactional
    public List<PlayerAchievement> syncPlayerAchievements(
            @NonNull Player player,
            @NonNull List<PlayerAchievementResponse> achievementsResponse) {
        List<PlayerAchievement> playerAchievements = playerAchievementRepository.findAllByPlayerTag(player.getTag());
        List<PlayerAchievement> newPlayerAchievements = toAchievements(player, achievementsResponse);
        Map<String, PlayerAchievement> achievementsMap = new HashMap<>();
        Set<String> achievementsSet = new HashSet<>();
        playerAchievements.forEach(item -> achievementsMap.put(item.getName(), item));
        for (PlayerAchievement playerAchievement : newPlayerAchievements) {
            String name = playerAchievement.getName();
            PlayerAchievement existingPlayerAchievement = achievementsMap.get(name);
            if (existingPlayerAchievement != null) {
                existingPlayerAchievement.updateFrom(playerAchievement);
            } else {
                achievementsMap.put(name, playerAchievement);
            }
            achievementsSet.add(name);
        }
        List<PlayerAchievement> savedPlayerAchievements = playerAchievementRepository
                .saveAll(achievementsSet.stream().map(achievementsMap::get).toList());
        for (PlayerAchievement playerAchievement : achievementsMap.values()) {
            if (!achievementsSet.contains(playerAchievement.getName())) {
                playerAchievementRepository.delete(playerAchievement);
            }
        }
        log.debug("Player achievements synchronized: playerName=\"{}\", playerTag=\"{}\", count={}",
                player.getName(),
                player.getTag(),
                savedPlayerAchievements.size());
        return savedPlayerAchievements;
    }

    private List<PlayerAchievement> toAchievements(
            Player player,
            List<PlayerAchievementResponse> achievementsResponse) {
        return achievementsResponse
                .stream()
                .map(item -> toAchievement(player, item))
                .toList();
    }

    private PlayerAchievement toAchievement(
            Player player,
            PlayerAchievementResponse playerAchievementResponse) {
        return new PlayerAchievement(
                player,
                playerAchievementResponse.name(),
                playerAchievementResponse.stars(),
                playerAchievementResponse.value(),
                playerAchievementResponse.target(),
                playerAchievementResponse.info(),
                playerAchievementResponse.completionInfo(),
                playerAchievementResponse.village());
    }

}
