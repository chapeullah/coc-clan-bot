package com.chapeullah.guccigoblin.raidseason.service;

import com.chapeullah.guccigoblin.ClashOfClansClient;
import com.chapeullah.guccigoblin.clan.model.Clan;
import com.chapeullah.guccigoblin.clan.service.ClanService;
import com.chapeullah.guccigoblin.raidseason.dto.*;
import com.chapeullah.guccigoblin.raidseason.model.*;
import com.chapeullah.guccigoblin.raidseason.repository.RaidSeasonRepository;
import jakarta.transaction.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@DependsOn("environmentVariablesValidator")
public class RaidSeasonService {

    private static final DateTimeFormatter RAID_TIME_FORMAT = DateTimeFormatter.ofPattern("uuuuMMdd'T'HHmmss.SSSX");

    private final ClashOfClansClient client;
    private final RaidSeasonRepository raidSeasonRepository;
    private final ClanService clanService;
    private final RaidSeasonParticipantService raidSeasonParticipantService;
    private final RaidSeasonClanMemberSnapshotService clanMemberSnapshotService;

    private final JsonMapper jsonMapper;

    @Transactional
    public Optional<RaidSeason> findLatestByClanTag(@NonNull String clanTag) {
        return raidSeasonRepository.findFirstByClanTagOrderByStartTimeDesc(clanTag);
    }

    @Transactional
    public List<RaidSeason> getRecentByClanTag(@NonNull String clanTag) {
        return raidSeasonRepository.findTop5ByClanTagOrderByStartTimeDesc(clanTag);
    }

    @Transactional
    public Optional<RaidSeason> syncRaidSeason(@NonNull String clanTag) {
        String rawJson = client.getCurrentRaid(clanTag);
        RaidSeasonsResponse response = jsonMapper.readValue(rawJson, RaidSeasonsResponse.class);
        if (response == null) {
            throw new IllegalStateException("Raid seasons response is null: clanTag=\"" + clanTag + "\"");
        }
        if (response.items() == null) {
            throw new IllegalStateException("Raid season response items are null: clanTag=\"" + clanTag + "\"");
        }
        if (response.items().isEmpty()) {
            return Optional.empty();
        }
        if (response.items().size() != 1) {
            throw new IllegalStateException("Expected exactly one raid season: clanTag=\"" + clanTag
                    + "\", actualCount=" + response.items().size());
        }

        RaidSeasonResponse raidSeasonResponse = response.items().getFirst();
        RaidSeason raidSeason = raidSeasonRepository
                .findByClanTagAndStartTime(
                        clanTag,
                        parseTime(raidSeasonResponse.startTime()))
                .orElse(null);
        if (raidSeason == null) {
            raidSeason = createRaidSeason(clanService.getClanByTag(clanTag), raidSeasonResponse, rawJson);
        } else {
            raidSeason = updateRaidSeason(raidSeason, raidSeasonResponse, rawJson);
        }

        Integer expectedAttacks = raidSeasonResponse.totalAttacks();
        var participants = raidSeason.getParticipants();

        if (expectedAttacks == null
                || expectedAttacks < 0
                || participants.stream().anyMatch(participant ->
                participant.getAttackCount() == null
                        || participant.getAttackCount() < 0)) {
            throw new IllegalStateException(
                    "Raid attack counts are missing or invalid: clanTag=\""
                            + clanTag + "\"");
        }

        long actualAttacks = participants.stream()
                .mapToLong(RaidSeasonParticipant::getAttackCount)
                .sum();

        if (actualAttacks != expectedAttacks) {
            throw new IllegalStateException(
                    "Raid attacks count mismatch: clanTag=\"" + clanTag
                            + "\", expectedCount=" + expectedAttacks
                            + ", actualCount=" + actualAttacks);
        }

        return Optional.of(raidSeason);
    }

    private RaidSeason createRaidSeason(
            @NonNull Clan clan,
            @NonNull RaidSeasonResponse response,
            @NonNull String rawJson) {
        RaidSeason raidSeason = new RaidSeason(
                clan, response.state(),
                parseTime(response.startTime()),
                parseTime(response.endTime()),
                response.capitalTotalLoot(),
                response.raidsCompleted(),
                response.totalAttacks(),
                response.enemyDistrictsDestroyed(),
                response.offensiveReward(),
                response.defensiveReward(),
                rawJson);

        raidSeason = raidSeasonRepository.save(raidSeason);

        raidSeason.setParticipants(
                raidSeasonParticipantService.syncRaidSeasonParticipants(
                        raidSeason, response.members()));

        clanMemberSnapshotService.captureClanMembers(raidSeason);

        return raidSeason;
    }

    private RaidSeason updateRaidSeason(
            @NonNull RaidSeason raidSeason,
            @NonNull RaidSeasonResponse response,
            @NonNull String rawJson) {
        raidSeason.update(
                response.state(),
                response.capitalTotalLoot(),
                response.raidsCompleted(),
                response.totalAttacks(),
                response.enemyDistrictsDestroyed(),
                response.offensiveReward(),
                response.defensiveReward(),
                rawJson);
        raidSeason.setParticipants(
                raidSeasonParticipantService
                        .syncRaidSeasonParticipants(raidSeason, response.members()));
        return raidSeasonRepository.save(raidSeason);
    }


    private static Instant parseTime(String value) {
        return Instant.from(RAID_TIME_FORMAT.parse(value));
    }
}
