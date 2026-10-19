package com.chapeullah.guccigoblin.member;

import com.chapeullah.guccigoblin.clan.dto.ClanMemberResponse;
import com.chapeullah.guccigoblin.clan.model.Clan;
import com.chapeullah.guccigoblin.member.model.Member;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MemberServiceTest {

    @Test
    void firstRosterSyncLeavesJoiningLowerBoundUnknown() {
        Fixture fixture = new Fixture();
        Instant observedAt = Instant.now().minusSeconds(10);

        Member member = fixture.service.syncMembers(
                fixture.clan, List.of(response("#PLAYER")), observedAt).getFirst();

        assertNull(member.getJoinedAfter());
        assertEquals(observedAt, fixture.clan.getMemberListSyncedAt());
    }

    @Test
    void newMemberUsesPreviousSuccessfulRosterObservation() {
        Fixture fixture = new Fixture();
        Instant previousObservation = Instant.now().minusSeconds(20);
        Instant currentObservation = Instant.now().minusSeconds(10);
        Member existing = fixture.service.syncMembers(fixture.clan,
                List.of(response("#OLD")), previousObservation).getFirst();
        Instant existingJoinedAt = existing.getJoinedAt();

        List<Member> currentMembers = fixture.service.syncMembers(fixture.clan,
                List.of(response("#OLD"), response("#NEW")), currentObservation);
        Member added = currentMembers.stream()
                .filter(member -> "#NEW".equals(member.getTag()))
                .findFirst().orElseThrow();

        assertEquals(previousObservation, added.getJoinedAfter());
        assertEquals(currentObservation, fixture.clan.getMemberListSyncedAt());
        assertEquals(existingJoinedAt, existing.getJoinedAt());
        assertNull(existing.getJoinedAfter());
    }

    @Test
    void rejoiningMemberUsesLastRosterWhereItWasAbsent() {
        Fixture fixture = new Fixture();
        Instant firstObservation = Instant.now().minusSeconds(30);
        Member player = fixture.service.syncMembers(fixture.clan,
                List.of(response("#PLAYER")), firstObservation).getFirst();
        Instant absentObservation = firstObservation.plusSeconds(10);
        fixture.service.syncMembers(fixture.clan,
                List.of(response("#OTHER")), absentObservation);
        assertEquals(Boolean.FALSE, player.getInClan());

        Instant returnedObservation = absentObservation.plusSeconds(10);
        fixture.service.syncMembers(fixture.clan,
                List.of(response("#PLAYER"), response("#OTHER")), returnedObservation);

        assertEquals(Boolean.TRUE, player.getInClan());
        assertEquals(absentObservation, player.getJoinedAfter());
        assertEquals(returnedObservation, fixture.clan.getMemberListSyncedAt());
        assertEquals(2, fixture.members.size());
    }

    private static ClanMemberResponse response(String tag) {
        return new ClanMemberResponse(tag, tag, "member", 16, 200,
                null, null, 5000, 1000, 1, null, 0, 0, null, null);
    }

    private static class Fixture {

        private final Clan clan = new Clan("#CLAN", "Clan", "open", null, null, null,
                null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null);
        private final List<Member> members = new ArrayList<>();
        private final MemberService service;

        private Fixture() {
            MemberRepository repository = (MemberRepository) Proxy.newProxyInstance(
                    MemberRepository.class.getClassLoader(),
                    new Class<?>[]{MemberRepository.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "findAllByClanTag" -> members;
                        case "save" -> {
                            Member member = (Member) args[0];
                            if (!members.contains(member)) {
                                members.add(member);
                            }
                            yield member;
                        }
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
            service = new MemberService(repository, null, null,
                    new MemberHouseElementService(null), new MemberDeltaService());
        }
    }
}
