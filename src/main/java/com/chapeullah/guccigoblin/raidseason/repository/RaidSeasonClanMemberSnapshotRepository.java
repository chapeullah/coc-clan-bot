package com.chapeullah.guccigoblin.raidseason.repository;

import com.chapeullah.guccigoblin.raidseason.model.RaidSeason;
import com.chapeullah.guccigoblin.raidseason.model.RaidSeasonClanMemberSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RaidSeasonClanMemberSnapshotRepository
        extends JpaRepository<RaidSeasonClanMemberSnapshot, Long> {

    boolean existsByRaidSeason(RaidSeason raidSeason);

    List<RaidSeasonClanMemberSnapshot> findAllByRaidSeason(RaidSeason raidSeason);

}
