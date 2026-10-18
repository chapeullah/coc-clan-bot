package com.chapeullah.guccigoblin.player.service;

import com.chapeullah.guccigoblin.player.dto.PlayerSpellResponse;
import com.chapeullah.guccigoblin.player.model.Player;
import com.chapeullah.guccigoblin.player.model.PlayerSpell;
import com.chapeullah.guccigoblin.player.repository.PlayerSpellRepository;
import jakarta.transaction.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerSpellService {

    private final PlayerSpellRepository playerSpellRepository;

    private record SpellKey(String name, String village) {}

    @Transactional
    public List<PlayerSpell> syncPlayerSpells(
            @NonNull Player player,
            @NonNull List<PlayerSpellResponse> playerSpellRespons) {
        List<PlayerSpell> playerSpells = playerSpellRepository.findAllByPlayerTag(player.getTag());
        List<PlayerSpell> newPlayerSpells = toSpells(player, playerSpellRespons);
        Map<SpellKey, PlayerSpell> spellMap = new HashMap<>();
        Set<SpellKey> spellSet = new HashSet<>();
        playerSpells.forEach(item -> spellMap
                .put(new SpellKey(item.getName(), item.getVillage()), item));
        for (PlayerSpell playerSpell : newPlayerSpells) {
            String name = playerSpell.getName();
            String village = playerSpell.getVillage();
            PlayerSpell existingPlayerSpell = spellMap.get(new SpellKey(name, village));
            if (existingPlayerSpell != null) {
                existingPlayerSpell.updateFrom(playerSpell);
            } else {
                spellMap.put(new SpellKey(name, village), playerSpell);
            }
            spellSet.add(new SpellKey(name, village));
        }
        List<PlayerSpell> savedPlayerSpells = playerSpellRepository
                .saveAll(spellSet.stream().map(spellMap::get).toList());
        for (PlayerSpell playerSpell : spellMap.values()) {
            if (!spellSet.contains(new SpellKey(playerSpell.getName(), playerSpell.getVillage()))) {
                playerSpellRepository.delete(playerSpell);
            }
        }
        log.debug("Player spells synchronized: playerName=\"{}\", playerTag=\"{}\", count={}",
                player.getName(),
                player.getTag(),
                savedPlayerSpells.size());
        return savedPlayerSpells;
    }

    private List<PlayerSpell> toSpells(Player player, List<PlayerSpellResponse> playerSpellRespons) {
        return playerSpellRespons
                .stream()
                .map(spell -> toSpell(player, spell))
                .toList();
    }

    private PlayerSpell toSpell(Player player, PlayerSpellResponse playerSpellResponse) {
        return new PlayerSpell(
                player,
                playerSpellResponse.name(),
                playerSpellResponse.level(),
                playerSpellResponse.maxLevel(),
                playerSpellResponse.village());
    }

}
