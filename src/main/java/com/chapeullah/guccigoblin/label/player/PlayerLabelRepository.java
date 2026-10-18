package com.chapeullah.guccigoblin.label.player;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlayerLabelRepository extends JpaRepository<PlayerLabel, Integer> {}
