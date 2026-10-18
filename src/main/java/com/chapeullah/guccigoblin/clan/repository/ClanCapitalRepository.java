package com.chapeullah.guccigoblin.clan.repository;

import com.chapeullah.guccigoblin.clan.model.ClanCapital;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClanCapitalRepository extends JpaRepository<ClanCapital, String> {}
