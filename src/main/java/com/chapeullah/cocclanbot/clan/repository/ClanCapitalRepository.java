package com.chapeullah.cocclanbot.clan.repository;

import com.chapeullah.cocclanbot.clan.model.ClanCapital;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClanCapitalRepository extends JpaRepository<ClanCapital, String> {}
