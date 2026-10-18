package com.chapeullah.guccigoblin.raidseason.service;

import com.chapeullah.guccigoblin.ClashOfClansClient;
import com.chapeullah.guccigoblin.member.dto.MemberResponse;
import com.chapeullah.guccigoblin.member.dto.MemberResponses;
import com.chapeullah.guccigoblin.raidseason.model.RaidSeason;
import com.chapeullah.guccigoblin.raidseason.model.RaidSeasonClanMemberSnapshot;
import com.chapeullah.guccigoblin.raidseason.repository.RaidSeasonClanMemberSnapshotRepository;
import jakarta.transaction.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RaidSeasonClanMemberSnapshotService {

    private final ClashOfClansClient client;
    private final RaidSeasonClanMemberSnapshotRepository snapshotRepository;

    @Transactional
    public List<RaidSeasonClanMemberSnapshot> getByRaidSeason(@NonNull RaidSeason raidSeason) {
        return snapshotRepository.findAllByRaidSeason(raidSeason);
    }

    @Transactional
    public void captureClanMembers(@NonNull RaidSeason raidSeason) {
        if (snapshotRepository.existsByRaidSeason(raidSeason)) {
            return;
        }

        Instant now = Instant.now();

        if (now.isBefore(raidSeason.getStartTime())
                || !now.isBefore(raidSeason.getEndTime())) {
            log.warn(
                    "Cannot capture current clan members outside raid season: raidSeasonId={}",
                    raidSeason.getId());
            return;
        }

        String clanTag = raidSeason.getClan().getTag();
        MemberResponses response = client.getClanMembers(clanTag);

        if (response == null
                || response.memberList() == null
                || response.memberList().isEmpty()) {
            throw new IllegalStateException(
                    "Clan member list is missing or empty: clanTag=\""
                            + clanTag + "\"");
        }

        for (MemberResponse member : response.memberList()) {
            createSnapshot(raidSeason, member);
        }
    }

    private RaidSeasonClanMemberSnapshot createSnapshot(
            @NonNull RaidSeason raidSeason,
            @NonNull MemberResponse response) {
        RaidSeasonClanMemberSnapshot snapshot =
                new RaidSeasonClanMemberSnapshot(
                        raidSeason,
                        response.tag(),
                        response.name());

        return snapshotRepository.save(snapshot);
    }
}