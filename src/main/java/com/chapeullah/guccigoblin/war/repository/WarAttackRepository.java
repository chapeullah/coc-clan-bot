package com.chapeullah.guccigoblin.war.repository;

import com.chapeullah.guccigoblin.war.model.War;
import com.chapeullah.guccigoblin.war.model.WarAttack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WarAttackRepository extends JpaRepository<WarAttack, Long> {

    boolean existsByWar(War war);

    boolean existsByWarAndAttackOrder(War war, Integer attackOrder);

    List<WarAttack> findAllByWar(War war);

}
