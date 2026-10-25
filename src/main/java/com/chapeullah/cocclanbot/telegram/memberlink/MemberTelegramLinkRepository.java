package com.chapeullah.cocclanbot.telegram.memberlink;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MemberTelegramLinkRepository extends JpaRepository<MemberTelegramLink, String> {

    List<MemberTelegramLink> findAllByTelegramUserId(Long telegramUserId);

    boolean existsByTelegramUserId(Long telegramUserId);

    int deleteByMemberTag(String memberTag);

}
