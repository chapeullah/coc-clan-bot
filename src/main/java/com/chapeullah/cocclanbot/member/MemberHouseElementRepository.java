package com.chapeullah.cocclanbot.member;

import com.chapeullah.cocclanbot.member.model.MemberHouseElement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberHouseElementRepository extends JpaRepository<MemberHouseElement, Long> {
}
