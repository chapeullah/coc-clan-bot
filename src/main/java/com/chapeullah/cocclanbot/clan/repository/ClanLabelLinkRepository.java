package com.chapeullah.cocclanbot.clan.repository;

import com.chapeullah.cocclanbot.clan.model.ClanLabelLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClanLabelLinkRepository extends JpaRepository<ClanLabelLink, Long> {

    List<ClanLabelLink> findAllByClanTag(String clanTag);

}
