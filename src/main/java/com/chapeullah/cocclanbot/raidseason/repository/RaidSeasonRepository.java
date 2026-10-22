package com.chapeullah.cocclanbot.raidseason.repository;

import com.chapeullah.cocclanbot.raidseason.model.RaidSeason;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface RaidSeasonRepository extends JpaRepository<RaidSeason, Long> {

    Optional<RaidSeason> findFirstByClanTagOrderByStartTimeDesc(String clanTag);

    List<RaidSeason> findTop5ByClanTagOrderByStartTimeDesc(String clanTag);

    Optional<RaidSeason> findByClanTagAndStartTime(String clanTag, Instant startTime);

    List<RaidSeason> findByClanTagAndStateOrderByEndTimeDesc(String clanTag, String state, Limit limit);

}
