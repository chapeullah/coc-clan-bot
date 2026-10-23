package com.chapeullah.cocclanbot.telegram.memberlink;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberTelegramLinkRepository extends JpaRepository<MemberTelegramLink, String> {

    @Modifying
    @Query("delete from MemberTelegramLink link where link.memberTag = :memberTag")
    int deleteByMemberTag(@Param("memberTag") String memberTag);
}
