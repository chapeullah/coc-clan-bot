package com.chapeullah.cocclanbot.raidseason.repository;

import com.chapeullah.cocclanbot.raidseason.model.RaidSeason;
import com.chapeullah.cocclanbot.raidseason.model.RaidSeasonParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RaidSeasonParticipantRepository
        extends JpaRepository<RaidSeasonParticipant, Long> {

    List<RaidSeasonParticipant> findAllByRaidSeason(RaidSeason raidSeason);
}