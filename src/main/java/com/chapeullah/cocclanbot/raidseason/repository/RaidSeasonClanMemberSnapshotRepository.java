package com.chapeullah.cocclanbot.raidseason.repository;

import com.chapeullah.cocclanbot.raidseason.model.RaidSeason;
import com.chapeullah.cocclanbot.raidseason.model.RaidSeasonClanMemberSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RaidSeasonClanMemberSnapshotRepository
        extends JpaRepository<RaidSeasonClanMemberSnapshot, Long> {

    List<RaidSeasonClanMemberSnapshot> findAllByRaidSeason(RaidSeason raidSeason);

}
