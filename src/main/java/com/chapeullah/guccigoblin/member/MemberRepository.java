package com.chapeullah.guccigoblin.member;

import com.chapeullah.guccigoblin.member.model.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByTagAndClanTag(String tag, String clanTag);

    List<Member> findAllByClanTag(String clanTag);

    List<Member> findAllByClanTagAndInClanTrueOrderByClanRankAsc(String clanTag);

}
