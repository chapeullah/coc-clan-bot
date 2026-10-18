package com.chapeullah.guccigoblin.player.repository;

import com.chapeullah.guccigoblin.player.model.PlayerHeroEquipmentLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlayerHeroEquipmentLinkRepository extends JpaRepository<PlayerHeroEquipmentLink, Long> {

    List<PlayerHeroEquipmentLink> findAllByPlayerHeroId(Long heroId);

}
