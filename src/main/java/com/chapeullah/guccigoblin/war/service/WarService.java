package com.chapeullah.guccigoblin.war.service;

import com.chapeullah.guccigoblin.ClashOfClansClient;
import com.chapeullah.guccigoblin.war.dto.WarClanBadgeUrlsResponse;
import com.chapeullah.guccigoblin.war.dto.WarResponse;
import com.chapeullah.guccigoblin.war.model.War;
import com.chapeullah.guccigoblin.war.model.WarClanBadgeUrls;
import com.chapeullah.guccigoblin.war.model.WarLogEntry;
import com.chapeullah.guccigoblin.war.repository.WarRepository;
import jakarta.transaction.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WarService {

    private static final DateTimeFormatter WAR_TIME_FORMAT = DateTimeFormatter.ofPattern("uuuuMMdd'T'HHmmss.SSSX");

    private final ClashOfClansClient client;

    private final WarRepository warRepository;
    private final Clock clock;

    private final WarParticipantService warParticipantService;
    private final WarAttackService warAttackService;
    private final WarLogService warLogService;

    @Transactional
    public Optional<War> findCurrentByClanTag(@NonNull String clanTag) {
        return warRepository.findFirstByClanTagAndStateInOrderByStartTimeDesc(
                clanTag, List.of("preparation", "inWar"));
    }

    @Transactional
    public List<War> getRecentCompletedByClanTag(@NonNull String clanTag) {
        return warRepository.findTop5ByClanTagAndStateOrderByEndTimeDesc(clanTag, "warEnded");
    }

    @Transactional
    public Optional<War> syncCurrentWar(@NonNull String clanTag) {
        WarResponse response = client.getCurrentWar(clanTag);
        if (response == null) {
            throw new IllegalStateException("Empty current war response: clanTag=" + clanTag);
        }
        if ("notInWar".equals(response.state())) {
            return Optional.empty();
        }

        War war = warRepository
                .findByClanTagAndOpponentClanTagAndStartTime(
                        response.clan().tag(),
                        response.opponent().tag(),
                        Instant.from(WAR_TIME_FORMAT.parse(response.startTime())))
                .orElse(null);

        WarClanBadgeUrls warClanBadgeUrls = toWarClanBadgeUrls(response.clan().badgeUrls());
        if (war == null) {
            war = createWar(response);
            war.setWarClanBadgeUrls(warClanBadgeUrls);
        } else {
            if (war.getWarLogSyncedAt() == null) {
                updateWar(war, response);
            }
            if (!Objects.equals(war.getWarClanBadgeUrls(), warClanBadgeUrls)) {
                war.setWarClanBadgeUrls(warClanBadgeUrls);
                war.setWarClanBadgeUrlsChangedAt(clock.instant());
            }
        }

        if (!clock.instant().isBefore(war.getEndTime())) {
            war.finish();
        }

        war = warRepository.save(war);

        warParticipantService.syncWarParticipants(war, response.clan(), response.opponent());

        warAttackService.syncAttacks(war, response.clan(), response.opponent());

        return Optional.of(war);
    }

    @Transactional
    public List<War> finishEndedWars() {
        return warRepository.saveAll(warRepository
                .findAllByEndTimeLessThanEqualAndStateNot(
                        clock.instant(),
                        "warEnded")
                .stream()
                .map(War::finish)
                .toList());
    }

    @Transactional
    public List<War> syncEndedWarsFromWarLog(@NonNull String clanTag) {
        Instant now = clock.instant();

        List<War> wars = warRepository
                .findAllByClanTagAndEndTimeLessThanEqualAndWarLogSyncedAtIsNull(
                        clanTag, now);

        List<War> syncedWars = new ArrayList<>();

        for (War war : wars) {
            WarLogEntry entry = warLogService
                    .findByClanTagAndEndTime(war.getClanTag(), war.getEndTime())
                    .orElse(null);

            if (entry == null) {
                continue;
            }

            if (!Objects.equals(war.getOpponentClanTag(), entry.getOpponentClanTag())
                    || !Objects.equals(war.getTeamSize(), entry.getTeamSize())
                    || (entry.getAttacksPerMember() != null
                    && !Objects.equals(
                    war.getAttacksPerMember(), entry.getAttacksPerMember()))
                    || (entry.getBattleModifier() != null
                    && !Objects.equals(
                    war.getBattleModifier(), entry.getBattleModifier()))) {
                log.warn(
                        "War log entry does not match war: warId={}, warLogEntryId={}",
                        war.getId(), entry.getId());
                continue;
            }

            if (entry.getClanAttacks() == null
                    || entry.getClanStars() == null
                    || entry.getClanDestructionPercentage() == null
                    || entry.getOpponentClanStars() == null
                    || entry.getOpponentClanDestructionPercentage() == null) {
                log.warn(
                        "War log entry is incomplete: warId={}, warLogEntryId={}",
                        war.getId(), entry.getId());
                continue;
            }

            war.update(
                    entry.getClanAttacks(),
                    entry.getClanStars(),
                    entry.getClanDestructionPercentage(),
                    entry.getClanLevel() == null
                            ? war.getClanLevel()
                            : entry.getClanLevel(),

                    // В текущей WarLogEntry нет количества атак соперника.
                    war.getOpponentClanAttacks(),
                    entry.getOpponentClanStars(),
                    entry.getOpponentClanDestructionPercentage(),
                    entry.getOpponentClanLevel() == null
                            ? war.getOpponentClanLevel()
                            : entry.getOpponentClanLevel(),

                    "warEnded");

            war.markWarLogSynced(now);
            syncedWars.add(war);
        }

        return warRepository.saveAll(syncedWars);
    }

    private War createWar(@NonNull WarResponse response) {
        return new War(
                response.clan().tag(),
                response.clan().name(),
                response.clan().attacks(),
                response.clan().stars(),
                response.clan().destructionPercentage(),
                response.clan().clanLevel(),

                response.opponent().tag(),
                response.opponent().name(),
                response.opponent().attacks(),
                response.opponent().stars(),
                response.opponent().destructionPercentage(),
                response.opponent().clanLevel(),

                response.state(),
                response.teamSize(),
                response.attacksPerMember(),
                response.battleModifier(),
                Instant.from(WAR_TIME_FORMAT.parse(response.preparationStartTime())),
                Instant.from(WAR_TIME_FORMAT.parse(response.startTime())),
                Instant.from(WAR_TIME_FORMAT.parse(response.endTime())));
    }

    private void updateWar(@NonNull War war, @NonNull WarResponse response) {
        war.update(
                response.clan().attacks(),
                response.clan().stars(),
                response.clan().destructionPercentage(),
                response.clan().clanLevel(),
                response.opponent().attacks(),
                response.opponent().stars(),
                response.opponent().destructionPercentage(),
                response.opponent().clanLevel(),
                response.state());
    }

    private WarClanBadgeUrls toWarClanBadgeUrls(WarClanBadgeUrlsResponse response) {
        return response == null ? null
                : WarClanBadgeUrls.of(response.small(), response.large(), response.medium());
    }
}
