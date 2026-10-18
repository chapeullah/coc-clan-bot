package com.chapeullah.guccigoblin.player.repository;

import com.chapeullah.guccigoblin.player.model.PlayerAchievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlayerAchievementRepository extends JpaRepository<PlayerAchievement, Long> {

    List<PlayerAchievement> findAllByPlayerTag(String playerTag);

}
