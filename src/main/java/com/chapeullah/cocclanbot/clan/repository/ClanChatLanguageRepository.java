package com.chapeullah.cocclanbot.clan.repository;

import com.chapeullah.cocclanbot.clan.model.ClanChatLanguage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClanChatLanguageRepository extends JpaRepository<ClanChatLanguage, Integer> {}
