package com.chapeullah.cocclanbot.clan.repository;

import com.chapeullah.cocclanbot.clan.model.ClanCapitalDistrict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClanCapitalDistrictRepository extends JpaRepository<ClanCapitalDistrict, Long> {}
