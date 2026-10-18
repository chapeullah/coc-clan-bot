package com.chapeullah.guccigoblin.clan.repository;

import com.chapeullah.guccigoblin.clan.model.ClanLabelLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClanLabelLinkRepository extends JpaRepository<ClanLabelLink, Long> {

    List<ClanLabelLink> findAllByClanTag(String clanTag);

}
