package com.chapeullah.guccigoblin.war.service;

import com.chapeullah.guccigoblin.ClashOfClansClient;
import com.chapeullah.guccigoblin.war.dto.WarLogClanBadgeUrlsResponse;
import com.chapeullah.guccigoblin.war.dto.WarLogClanResponse;
import com.chapeullah.guccigoblin.war.dto.WarLogEntryResponse;
import com.chapeullah.guccigoblin.war.dto.WarLogResponse;
import com.chapeullah.guccigoblin.war.model.WarLogEntry;
import com.chapeullah.guccigoblin.war.model.WarLogEntryClanBadgeUrls;
import com.chapeullah.guccigoblin.war.repository.WarLogRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WarLogService {

    private static final DateTimeFormatter WAR_TIME_FORMAT = DateTimeFormatter.ofPattern("uuuuMMdd'T'HHmmss.SSSX");

    private final ClashOfClansClient client;
    private final WarLogRepository warLogRepository;

    @Transactional(readOnly = true)
    public Optional<WarLogEntry> findByClanTagAndEndTime(
            @NonNull String clanTag,
            @NonNull Instant endTime) {
        return warLogRepository.findByClanTagAndEndTime(clanTag, endTime);
    }

    @Transactional
    public WarLogEntry getOrSyncWarLogEntry(
            @NonNull String clanTag,
            @NonNull Instant endTime) {
        WarLogEntry warLogEntry = warLogRepository
                .findByClanTagAndEndTime(clanTag, endTime)
                .orElse(null);
        if (warLogEntry != null) {
            return warLogEntry;
        }
        syncWarLog(clanTag);
        return warLogRepository
                .findByClanTagAndEndTime(clanTag, endTime)
                .orElseThrow(() -> new IllegalStateException(
                        "War log entry not found: clanTag=\"" + clanTag
                                + "\", endTime=" + endTime));
    }

    @Transactional
    public List<WarLogEntry> syncWarLog(@NonNull String clanTag) {
        WarLogResponse response = client.getWarLog(clanTag);
        if (response.items() == null) {
            throw new IllegalStateException("War log items are missing: clanTag=\"" + clanTag + "\"");
        }

        List<WarLogEntry> changedWarLogEntries = new ArrayList<>();

        for (WarLogEntryResponse entryResponse : response.items()) {
            WarLogEntry incoming = toWarLogEntry(entryResponse);
            WarLogEntry existing = warLogRepository
                    .findByClanTagAndEndTime(
                            incoming.getClanTag(),
                            incoming.getEndTime())
                    .orElse(null);
            if (existing == null) {
                changedWarLogEntries.add(incoming);
            } else if (existing.updateFrom(incoming)) {
                changedWarLogEntries.add(existing);
            }
        }

        return warLogRepository.saveAll(changedWarLogEntries);
    }

    private WarLogEntry toWarLogEntry(@NonNull WarLogEntryResponse response) {
        WarLogClanResponse clan = response.clan();
        WarLogClanResponse opponent = response.opponent();

        if (clan == null || clan.tag() == null || response.endTime() == null) {
            throw new IllegalStateException("War log entry is missing clan tag or end time");
        }

        WarLogClanBadgeUrlsResponse clanBadgeUrls = clan.badgeUrls();
        WarLogClanBadgeUrlsResponse opponentBadgeUrls =
                opponent == null ? null : opponent.badgeUrls();

        return new WarLogEntry(
                response.result(),
                Instant.from(WAR_TIME_FORMAT.parse(response.endTime())),
                response.teamSize(),
                response.attacksPerMember(),
                response.battleModifier(),

                clan.tag(),
                clan.name(),
                clan.clanLevel(),
                clan.attacks(),
                clan.stars(),
                clan.destructionPercentage(),
                clan.expEarned(),
                toClanBadgeUrls(clanBadgeUrls),

                opponent == null ? null : opponent.tag(),
                opponent == null ? null : opponent.name(),
                opponent == null ? null : opponent.clanLevel(),
                opponent == null ? null : opponent.stars(),
                opponent == null ? null : opponent.destructionPercentage(),
                toClanBadgeUrls(opponentBadgeUrls));
    }

    private WarLogEntryClanBadgeUrls toClanBadgeUrls(WarLogClanBadgeUrlsResponse response) {
        return response == null ? null
                : WarLogEntryClanBadgeUrls.of(response.small(), response.large(), response.medium());
    }

}