package com.chapeullah.cocclanbot.war.repository;

import com.chapeullah.cocclanbot.war.model.WarLogEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface WarLogRepository extends JpaRepository<WarLogEntry, Long> {

    Optional<WarLogEntry> findByClanTagAndEndTime(String clanTag, Instant endTime);

    List<WarLogEntry> findByClanTagAndOpponentClanTagAndEndTimeGreaterThanAndEndTimeLessThanEqualOrderByEndTimeAsc(
            String clanTag, String opponentClanTag, Instant after, Instant through);
}
