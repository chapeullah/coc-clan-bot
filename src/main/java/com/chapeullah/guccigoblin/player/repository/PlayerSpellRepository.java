package com.chapeullah.guccigoblin.player.repository;

import com.chapeullah.guccigoblin.player.model.PlayerSpell;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlayerSpellRepository extends JpaRepository<PlayerSpell, Long> {

    List<PlayerSpell> findAllByPlayerTag(String playerTag);

}
