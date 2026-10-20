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
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RaidSeasonClanMemberSnapshotService {

    private static final int MIN_TOWN_HALL_LEVEL = 6;
    private static final int MAX_RAID_PARTICIPANTS = 50;

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

        Set<String> attackingTags = raidSeason.getParticipants().stream()
                .filter(participant -> participant.getAttackCount() != null
                        && participant.getAttackCount() > 0)
                .map(participant -> participant.getTag())
                .collect(Collectors.toSet());

        for (Member member : members) {
            RaidSeasonClanMemberSnapshot snapshot = snapshots.stream()
                    .filter(existing -> existing.getTag().equals(member.getTag()))
                    .findFirst()
                    .orElse(null);

            if (snapshot == null) {
                snapshots.add(createSnapshot(raidSeason, member, clan.getType(), attackingTags));
            } else {
                snapshot.updateParticipation(
                        member.getRole(),
                        clan.getType(),
                        getParticipationEligible(raidSeason, snapshot, member, clan.getType(), attackingTags));
                snapshotRepository.save(snapshot);
            }
        }
    }

    private RaidSeasonClanMemberSnapshot createSnapshot(
            @NonNull RaidSeason raidSeason,
            @NonNull Member member,
            @NonNull String clanType,
            @NonNull Set<String> attackingTags) {
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
                getParticipationEligible(raidSeason, snapshot, member, clanType, attackingTags));

        return snapshotRepository.save(snapshot);
    }

    private Boolean getParticipationEligible(
            @NonNull RaidSeason raidSeason,
            @NonNull RaidSeasonClanMemberSnapshot snapshot,
            @NonNull Member member,
            @NonNull String clanType,
            @NonNull Set<String> attackingTags) {
        if (attackingTags.contains(member.getTag())) {
            return true;
        }

        if (member.getTownHallLevel() != null && member.getTownHallLevel() < MIN_TOWN_HALL_LEVEL) {
            return false;
        }

        // Filling the last slot must not erase an earlier observed opportunity to attack.
        if (attackingTags.size() >= MAX_RAID_PARTICIPANTS
                && !Boolean.TRUE.equals(snapshot.getParticipationOpportunityObserved())) {
            return false;
        }

        if ("admin".equals(member.getRole())
                || "coLeader".equals(member.getRole())
                || "leader".equals(member.getRole())
                || "inviteOnly".equals(clanType)
                || "closed".equals(clanType)) {
            return true;
        }

        if (!"member".equals(member.getRole()) || !"open".equals(clanType)) {
            return null;
        }

        if (member.getJoinedAt() != null && !member.getJoinedAt().isAfter(raidSeason.getStartTime())) {
            return true;
        }

        if (member.getJoinedAfter() != null && !member.getJoinedAfter().isBefore(raidSeason.getStartTime())) {
            return false;
        }

        return null;
    }
}
