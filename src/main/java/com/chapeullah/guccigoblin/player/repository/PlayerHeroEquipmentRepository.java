package com.chapeullah.guccigoblin.player.repository;

import com.chapeullah.guccigoblin.player.model.PlayerHeroEquipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlayerHeroEquipmentRepository extends JpaRepository<PlayerHeroEquipment, Long> {

    List<PlayerHeroEquipment> findAllByPlayerTag(String playerTag);

}
