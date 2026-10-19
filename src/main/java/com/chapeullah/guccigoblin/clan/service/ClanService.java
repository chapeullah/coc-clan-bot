package com.chapeullah.guccigoblin.clan.service;

import com.chapeullah.guccigoblin.ClashOfClansClient;
import com.chapeullah.guccigoblin.capitalleague.CapitalLeague;
import com.chapeullah.guccigoblin.capitalleague.CapitalLeagueService;
import com.chapeullah.guccigoblin.clan.repository.ClanRepository;
import com.chapeullah.guccigoblin.clan.dto.ClanBadgeUrlsResponse;
import com.chapeullah.guccigoblin.clan.dto.ClanResponse;
import com.chapeullah.guccigoblin.clan.model.Clan;
import com.chapeullah.guccigoblin.clan.model.ClanBadgeUrls;
import com.chapeullah.guccigoblin.clan.model.ClanChatLanguage;
import com.chapeullah.guccigoblin.clan.model.ClanLabelLink;
import com.chapeullah.guccigoblin.label.clan.ClanLabelService;
import com.chapeullah.guccigoblin.label.dto.LabelResponse;
import com.chapeullah.guccigoblin.leaguetier.LeagueTier;
import com.chapeullah.guccigoblin.leaguetier.LeagueTierService;
import com.chapeullah.guccigoblin.location.Location;
import com.chapeullah.guccigoblin.location.LocationService;
import com.chapeullah.guccigoblin.member.MemberService;
import com.chapeullah.guccigoblin.member.model.Member;
import com.chapeullah.guccigoblin.warleague.WarLeague;
import com.chapeullah.guccigoblin.warleague.WarLeagueService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClanService {

    private final ClashOfClansClient client;

    private final ClanRepository clanRepository;

    private final LocationService locationService;
    private final CapitalLeagueService capitalLeagueService;
    private final WarLeagueService warLeagueService;
    private final LeagueTierService leagueTierService;
    private final ClanLabelService clanLabelService;
    private final ClanLabelLinkService clanLabelLinkService;
    private final ClanChatLanguageService clanChatLanguageService;
    private final ClanCapitalService clanCapitalService;

    private final MemberService memberService;

    @Transactional
    public Clan getOrSyncClanByTag(@NonNull String clanTag) {
        return clanRepository.findById(clanTag)
                .orElseGet(() -> syncClan(clanTag));
    }

    @Transactional(readOnly = true)
    public Clan getClanByTag(@NonNull String clanTag) {
        return clanRepository
                .findById(clanTag)
                .orElseThrow(() -> new IllegalArgumentException("Clan not found: clanTag=\"" + clanTag + "\""));
    }

    @Transactional
    public Clan syncClan(@NonNull String clanTag) {
        Instant observedAt = Instant.now();
        ClanResponse response = client.getClan(clanTag);
        if (response.memberList() == null || response.memberList().isEmpty()) {
            throw new IllegalStateException("Clan member list is missing or empty: clanTag=\"" + clanTag + "\"");
        }
        if (response.memberCount() == null
                || response.memberCount() != response.memberList().size()) {
            throw new IllegalStateException("Clan member count mismatch: clanTag=\"" + clanTag
                    + "\", expectedCount=" + response.memberCount()+ ", actualCount="
                    + response.memberList().size());
        }

        Set<String> memberTags = new HashSet<>();
        for (var member : response.memberList()) {
            if (member == null
                    || member.tag() == null
                    || member.tag().isBlank()
                    || !memberTags.add(member.tag())) {
                throw new IllegalStateException("Invalid or duplicate clan member tag: clanTag=\""
                        + clanTag + "\"");
            }
        }

        syncLocationsIfMissing(response);
        syncCapitalLeaguesIfMissing(response);
        syncWarLeaguesIfMissing(response);
        syncClanLabelsIfMissing(response);
        syncLeagueTiersIfMissing(response);

        Clan clan = clanRepository.findById(response.tag()).orElse(null);
        if (clan == null) {
            clan = createClan(response, observedAt);
        } else {
            updateClan(clan, response, observedAt);
        }

        clan.setClanCapital(clanCapitalService.syncAndGetByClan(clan, response.clanCapital()));

        return clanRepository.save(clan);
    }

    private void syncLocationsIfMissing(
            @NonNull ClanResponse response) {
        if (response.location() == null) {
            log.debug("Clan location is missing: clanTag=\"{}\"", response.tag());
            return;
        }
        Integer locationId = response.location().id();
        if (locationService.findById(locationId).isEmpty()) {
            log.info("Location is missing locally; synchronizing locations: locationId={}", locationId);
            locationService.syncAll();
        }
    }

    private void syncCapitalLeaguesIfMissing(
            @NonNull ClanResponse response) {
        if (response.capitalLeague() == null) {
            log.debug("Clan capital league is missing in response: clanTag=\"{}\"", response.tag());
            return;
        }
        Integer capitalLeagueId = response.capitalLeague().id();
        if (capitalLeagueService.findById(capitalLeagueId).isEmpty()) {
            log.info("Capital league is missing locally; synchronizing capital leagues: capitalLeagueId={}",
                    capitalLeagueId);
            capitalLeagueService.syncAll();
        }
    }

    private void syncWarLeaguesIfMissing(
            @NonNull ClanResponse response) {
        if (response.warLeague() == null) {
            log.debug("Clan war league is missing: clanTag=\"{}\"", response.tag());
            return;
        }
        Integer warLeagueId = response.warLeague().id();
        if (warLeagueService.findById(warLeagueId).isEmpty()) {
            log.info("War league is missing locally; synchronizing war leagues: warLeagueId={}", warLeagueId);
            warLeagueService.syncAll();
        }
    }

    private void syncClanLabelsIfMissing(@NonNull ClanResponse clanResponse) {
        List<LabelResponse> labels = clanResponse.labels();
        if (labels == null) {
            log.debug("Clan labels are missing: clanTag=\"{}\"", clanResponse.tag());
            return;

        }
        for (LabelResponse labelResponse : labels) {
            if (labelResponse == null) {
                log.debug("Clan label is missing: clanTag=\"{}\"", clanResponse.tag());
                continue;
            }
            Integer clanLabelId = labelResponse.id();
            if (clanLabelService.findById(clanLabelId).isEmpty()) {
                log.info("Clan label is missing locally; synchronizing clan labels: clanLabelId={}", clanLabelId);
                clanLabelService.syncAll();
            }
        }
    }

    private void syncLeagueTiersIfMissing(
            @NonNull ClanResponse response) {
        if (response.requiredLeagueTier() == null) {
            log.debug("Clan required league tier is missing: clanTag=\"{}\"", response.tag());
            return;
        }
        Integer leagueTierId = response.requiredLeagueTier().id();
        if (leagueTierService.findById(leagueTierId).isEmpty()) {
            log.info("League tier is missing locally; synchronizing league tiers: leagueTierId={}", leagueTierId);
            leagueTierService.syncAll();
        }
    }

    public Clan createClan(@NonNull ClanResponse response, @NonNull Instant observedAt) {
        Location location = response.location() == null ? null
                : locationService.getById(response.location().id());

        CapitalLeague capitalLeague = response.capitalLeague() == null ? null
                : capitalLeagueService.getById(response.capitalLeague().id());

        WarLeague warLeague = response.warLeague() == null ? null
                : warLeagueService.getById(response.warLeague().id());

        LeagueTier requiredLeagueTier = response.requiredLeagueTier() == null ? null
                : leagueTierService.getById(response.requiredLeagueTier().id());

        ClanChatLanguage chatLanguage = response.chatLanguage() == null ? null
                : clanChatLanguageService.getOrCreate(response.chatLanguage());

        Clan clan = new Clan(
                response.tag(),
                response.name(),
                response.type(),
                response.description(),
                location,
                response.isFamilyFriendly(),
                response.clanLevel(),
                response.clanPoints(),
                response.clanBuilderBasePoints(),
                response.clanCapitalPoints(),
                capitalLeague,
                response.requiredTrophies(),
                response.warFrequency(),
                response.warWinStreak(),
                response.warWins(),
                response.warTies(),
                response.warLosses(),
                response.isWarLogPublic(),
                warLeague,
                response.memberCount(),
                response.requiredBuilderBaseTrophies(),
                response.requiredTownHallLevel(),
                requiredLeagueTier,
                chatLanguage);

        clan.setClanBadgeUrls(toClanBadgeUrls(response.badgeUrls()));
        clan = clanRepository.save(clan);

        memberService.syncMembers(clan, response.memberList(), observedAt);

        clanLabelLinkService.syncAndGetByClan(clan, response.labels());

        return clan;
    }

    public void updateClan(
            @NonNull Clan clan,
            @NonNull ClanResponse response,
            @NonNull Instant observedAt) {
        if (!clan.getTag().equals(response.tag())) {
            throw new IllegalArgumentException("Clan tag mismatch");
        }

        Instant now = Instant.now();

        // type
        // typeChangedAt
        if (!Objects.equals(clan.getType(), response.type())) {
            clan.setType(response.type());
            clan.setTypeChangedAt(now);
        }

        // description
        // descriptionChangedAt
        if (!Objects.equals(clan.getDescription(), response.description())) {
            clan.setDescription(response.description());
            clan.setDescriptionChangedAt(now);
        }

        // location
        // locationChangedAt
        Location location = response.location() == null ? null
                : locationService.getById(response.location().id());
        if (!Objects.equals(clan.getLocation(), location)) {
            clan.setLocation(location);
            clan.setLocationChangedAt(now);
        }

        // clanBadgeUrls
        // clanBadgeUrlsChangedAt
        ClanBadgeUrls newClanBadgeUrls = toClanBadgeUrls(response.badgeUrls());
        if (!Objects.equals(clan.getClanBadgeUrls(), newClanBadgeUrls)) {
            clan.setClanBadgeUrls(newClanBadgeUrls);
            clan.setClanBadgeUrlsChangedAt(now);
        }

        // clanLevel
        // clanLevelChangedAt
        if (!Objects.equals(clan.getClanLevel(), response.clanLevel())) {
            clan.setClanLevel(response.clanLevel());
            clan.setClanLevelChangedAt(now);
        }

        // clanPoints
        // clanPointsChanged
        if (!Objects.equals(clan.getClanPoints(), response.clanPoints())) {
            clan.setClanPoints(response.clanPoints());
            clan.setClanPointsChangedAt(now);
        }

        // clanBuilderBasePoints
        // clanBuilderBasePointsChangedAt
        if (!Objects.equals(clan.getClanBuilderBasePoints(), response.clanBuilderBasePoints())) {
            clan.setClanBuilderBasePoints(response.clanBuilderBasePoints());
            clan.setClanBuilderBasePointsChangedAt(now);
        }

        // clanCapitalPoints
        // clanCapitalPointsChangedAt
        if (!Objects.equals(clan.getClanCapitalPoints(), response.clanCapitalPoints())) {
            clan.setClanCapitalPoints(response.clanCapitalPoints());
            clan.setClanCapitalPointsChangedAt(now);
        }

        // capitalLeague
        // capitalLeagueChangedAt
        CapitalLeague capitalLeague = response.capitalLeague() == null ? null :
                capitalLeagueService.getById(response.capitalLeague().id());
        if (!Objects.equals(clan.getCapitalLeague(), capitalLeague)) {
            clan.setCapitalLeague(capitalLeague);
            clan.setCapitalLeagueChangedAt(now);
        }

        // requiredTrophies
        // requiredTrophiesChangedAt
        if (!Objects.equals(clan.getRequiredTrophies(), response.requiredTrophies())) {
            clan.setRequiredTrophies(response.requiredTrophies());
            clan.setRequiredTrophiesChangedAt(now);
        }

        // warFrequency
        // warFrequencyChangedAt
        if (!Objects.equals(clan.getWarFrequency(), response.warFrequency())) {
            clan.setWarFrequency(response.warFrequency());
            clan.setWarFrequencyChangedAt(now);
        }

        // warWinStreak
        // warWinStreakChangedAt
        if (!Objects.equals(clan.getWarWinStreak(), response.warWinStreak())) {
            clan.setWarWinStreak(response.warWinStreak());
            clan.setWarWinStreakChangedAt(now);
        }

        // warWins
        // warWinsChangedAt
        if (!Objects.equals(clan.getWarWins(), response.warWins())) {
            clan.setWarWins(response.warWins());
            clan.setWarWinsChangedAt(now);
        }

        // warTies
        // warTiesChangedAt
        if (!Objects.equals(clan.getWarTies(), response.warTies())) {
            clan.setWarTies(response.warTies());
            clan.setWarTiesChangedAt(now);
        }

        // warLosses
        // warLossesChangedAt
        if (!Objects.equals(clan.getWarLosses(), response.warLosses())) {
            clan.setWarLosses(response.warLosses());
            clan.setWarLossesChangedAt(now);
        }

        // isWarLogPublic
        // isWarLogPublicChangedAt
        if (!Objects.equals(clan.getIsWarLogPublic(), response.isWarLogPublic())) {
            clan.setIsWarLogPublic(response.isWarLogPublic());
            clan.setIsWarLogPublicChangedAt(now);
        }

        // warLeague
        // warLeagueChangedAt
        WarLeague warLeague = response.warLeague() == null ? null
                : warLeagueService.getById(response.warLeague().id());
        if (!Objects.equals(clan.getWarLeague(), warLeague)) {
            clan.setWarLeague(warLeague);
            clan.setWarLeagueChangedAt(now);
        }

        // memberCount
        // memberCountChangedAt
        if (!Objects.equals(clan.getMemberCount(), response.memberCount())) {
            clan.setMemberCount(response.memberCount());
            clan.setMemberCountChangedAt(now);
        }

        // memberList
        // memberListChangedAt
        Set<String> memberSetBefore = memberService
                .getCurrentMembersByClanTag(clan.getTag())
                .stream()
                .map(Member::getTag)
                .collect(Collectors.toSet());
        Set<String> memberSetAfter = memberService
                .syncMembers(clan, response.memberList(), observedAt)
                .stream()
                .map(Member::getTag)
                .collect(Collectors.toSet());
        if (!Objects.equals(memberSetBefore, memberSetAfter)) {
            clan.setMemberListChangedAt(now);
        }

        // labelLinks
        // labelLinksChangedAt
        Set<ClanLabelLink> clanLabelLinkSetBefore =
                new HashSet<>(clanLabelLinkService
                        .getAllByClanTag(clan.getTag()));
        Set<ClanLabelLink> clanLabelLinkSetAfter =
                new HashSet<>(clanLabelLinkService
                        .syncAndGetByClan(clan, response.labels()));
        if (!Objects.equals(clanLabelLinkSetBefore, clanLabelLinkSetAfter)) {
            clan.setLabelLinksChangedAt(now);
        }

        // requiredBuilderBaseTrophies
        // requiredBuilderBaseTrophiesChangedAt
        if (!Objects.equals(
                clan.getRequiredBuilderBaseTrophies(),
                response.requiredBuilderBaseTrophies())) {
            clan.setRequiredBuilderBaseTrophies(
                    response.requiredBuilderBaseTrophies());
            clan.setRequiredBuilderBaseTrophiesChangedAt(now);
        }

        // requiredTownHallLevel
        // requiredTownHallLevelChangedAt
        if (!Objects.equals(clan.getRequiredTownHallLevel(), response.requiredTownHallLevel())) {
            clan.setRequiredTownHallLevel(response.requiredTownHallLevel());
            clan.setRequiredTownHallLevelChangedAt(now);
        }

        // requiredLeagueTier
        // requiredLeagueTierChangedAt
        LeagueTier requiredLeagueTier = response.requiredLeagueTier() == null ? null
                : leagueTierService.getById(response.requiredLeagueTier().id());
        if (!Objects.equals(clan.getRequiredLeagueTier(), requiredLeagueTier)) {
            clan.setRequiredLeagueTier(requiredLeagueTier);
            clan.setRequiredLeagueTierChangedAt(now);
        }

        // clanChatLanguage
        // clanChatLanguageChangedAt
        ClanChatLanguage clanChatLanguage = response.chatLanguage() == null
                ? null
                : clanChatLanguageService.getOrCreate(response.chatLanguage());
        if (!Objects.equals(clan.getClanChatLanguage(), clanChatLanguage)) {
            clan.setClanChatLanguage(clanChatLanguage);
            clan.setClanChatLanguageChangedAt(now);
        }
    }

    private ClanBadgeUrls toClanBadgeUrls(ClanBadgeUrlsResponse response) {
        return response == null ? null
                : ClanBadgeUrls.of(response.small(), response.large(), response.medium());
    }
}
