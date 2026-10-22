package com.chapeullah.cocclanbot.label.clan;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClanLabelRepository extends JpaRepository<ClanLabel, Integer> {}
