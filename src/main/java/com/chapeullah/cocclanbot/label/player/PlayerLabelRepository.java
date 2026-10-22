package com.chapeullah.cocclanbot.label.player;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlayerLabelRepository extends JpaRepository<PlayerLabel, Integer> {}
