package com.chapeullah.guccigoblin.capitalleague;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CapitalLeagueRepository extends JpaRepository<CapitalLeague, Integer> {}
