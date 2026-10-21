package com.chapeullah.guccigoblin.war.repository;

import com.chapeullah.guccigoblin.war.model.War;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface WarRepository extends JpaRepository<War, Long> {

    Optional<War> findByClanTagAndOpponentClanTagAndStartTime(String clanTag, String opponentTag, Instant startTime);

    List<War> findAllByEndTimeLessThanEqualAndStateNot(Instant now, String state);

    Optional<War> findFirstByStateInOrderByStartTimeDesc(List<String> states);

    Optional<War> findFirstByClanTagAndStateInOrderByStartTimeDesc(String clanTag, List<String> states);

    List<War> findTop5ByClanTagAndStateOrderByEndTimeDesc(String clanTag, String state);

    List<War> findAllByClanTagAndEndTimeLessThanEqualAndWarLogSyncedAtIsNull(String clanTag, Instant now);

    List<War> findByClanTagAndStateOrderByEndTimeDesc(String clanTag, String state, Limit limit);

    List<War> findDistinctByClanTagAndStateAndWarLogSyncedAtIsNotNullAndParticipantsPlayerTagAndParticipants_ClanTag(
            String clanTag,
            String state,
            String memberTag,
            String participantClanTag,
            Sort sort);

}
