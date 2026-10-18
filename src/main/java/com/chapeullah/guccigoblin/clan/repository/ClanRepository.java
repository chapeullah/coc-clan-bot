package com.chapeullah.guccigoblin.clan.repository;

import com.chapeullah.guccigoblin.clan.model.Clan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClanRepository extends JpaRepository<Clan, String> {}
