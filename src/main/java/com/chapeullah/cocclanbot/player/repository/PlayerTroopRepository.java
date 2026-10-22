package com.chapeullah.cocclanbot.player.repository;

import com.chapeullah.cocclanbot.player.model.PlayerTroop;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlayerTroopRepository extends JpaRepository<PlayerTroop, Long> {

    List<PlayerTroop> findAllByPlayerTag(String playerTag);

}
