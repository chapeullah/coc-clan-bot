package com.chapeullah.guccigoblin.player.repository;

import com.chapeullah.guccigoblin.player.model.PlayerHouseElement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlayerHouseElementRepository extends JpaRepository<PlayerHouseElement, Long> {

    List<PlayerHouseElement> findAllByPlayerTag(String playerTag);

}
