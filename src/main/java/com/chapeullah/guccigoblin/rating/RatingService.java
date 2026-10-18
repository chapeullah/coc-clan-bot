package com.chapeullah.guccigoblin.rating;

import com.chapeullah.guccigoblin.raidseason.model.RaidSeason;
import com.chapeullah.guccigoblin.raidseason.model.RaidSeasonClanMemberSnapshot;
import com.chapeullah.guccigoblin.raidseason.model.RaidSeasonParticipant;
import com.chapeullah.guccigoblin.raidseason.service.RaidSeasonClanMemberSnapshotService;
import com.chapeullah.guccigoblin.raidseason.service.RaidSeasonParticipantService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RatingService {

    private final RaidSeasonClanMemberSnapshotService raidSeasonClanMemberSnapshotService;
    private final RaidSeasonParticipantService raidSeasonParticipantService;

    public RaidAverages calculateRaidAverages(@NonNull RaidSeason raidSeason) {
        List<RaidSeasonClanMemberSnapshot> clanMemberSnapshot =
                raidSeasonClanMemberSnapshotService.getByRaidSeason(raidSeason);
        if (clanMemberSnapshot.isEmpty()) {
            throw new IllegalStateException(
                    "Clan member snapshot is empty: raidSeasonId="
                            + raidSeason.getId() + ", clanTag=\""
                            + raidSeason.getClan().getTag() + "\"");
        }
        Map<String, RaidSeasonParticipant> raidSeasonParticipantsByTag =
                raidSeasonParticipantService.getByRaidSeason(raidSeason)
                        .stream()
                        .collect(Collectors.toMap(
                                RaidSeasonParticipant::getTag,
                                Function.identity()));


        int attackCountSum = 0;
        int resourceLootedSum = 0;
        for (var member : clanMemberSnapshot) {
            var participant = raidSeasonParticipantsByTag.get(member.getTag());
            if (participant != null) {
                attackCountSum += participant.getAttackCount();
                resourceLootedSum += participant.getCapitalResourcesLooted();
            }
        }

        int clanMemberSnapshotSize = clanMemberSnapshot.size();

        return new RaidAverages(
                (double) attackCountSum / clanMemberSnapshotSize,
                (double) resourceLootedSum / clanMemberSnapshotSize);
    }

}
