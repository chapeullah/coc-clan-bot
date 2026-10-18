package com.chapeullah.guccigoblin.member;

import com.chapeullah.guccigoblin.member.model.MemberHouseElement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberHouseElementRepository extends JpaRepository<MemberHouseElement, Long> {
}
