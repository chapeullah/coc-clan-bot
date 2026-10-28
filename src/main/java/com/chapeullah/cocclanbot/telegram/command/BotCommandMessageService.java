package com.chapeullah.cocclanbot.telegram.command;

import com.chapeullah.cocclanbot.clan.model.Clan;
import com.chapeullah.cocclanbot.clan.service.ClanService;
import com.chapeullah.cocclanbot.member.MemberService;
import com.chapeullah.cocclanbot.member.model.Member;
import com.chapeullah.cocclanbot.raidseason.model.RaidSeason;
import com.chapeullah.cocclanbot.raidseason.model.RaidSeasonClanMemberSnapshot;
import com.chapeullah.cocclanbot.raidseason.model.RaidSeasonParticipant;
import com.chapeullah.cocclanbot.raidseason.service.RaidSeasonClanMemberSnapshotService;
import com.chapeullah.cocclanbot.raidseason.service.RaidSeasonParticipantService;
import com.chapeullah.cocclanbot.raidseason.service.RaidSeasonService;
import com.chapeullah.cocclanbot.rating.MemberRaidStatistic;
import com.chapeullah.cocclanbot.rating.MemberStatistic;
import com.chapeullah.cocclanbot.rating.MemberWarStatistic;
import com.chapeullah.cocclanbot.rating.RatingService;
import com.chapeullah.cocclanbot.rating.WarRatingService;
import com.chapeullah.cocclanbot.telegram.TelegramAccessService;
import com.chapeullah.cocclanbot.telegram.dto.TelegramMessage;
import com.chapeullah.cocclanbot.telegram.groupchatid.TelegramGroupChatIdService;
import com.chapeullah.cocclanbot.telegram.groupchatthreads.TelegramGroupChatThreadsService;
import com.chapeullah.cocclanbot.war.model.War;
import com.chapeullah.cocclanbot.war.model.WarAttack;
import com.chapeullah.cocclanbot.war.model.WarParticipant;
import com.chapeullah.cocclanbot.war.service.WarAttackService;
import com.chapeullah.cocclanbot.war.service.WarParticipantService;
import com.chapeullah.cocclanbot.war.service.WarService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.chapeullah.cocclanbot.telegram.command.BotMessages.*;

@Service
@RequiredArgsConstructor
public class BotCommandMessageService {

    private final ClanService clanService;
    private final MemberService memberService;
    private final WarService warService;
    private final WarParticipantService warParticipantService;
    private final WarAttackService warAttackService;
    private final RaidSeasonService raidSeasonService;
    private final RaidSeasonParticipantService raidSeasonParticipantService;
    private final RaidSeasonClanMemberSnapshotService raidSeasonClanMemberSnapshotService;
    private final RatingService ratingService;
    private final WarRatingService warRatingService;
    private final TelegramGroupChatThreadsService telegramGroupChatThreadsService;
    private final TelegramGroupChatIdService telegramGroupChatIdService;
    private final TelegramAccessService telegramAccessService;
    private final ObjectProvider<BotCommandService> botCommandService;

    @Transactional(readOnly = true)
    public String getClanMessage(@NonNull String clanTag) {
        Clan clan = clanService.getClanByTag(clanTag);
        Integer wins = clan.getWarWins();
        Integer losses = clan.getWarLosses();
        Double warWinRate = null;
        if (wins != null && losses != null) {
            long totalWars = (long) wins + losses;
            if (totalWars != 0) {
                warWinRate = wins * 100.0 / totalWars;
            }
        }
        return BotMessages.formatClanMessage(
                clan.getName(), clan.getTag(), clan.getDescription(),
                clan.getClanLevel(), clan.getMemberCount(), clan.getType(),
                clan.getClanPoints(), clan.getClanCapitalPoints(), clan.getWarLeague(),
                warWinRate, wins, clan.getWarWinStreak(), clan.getLocation());
    }

    @Transactional(readOnly = true)
    public String getHelpMessage(@NonNull TelegramMessage message) {
        Long userId = getUserId(message);
        if (userId == null) {
            throw new IllegalStateException("Command sender is missing: chatId=" + message.telegramChat().id()
                    + ", messageId=" + message.messageId());
        }
        boolean isAdmin = telegramAccessService.isAdmin(userId);
        List<List<BotCommand>> visibleGroups = botCommandService.getObject().getCommandsByType().values().stream()
                .map(commands -> commands.stream()
                        .filter(command -> isAdmin || !command.isAdminOnly())
                        .toList())
                .filter(group -> !group.isEmpty())
                .toList();
        return BotMessages.formatHelpMessage(visibleGroups, "private".equals(message.telegramChat().type()));
    }

    @Transactional(readOnly = true)
    public String getMemberMessage(@NonNull String clanTag, String args) {
        if (args == null || args.isBlank()) {
            return MEMBER_TAG_REQUIRED;
        }
        String tag = args.strip().toUpperCase(Locale.ROOT);
        if (!tag.startsWith("#")) {
            tag = "#" + tag;
        }
        if (!tag.matches("#[A-Z0-9]+")) {
            return INVALID_MEMBER_TAG_FORMAT;
        }
        Member member = memberService.findByTagAndClanTag(tag, clanTag).orElse(null);
        if (member == null) {
            return MEMBER_NOT_FOUND;
        }
        return BotMessages.formatMemberMessage(
                member.getName(), member.getTag(), member.getRole(), member.getInClan(),
                member.getClanRank(), member.getTownHallLevel(), member.getExpLevel(), member.getLeagueTier(),
                member.getTrophies(), member.getBuilderBaseTrophies(), member.getDonations(),
                member.getDonationsReceived(), member.getTotalDonations(), member.getTotalDonationsReceived());
    }

    @Transactional(readOnly = true)
    public String getMembersMessage(@NonNull String clanTag) {
        List<Member> members = memberService.getCurrentMembersByClanTag(clanTag);
        if (members.isEmpty()) {
            return NO_MEMBERS;
        }
        Map<String, MemberStatistic> statisticsByTag = ratingService.calculateClanMemberStatistics(clanTag);
        List<String> memberLines = members.stream()
                .map(member -> {
                    MemberStatistic statistic = statisticsByTag.get(member.getTag());
                    Double rating = statistic == null ? null : statistic.totalRating().orElse(null);
                    return BotMessages.formatMemberListItem(
                            member.getClanRank(), member.getName(), member.getTag(), rating);
                })
                .toList();
        return BotMessages.formatMembersMessage(memberLines);
    }

    @Transactional(readOnly = true)
    public String getRaidMessage(@NonNull String clanTag) {
        RaidSeason raidSeason = raidSeasonService.findLatestByClanTag(clanTag).orElse(null);
        if (raidSeason == null) {
            return NO_RAID_SEASONS;
        }
        List<RaidSeasonParticipant> participants = raidSeasonParticipantService.getByRaidSeason(raidSeason);
        Long participantCount = isRaidAttackCountConsistent(raidSeason, participants)
                ? participants.stream().filter(participant -> participant.getAttackCount() > 0).count()
                : null;
        return BotMessages.formatRaidMessage(
                raidSeason.getState(), raidSeason.getStartTime(), raidSeason.getEndTime(),
                raidSeason.getCapitalTotalLoot(), raidSeason.getTotalAttacks(), participantCount,
                raidSeason.getRaidsCompleted(), raidSeason.getEnemyDistrictsDestroyed());
    }

    @Transactional(readOnly = true)
    public String getRaidMissedMessage(@NonNull String clanTag) {
        RaidSeason raidSeason = raidSeasonService.findLatestByClanTag(clanTag).orElse(null);
        if (raidSeason == null) {
            return RAID_NOT_FOUND;
        }
        List<RaidSeasonParticipant> participants = raidSeasonParticipantService.getByRaidSeason(raidSeason);
        if (!isRaidAttackCountConsistent(raidSeason, participants)) {
            return RAID_ATTACK_DATA_INCONSISTENT;
        }
        List<RaidSeasonClanMemberSnapshot> members = raidSeasonClanMemberSnapshotService.getByRaidSeason(raidSeason);
        if (members.isEmpty()) {
            return RAID_SNAPSHOT_NOT_FOUND;
        }
        Set<String> attackedTags = new HashSet<>();
        for (RaidSeasonParticipant participant : participants) {
            if (participant.getAttackCount() > 0) {
                attackedTags.add(participant.getTag());
            }
        }
        List<RaidSeasonClanMemberSnapshot> membersWithoutAttacks = members.stream()
                .filter(member -> !attackedTags.contains(member.getTag()))
                .filter(member -> !Boolean.FALSE.equals(member.getParticipationEligible()))
                .toList();
        return BotMessages.formatRaidMissedMessage(
                raidSeason.getState(), raidSeason.getStartTime(), raidSeason.getEndTime(), membersWithoutAttacks);
    }

    @Transactional(readOnly = true)
    public String getRaidsMessage(@NonNull String clanTag) {
        List<RaidSeason> raidSeasons = raidSeasonService.getRecentByClanTag(clanTag);
        if (raidSeasons.isEmpty()) {
            return RAID_NOT_FOUND;
        }
        return BotMessages.formatRaidsMessage(raidSeasons);
    }

    @Transactional(readOnly = true)
    public String getRatingMessage(@NonNull String clanTag) {
        List<MemberStatistic> statistics = ratingService.calculateClanMemberStatistics(clanTag).values().stream()
                .sorted(Comparator
                        .comparingDouble((MemberStatistic statistic) ->
                                statistic.totalRating().orElse(Double.NEGATIVE_INFINITY))
                        .reversed()
                        .thenComparing(MemberStatistic::name, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(MemberStatistic::tag))
                .toList();
        if (statistics.isEmpty()) {
            return NO_MEMBERS;
        }
        List<String> lines = new ArrayList<>();
        for (MemberStatistic statistic : statistics) {
            lines.add(BotMessages.formatRatingListItem(
                    lines.size() + 1, statistic.name(), statistic.totalRating().orElse(null)));
        }
        return BotMessages.formatRatingMessage(lines);
    }

    @Transactional(readOnly = true)
    public String getWarRatingMessage(@NonNull String clanTag) {
        List<MemberWarStatistic> statistics = warRatingService.getMemberWarStatistics(clanTag);
        if (statistics.isEmpty()) {
            return NO_MEMBERS;
        }
        List<String> lines = new ArrayList<>();
        for (MemberWarStatistic statistic : statistics) {
            Double averageAttackCount = statistic.warCount() > 0
                    ? (double) statistic.warAttackCount() / statistic.warCount()
                    : null;
            lines.add(BotMessages.formatWarRatingListItem(
                    lines.size() + 1, statistic.name(), statistic.warRating().orElse(null),
                    statistic.averageStarsPerAttack().orElse(null),
                    statistic.averageDestructionPercentagePerAttack().orElse(null), averageAttackCount));
        }
        return BotMessages.formatWarRatingMessage(lines);
    }

    @Transactional(readOnly = true)
    public String getRaidRatingMessage(@NonNull String clanTag) {
        List<MemberRaidStatistic> statistics = ratingService.calculateClanMemberRaidStatistics(clanTag).values().stream()
                .sorted(Comparator
                        .comparingDouble((MemberRaidStatistic statistic) ->
                                statistic.raidRating().orElse(Double.NEGATIVE_INFINITY))
                        .reversed()
                        .thenComparing(MemberRaidStatistic::name, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(MemberRaidStatistic::tag))
                .toList();
        if (statistics.isEmpty()) {
            return NO_MEMBERS;
        }
        List<String> lines = new ArrayList<>();
        for (MemberRaidStatistic statistic : statistics) {
            lines.add(BotMessages.formatRaidRatingListItem(
                    lines.size() + 1, statistic.name(), statistic.raidRating().orElse(null),
                    statistic.averageRaidLootPerAttack().orElse(null),
                    statistic.averageRaidAttacksPerSeason().orElse(null)));
        }
        return BotMessages.formatRaidRatingMessage(lines);
    }

    @Transactional(readOnly = true)
    public String getCurrentWarMessage(@NonNull String clanTag) {
        War war = warService.findCurrentByClanTag(clanTag).orElse(null);
        if (war == null) {
            return NO_ACTIVE_WAR;
        }
        int maximumAttacks = war.getTeamSize() * war.getAttacksPerMember();
        return BotMessages.formatCurrentWarMessage(war, maximumAttacks);
    }

    @Transactional(readOnly = true)
    public String getWarsMessage(@NonNull String clanTag) {
        List<War> wars = warService.getRecentCompletedByClanTag(clanTag);
        if (wars.isEmpty()) {
            return NO_COMPLETED_WARS;
        }
        List<String> lines = wars.stream()
                .map(war -> BotMessages.formatWarHistoryItem(war, getWarResult(war)))
                .toList();
        return BotMessages.formatWarsMessage(lines);
    }

    private static String getWarResult(War war) {
        if (war.getWarLogSyncedAt() == null) {
            return DATA_PENDING;
        }
        int starsComparison = Integer.compare(war.getClanStars(), war.getOpponentClanStars());
        if (starsComparison > 0) {
            return WAR_VICTORY;
        }
        if (starsComparison < 0) {
            return WAR_DEFEAT;
        }
        int destructionComparison = Double.compare(
                war.getClanDestructionPercentage(), war.getOpponentClanDestructionPercentage());
        if (destructionComparison > 0) {
            return WAR_VICTORY;
        }
        if (destructionComparison < 0) {
            return WAR_DEFEAT;
        }
        return WAR_DRAW;
    }

    @Transactional(readOnly = true)
    public String getAttacksMessage(@NonNull String clanTag) {
        War war = warService.findCurrentByClanTag(clanTag).orElse(null);
        if (war == null) {
            return NO_ACTIVE_WAR;
        }
        if ("preparation".equals(war.getState())) {
            return WAR_NOT_STARTED;
        }
        List<WarParticipant> participants = warParticipantService.getByWar(war).stream()
                .filter(participant -> clanTag.equals(participant.getClanTag()))
                .sorted(Comparator.comparing(WarParticipant::getMapPosition))
                .toList();
        Map<String, Integer> usedAttacks = new HashMap<>();
        for (WarAttack attack : warAttackService.getByWar(war)) {
            usedAttacks.merge(attack.getAttackerTag(), 1, Integer::sum);
        }
        List<String> lines = new ArrayList<>();
        for (WarParticipant participant : participants) {
            int remaining = Math.max(0, war.getAttacksPerMember()
                    - usedAttacks.getOrDefault(participant.getPlayerTag(), 0));
            if (remaining > 0) {
                lines.add(BotMessages.formatUnusedAttackListItem(
                        participant.getMapPosition(), participant.getPlayerName(), remaining, war.getAttacksPerMember()));
            }
        }
        return lines.isEmpty() ? ALL_ATTACKS_USED : BotMessages.formatUnusedAttacksMessage(lines);
    }

    public String getStartMessage(@NonNull TelegramMessage message) {
        return BotMessages.formatStartMessage("private".equals(message.telegramChat().type()));
    }

    @Transactional
    public String getSetGroupMessage(@NonNull TelegramMessage message) {
        long chatId = message.telegramChat().id();
        if (!telegramAccessService.isAdmin(getUserId(message))) {
            return ACCESS_DENIED;
        }
        if (chatId >= 0) {
            return SET_GROUP_PRIVATE_CHAT;
        }
        Long previousChatId = telegramGroupChatIdService.getChatId().orElse(null);
        if (!Objects.equals(previousChatId, chatId)) {
            telegramGroupChatIdService.upsertChatId(chatId);
            telegramGroupChatThreadsService.resetThreadIds();
        }
        return GROUP_SET;
    }

    @Transactional
    public String getSetWarThreadMessage(@NonNull TelegramMessage message) {
        long chatId = message.telegramChat().id();
        if (!telegramAccessService.isAdmin(getUserId(message))) {
            return ACCESS_DENIED;
        }
        Long groupChatId = telegramGroupChatIdService.getChatId().orElse(null);
        if (groupChatId == null) {
            return GROUP_NOT_SET;
        }
        if (chatId != groupChatId) {
            return WRONG_GROUP;
        }
        Integer warThreadId = message.forumThreadId();
        if (warThreadId == null || warThreadId <= 0) {
            return INVALID_THREAD;
        }
        telegramGroupChatThreadsService.upsertWarThreadId(warThreadId);
        return WAR_THREAD_SET;
    }

    @Transactional
    public String getSetMainThreadMessage(@NonNull TelegramMessage message) {
        long chatId = message.telegramChat().id();
        if (!telegramAccessService.isAdmin(getUserId(message))) {
            return ACCESS_DENIED;
        }
        Long groupChatId = telegramGroupChatIdService.getChatId().orElse(null);
        if (groupChatId == null) {
            return GROUP_NOT_SET;
        }
        if (chatId != groupChatId) {
            return WRONG_GROUP;
        }
        Integer mainThreadId = message.forumThreadId();
        if (mainThreadId != null && mainThreadId <= 0) {
            return INVALID_THREAD;
        }
        telegramGroupChatThreadsService.upsertMainThreadId(mainThreadId);
        return MAIN_THREAD_SET;
    }

    @Transactional
    public String getResetGroupMessage(@NonNull TelegramMessage message) {
        long chatId = message.telegramChat().id();
        if (!telegramAccessService.isAdmin(getUserId(message))) {
            return ACCESS_DENIED;
        }
        Long groupChatId = telegramGroupChatIdService.getChatId().orElse(null);
        if (groupChatId == null) {
            return GROUP_NOT_SET;
        }
        if (chatId != groupChatId) {
            return WRONG_GROUP;
        }
        telegramGroupChatIdService.resetChatId();
        telegramGroupChatThreadsService.resetThreadIds();
        return GROUP_RESET;
    }

    @Transactional
    public String getResetWarThreadMessage(@NonNull TelegramMessage message) {
        long chatId = message.telegramChat().id();
        if (!telegramAccessService.isAdmin(getUserId(message))) {
            return ACCESS_DENIED;
        }
        Long groupChatId = telegramGroupChatIdService.getChatId().orElse(null);
        if (groupChatId == null) {
            return GROUP_NOT_SET;
        }
        if (chatId != groupChatId) {
            return WRONG_GROUP;
        }
        if (telegramGroupChatThreadsService.getWarThreadId().isEmpty()) {
            return WAR_THREAD_NOT_SET;
        }
        telegramGroupChatThreadsService.resetWarThreadId();
        return WAR_THREAD_RESET;
    }

    @Transactional
    public String getResetMainThreadMessage(@NonNull TelegramMessage message) {
        long chatId = message.telegramChat().id();
        if (!telegramAccessService.isAdmin(getUserId(message))) {
            return ACCESS_DENIED;
        }
        Long groupChatId = telegramGroupChatIdService.getChatId().orElse(null);
        if (groupChatId == null) {
            return GROUP_NOT_SET;
        }
        if (chatId != groupChatId) {
            return WRONG_GROUP;
        }
        if (telegramGroupChatThreadsService.getMainThreadId().isEmpty()) {
            return MAIN_THREAD_NOT_SET;
        }
        telegramGroupChatThreadsService.resetMainThreadId();
        return MAIN_THREAD_RESET;
    }

    private static Long getUserId(TelegramMessage message) {
        return message.telegramUser() == null ? null : message.telegramUser().id();
    }

    public static boolean isRaidAttackCountConsistent(
            @NonNull RaidSeason raidSeason,
            @NonNull List<RaidSeasonParticipant> participants) {
        Integer expectedAttacks = raidSeason.getTotalAttacks();
        if (expectedAttacks == null || expectedAttacks < 0) {
            return false;
        }
        long actualAttacks = 0;
        for (RaidSeasonParticipant participant : participants) {
            Integer attackCount = participant.getAttackCount();
            if (attackCount == null || attackCount < 0) {
                return false;
            }
            actualAttacks += attackCount;
        }
        return actualAttacks == expectedAttacks;
    }
}
