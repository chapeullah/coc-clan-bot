package com.chapeullah.guccigoblin.raidseason.service;

import com.chapeullah.guccigoblin.clan.model.Clan;
import com.chapeullah.guccigoblin.clan.service.ClanService;
import com.chapeullah.guccigoblin.member.MemberService;
import com.chapeullah.guccigoblin.member.model.Member;
import com.chapeullah.guccigoblin.raidseason.model.RaidSeason;
import com.chapeullah.guccigoblin.raidseason.model.RaidSeasonClanMemberSnapshot;
import com.chapeullah.guccigoblin.raidseason.repository.RaidSeasonClanMemberSnapshotRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RaidSeasonClanMemberSnapshotService {

    private final ClanService clanService;
    private final MemberService memberService;
    private final RaidSeasonClanMemberSnapshotRepository snapshotRepository;

    @Transactional(readOnly = true)
    public List<RaidSeasonClanMemberSnapshot> getByRaidSeason(@NonNull RaidSeason raidSeason) {
        return snapshotRepository.findAllByRaidSeason(raidSeason);
    }

    @Transactional
    public void syncClanMemberSnapshots(@NonNull RaidSeason raidSeason) {
        Instant now = Instant.now();
        if (!"ongoing".equals(raidSeason.getState())
                || now.isBefore(raidSeason.getStartTime())
                || !now.isBefore(raidSeason.getEndTime())) {
            return;
        }

        Clan clan = clanService.syncClan(raidSeason.getClan().getTag());
        List<Member> members = memberService.getCurrentMembersByClanTag(clan.getTag());

        if (!Instant.now().isBefore(raidSeason.getEndTime())) {
            return;
        }

        List<RaidSeasonClanMemberSnapshot> snapshots =
                snapshotRepository.findAllByRaidSeason(raidSeason);

        for (Member member : members) {
            RaidSeasonClanMemberSnapshot snapshot = snapshots.stream()
                    .filter(existing -> existing.getTag().equals(member.getTag()))
                    .findFirst()
                    .orElse(null);

            if (snapshot == null) {
                snapshots.add(createSnapshot(raidSeason, member, clan.getType()));
            } else {
                snapshot.updateParticipation(
                        member.getRole(),
                        clan.getType(),
                        getParticipationEligible(raidSeason, snapshot, member, clan.getType()));
                snapshotRepository.save(snapshot);
            }
        }
    }

    private RaidSeasonClanMemberSnapshot createSnapshot(
            @NonNull RaidSeason raidSeason,
            @NonNull Member member,
            @NonNull String clanType) {
        RaidSeasonClanMemberSnapshot snapshot = new RaidSeasonClanMemberSnapshot(
                raidSeason,
                member.getTag(),
                member.getName(),
                member.getJoinedAt(),
                member.getJoinedAfter(),
                member.getRole(),
                clanType,
                null);

        snapshot.updateParticipation(
                member.getRole(),
                clanType,
                getParticipationEligible(raidSeason, snapshot, member, clanType));

        return snapshotRepository.save(snapshot);
    }

    private Boolean getParticipationEligible(
            @NonNull RaidSeason raidSeason,
            @NonNull RaidSeasonClanMemberSnapshot snapshot,
            @NonNull Member member,
            @NonNull String clanType) {
        boolean hasAttacks = raidSeason.getParticipants().stream()
                .anyMatch(participant -> participant.getTag().equals(member.getTag())
                        && participant.getAttackCount() != null
                        && participant.getAttackCount() > 0);
        if (hasAttacks
                || "admin".equals(member.getRole())
                || "coLeader".equals(member.getRole())
                || "leader".equals(member.getRole())
                || "inviteOnly".equals(clanType)
                || "closed".equals(clanType)) {
            return true;
        }

        if (!"member".equals(member.getRole()) || !"open".equals(clanType)) {
            return null;
        }

        if (snapshot.getJoinedAt() != null && !snapshot.getJoinedAt().isAfter(raidSeason.getStartTime())) {
            return true;
        }

        if (snapshot.getJoinedAfter() != null && !snapshot.getJoinedAfter().isBefore(raidSeason.getStartTime())) {
            return false;
        }

        return null;
    }
}
