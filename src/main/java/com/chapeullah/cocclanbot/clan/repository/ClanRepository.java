package com.chapeullah.cocclanbot.clan.repository;

import com.chapeullah.cocclanbot.clan.model.Clan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClanRepository extends JpaRepository<Clan, String> {}
