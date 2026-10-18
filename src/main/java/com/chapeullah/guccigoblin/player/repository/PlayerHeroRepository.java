package com.chapeullah.guccigoblin.player.repository;

import com.chapeullah.guccigoblin.player.model.PlayerHero;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlayerHeroRepository extends JpaRepository<PlayerHero, Long> {

    List<PlayerHero> findAllByPlayerTag(String playerTag);

}
