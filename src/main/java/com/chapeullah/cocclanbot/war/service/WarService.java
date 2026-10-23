package com.chapeullah.cocclanbot.war.service;

import com.chapeullah.cocclanbot.ClashOfClansClient;
import com.chapeullah.cocclanbot.war.dto.WarClanBadgeUrlsResponse;
import com.chapeullah.cocclanbot.war.dto.WarResponse;
import com.chapeullah.cocclanbot.war.model.War;
import com.chapeullah.cocclanbot.war.model.WarClanBadgeUrls;
import com.chapeullah.cocclanbot.war.model.WarLogEntry;
import com.chapeullah.cocclanbot.war.repository.WarRepository;
import jakarta.transaction.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
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
    public Slice<War> getCompletedByClanTagAndMemberTag(
            @NonNull String clanTag,
            @NonNull String memberTag,
            int pageNumber,
            int pageSize) {

        return warRepository
                .findDistinctByClanTagAndStateAndWarLogSyncedAtIsNotNullAndParticipantsPlayerTagAndParticipants_ClanTag(
                        clanTag,
                        "warEnded",
                        memberTag,
                        clanTag,
                        PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "endTime", "id")));
    }

    @Transactional
    public List<War> getRecentCompletedByClanTag(@NonNull String clanTag, int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be greater than zero");
        }
        return warRepository.findByClanTagAndStateOrderByEndTimeDesc(clanTag, "warEnded", Limit.of(limit));
    }

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

        List<War> matchingWars = warRepository
                .findAllByClanTagAndOpponentClanTagAndPreparationStartTimeOrderByIdAsc(
                        response.clan().tag(),
                        response.opponent().tag(),
                        Instant.from(WAR_TIME_FORMAT.parse(response.preparationStartTime())));
        if (matchingWars.size() > 1) {
            throw new IllegalStateException("Duplicate war identity requires data repair: warIds="
                    + matchingWars.stream().map(War::getId).toList());
        }
        War war = matchingWars.isEmpty() ? null : matchingWars.getFirst();

        WarClanBadgeUrls warClanBadgeUrls = toWarClanBadgeUrls(response.clan().badgeUrls());
        if (war == null) {
            war = createWar(response);
            war.setWarClanBadgeUrls(warClanBadgeUrls);
        } else {
            if (war.getWarLogSyncedAt() == null) {
                Instant previousStartTime = war.getStartTime();
                Instant previousEndTime = war.getEndTime();
                if (war.updateTimes(
                        Instant.from(WAR_TIME_FORMAT.parse(response.startTime())),
                        Instant.from(WAR_TIME_FORMAT.parse(response.endTime())))) {
                    log.info("War schedule changed: warId={}, preparationStartTime={}, "
                                    + "previousStartTime={}, startTime={}, previousEndTime={}, endTime={}",
                            war.getId(), war.getPreparationStartTime(),
                            previousStartTime, war.getStartTime(), previousEndTime, war.getEndTime());
                }
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
            if (warRepository.findAllByClanTagAndOpponentClanTagAndPreparationStartTimeOrderByIdAsc(
                    war.getClanTag(), war.getOpponentClanTag(), war.getPreparationStartTime()).size() != 1) {
                log.warn("Duplicate war identity requires data repair: warId={}, preparationStartTime={}",
                        war.getId(), war.getPreparationStartTime());
                continue;
            }
            WarLogEntry entry = findFinalWarLogEntry(war, now).orElse(null);

            if (entry == null) {
                continue;
            }

            if (!matchesWarMetadata(war, entry)) {
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

            if (warRepository.existsByClanTagAndEndTimeAndWarLogSyncedAtIsNotNull(
                    war.getClanTag(), entry.getEndTime())) {
                log.warn("War log entry already belongs to a confirmed war: warId={}, warLogEntryId={}",
                        war.getId(), entry.getId());
                continue;
            }

            Instant previousEndTime = war.getEndTime();
            if (war.updateTimes(war.getStartTime(), entry.getEndTime())) {
                log.info("War end time confirmed from war log: warId={}, warLogEntryId={}, "
                                + "previousEndTime={}, endTime={}",
                        war.getId(), entry.getId(), previousEndTime, war.getEndTime());
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

    private Optional<WarLogEntry> findFinalWarLogEntry(War war, Instant now) {
        Optional<WarLogEntry> exact = warLogService.findByClanTagAndEndTime(
                war.getClanTag(), war.getEndTime());
        if (exact.isPresent()) {
            return exact;
        }

        Optional<Instant> nextPreparation = warRepository
                .findFirstByClanTagAndPreparationStartTimeGreaterThanOrderByPreparationStartTimeAsc(
                        war.getClanTag(), war.getPreparationStartTime())
                .map(War::getPreparationStartTime)
                .filter(preparation -> !preparation.isAfter(now));
        if (nextPreparation.isEmpty()) {
            log.debug("Shifted war log match awaits the next preparation boundary: warId={}, endTime={}",
                    war.getId(), war.getEndTime());
            return Optional.empty();
        }
        Instant through = nextPreparation.orElseThrow();
        List<WarLogEntry> candidates = warLogService.findByClanAndOpponentAndEndTimeRange(
                        war.getClanTag(), war.getOpponentClanTag(), war.getStartTime(), through)
                .stream()
                .filter(entry -> matchesWarMetadata(war, entry))
                .filter(entry -> !warRepository.existsByClanTagAndEndTimeAndWarLogSyncedAtIsNotNull(
                        war.getClanTag(), entry.getEndTime()))
                .toList();
        if (candidates.size() == 1) {
            return Optional.of(candidates.getFirst());
        }
        if (candidates.size() > 1) {
            log.warn("Ambiguous war log match: warId={}, after={}, through={}, warLogEntryIds={}",
                    war.getId(), war.getStartTime(), through,
                    candidates.stream().map(WarLogEntry::getId).toList());
        } else {
            log.debug("Final war log entry not found: warId={}, endTime={}", war.getId(), war.getEndTime());
        }
        return Optional.empty();
    }

    private static boolean matchesWarMetadata(War war, WarLogEntry entry) {
        return Objects.equals(war.getClanTag(), entry.getClanTag())
                && Objects.equals(war.getOpponentClanTag(), entry.getOpponentClanTag())
                && Objects.equals(war.getTeamSize(), entry.getTeamSize())
                && (entry.getAttacksPerMember() == null
                || Objects.equals(war.getAttacksPerMember(), entry.getAttacksPerMember()))
                && (entry.getBattleModifier() == null
                || Objects.equals(war.getBattleModifier(), entry.getBattleModifier()));
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
