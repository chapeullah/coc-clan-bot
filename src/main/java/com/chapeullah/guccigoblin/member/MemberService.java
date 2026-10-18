package com.chapeullah.guccigoblin.member;

import com.chapeullah.guccigoblin.builderbaseleague.BuilderBaseLeague;
import com.chapeullah.guccigoblin.builderbaseleague.BuilderBaseLeagueService;
import com.chapeullah.guccigoblin.clan.dto.ClanMemberResponse;
import com.chapeullah.guccigoblin.clan.model.Clan;
import com.chapeullah.guccigoblin.leaguetier.LeagueTier;
import com.chapeullah.guccigoblin.leaguetier.LeagueTierService;
import com.chapeullah.guccigoblin.member.model.Member;
import com.chapeullah.guccigoblin.member.model.MemberDelta.Snapshot;
import com.chapeullah.guccigoblin.member.model.MemberHouseElement;
import jakarta.transaction.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;

    private final LeagueTierService leagueTierService;
    private final BuilderBaseLeagueService builderBaseLeagueService;
    private final MemberHouseElementService memberHouseElementService;
    private final MemberDeltaService memberDeltaService;

    public Optional<Member> findByTagAndClanTag(@NonNull String tag, @NonNull String clanTag) {
        return memberRepository.findByTagAndClanTag(tag, clanTag);
    }

    public List<Member> getAllMembersByClanTag(@NonNull String clanTag) {
        return memberRepository.findAllByClanTag(clanTag);
    }

    @Transactional
    public List<Member> getCurrentMembersByClanTag(@NonNull String clanTag) {
        return memberRepository.findAllByClanTagAndInClanTrueOrderByClanRankAsc(clanTag);
    }

    @Transactional
    public List<Member> syncMembers(
            @NonNull Clan clan,
            @NonNull List<ClanMemberResponse> clanMemberResponses) {
        if (clanMemberResponses.isEmpty()) {
            throw new IllegalArgumentException("Clan member list is empty: clanTag=\"" + clan.getTag() + "\"");
        }

        syncLeagueTiersAndBuilderBaseLeaguesIfMissing(clanMemberResponses);

        Map<String, Member> remainingMembersByTag = memberRepository
                .findAllByClanTag(clan.getTag())
                .stream()
                .collect(Collectors.toMap(
                        Member::getTag,
                        Function.identity()));

        Map<String, Snapshot> oldMembersByTag =
                remainingMembersByTag.values().stream()
                        .collect(Collectors.toMap(
                                Member::getTag,
                                Snapshot::from));

        Instant now = Instant.now();
        List<Member> currentMembers = new ArrayList<>();

        for (ClanMemberResponse response : clanMemberResponses) {
            Member member = remainingMembersByTag.remove(response.tag());

            if (member == null) {
                member = createMember(clan, response);
            } else {
                updateMember(member, response, now);
            }

            currentMembers.add(member);
        }

        for (Member member : remainingMembersByTag.values()) {
            member.leave(now);
        }

        Map<String, Member> newMembersByTag = currentMembers
                .stream()
                .collect(Collectors.toMap(
                        Member::getTag,
                        Function.identity()));

        memberDeltaService.memberDeltaOutput(
                oldMembersByTag,
                newMembersByTag);

        return currentMembers;
    }

    private Member createMember(
            @NonNull Clan clan,
            @NonNull ClanMemberResponse response) {
        LeagueTier leagueTier = response.leagueTier() == null ? null
                : leagueTierService.getById(response.leagueTier().id());
        BuilderBaseLeague builderBaseLeague = response.builderBaseLeague() == null ? null
                : builderBaseLeagueService.getById(response.builderBaseLeague().id());

        Member member = new Member(
                response.tag(),
                clan,
                response.name(),
                response.role(),
                response.townHallLevel(),
                response.expLevel(),
                leagueTier,
                response.trophies(),
                response.builderBaseTrophies(),
                response.clanRank(),
                response.previousClanRank(),
                response.donations(),
                response.donationsReceived(),
                builderBaseLeague);

        Member savedMember = memberRepository.save(member);

        memberHouseElementService.syncAndGetByMember(savedMember, response.playerHouse());

        return savedMember;
    }

    private void updateMember(
            @NonNull Member member,
            @NonNull ClanMemberResponse response,
            @NonNull Instant now) {
        if (!Objects.equals(member.getTag(), response.tag())) {
            throw new IllegalArgumentException(
                    "Member tags mismatch: currentMemberTag=\"" + member.getTag() + "\", sourceMemberTag=\""
                            + response.tag() + "\"");
        }

        LeagueTier leagueTier = response.leagueTier() == null ? null
                : leagueTierService.getById(response.leagueTier().id());
        BuilderBaseLeague builderBaseLeague = response.builderBaseLeague() == null ? null
                : builderBaseLeagueService.getById(response.builderBaseLeague().id());

        member.update(
                response.name(),
                response.role(),
                response.townHallLevel(),
                response.expLevel(),
                leagueTier,
                response.trophies(),
                response.builderBaseTrophies(),
                response.clanRank(),
                response.previousClanRank(),
                response.donations(),
                response.donationsReceived(),
                builderBaseLeague,
                now);

        Map<Integer, String> elementsBefore =
                member.getMemberHouseElements().stream()
                        .collect(Collectors.toMap(
                                MemberHouseElement::getElementId,
                                MemberHouseElement::getElementType));
        Map<Integer, String> elementsAfter =
                memberHouseElementService
                        .syncAndGetByMember(member, response.playerHouse())
                        .stream()
                        .collect(Collectors.toMap(
                                MemberHouseElement::getElementId,
                                MemberHouseElement::getElementType));
        if (!Objects.equals(elementsBefore, elementsAfter)) {
            member.markHouseElementsChanged(now);
        }

        member.rejoin(now);
    }

    private void syncLeagueTiersAndBuilderBaseLeaguesIfMissing(
            @NonNull List<ClanMemberResponse> responses) {
        boolean leagueTiersSyncRequired = false, builderBaseLeaguesSyncRequired = false;
        for (ClanMemberResponse response : responses) {
            if (response.leagueTier() == null && response.builderBaseLeague() == null) {
                continue;
            }

            // leagueTier
            Integer leagueTierId = response.leagueTier() == null ? null
                    : response.leagueTier().id();
            if (!leagueTiersSyncRequired
                    && leagueTierId != null
                    && leagueTierService.findById(leagueTierId).isEmpty()) {
                leagueTiersSyncRequired = true;
            }

            // builderBaseLeague
            Integer builderBaseLeagueId = response.builderBaseLeague() == null ? null
                    : response.builderBaseLeague().id();
            if (!builderBaseLeaguesSyncRequired
                    && builderBaseLeagueId != null
                    && builderBaseLeagueService.findById(builderBaseLeagueId).isEmpty()) {
                builderBaseLeaguesSyncRequired = true;
            }

            if (leagueTiersSyncRequired && builderBaseLeaguesSyncRequired) {
                break;
            }
        }
        if (leagueTiersSyncRequired) {
            leagueTierService.syncAll();
        }
        if (builderBaseLeaguesSyncRequired) {
            builderBaseLeagueService.syncAll();
        }
    }

}
