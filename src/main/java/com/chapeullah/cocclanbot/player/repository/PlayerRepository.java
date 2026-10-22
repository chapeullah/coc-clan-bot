package com.chapeullah.cocclanbot.player.repository;

import com.chapeullah.cocclanbot.player.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlayerRepository extends JpaRepository<Player, String> {}
