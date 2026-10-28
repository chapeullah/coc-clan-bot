package com.chapeullah.cocclanbot.telegram.command;

import com.chapeullah.cocclanbot.clan.model.Clan;
import com.chapeullah.cocclanbot.member.MemberService;
import com.chapeullah.cocclanbot.member.model.Member;
import com.chapeullah.cocclanbot.raidseason.model.RaidSeason;
import com.chapeullah.cocclanbot.raidseason.model.RaidSeasonClanMemberSnapshot;
import com.chapeullah.cocclanbot.raidseason.model.RaidSeasonParticipant;
import com.chapeullah.cocclanbot.raidseason.service.RaidSeasonParticipantService;
import com.chapeullah.cocclanbot.raidseason.service.RaidSeasonClanMemberSnapshotService;
import com.chapeullah.cocclanbot.raidseason.service.RaidSeasonService;
import com.chapeullah.cocclanbot.rating.*;
import com.chapeullah.cocclanbot.telegram.TelegramAccessService;
import com.chapeullah.cocclanbot.telegram.TelegramUtils;
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
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.chapeullah.cocclanbot.telegram.command.BotMessages.*;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@DependsOn("environmentVariablesService")
public class BotCommandMessageService {

    private static final String ACCESS_DENIED = "❌ Нет доступа";
    private static final String GROUP_NOT_SET = "❌ Группа не задана: /set_group";
    private static final String WRONG_GROUP = "❌ Команда доступна только в заданной группе.";
    private static final String INVALID_THREAD = "❌ Отправь команду внутри правильной темы.";

    private static final String NO_MEMBERS = "Список участников клана пуст";
    private static final String NO_RAID_SEASONS = "Сезоны рейдов ещё не сохранены";
    private static final String NO_ACTIVE_WAR = "Сейчас активной войны нет";
    private static final String WAR_VICTORY = "Победа";
    private static final String WAR_DEFEAT = "Поражение";

    private final WarService warService;
    private final WarParticipantService warParticipantService;
    private final WarAttackService warAttackService;
    private final RaidSeasonService raidSeasonService;
    private final RaidSeasonParticipantService raidSeasonParticipantService;
    private final RaidSeasonClanMemberSnapshotService raidSeasonClanMemberSnapshotService;
    private final MemberService memberService;
    private final RatingService ratingService;

    private final TelegramGroupChatThreadsService telegramGroupChatThreadsService;
    private final TelegramGroupChatIdService telegramGroupChatIdService;

    private final TelegramAccessService telegramAccessService;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter
            .ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.of("Europe/Moscow"));

    @Transactional(readOnly = true)
    public String getClanMessage(@NonNull Clan clan) {
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
                clan.getName(),
                clan.getTag(),
                clan.getDescription(),
                clan.getClanLevel(),
                clan.getMemberCount(),
                clan.getType(),
                clan.getClanPoints(),
                clan.getClanCapitalPoints(),
                clan.getWarLeague(),
                warWinRate,
                wins,
                clan.getWarWinStreak(),
                clan.getLocation());
    }

    public String getHelpMessage(
            @NonNull Map<BotCommandType, List<BotCommand>> commandsByType,
            boolean isAdmin,
            boolean isPrivateChat) {
        List<List<BotCommand>> visibleGroups = commandsByType.values()
                .stream()
                .map(commands -> commands
                        .stream()
                        .filter(command -> isAdmin || !command.isAdminOnly())
                        .toList())
                .filter(group -> !group.isEmpty())
                .toList();
        return BotMessages.formatHelpMessage(visibleGroups, isPrivateChat);
    }

    @Transactional(readOnly = true)
    public String getMemberMessage(@NonNull Member member) {
        return BotMessages.formatMemberMessage(
                member.getName(),
                member.getTag(),
                member.getRole(),
                member.getInClan(),
                member.getClanRank(),
                member.getTownHallLevel(),
                member.getExpLevel(),
                member.getLeagueTier(),
                member.getTrophies(),
                member.getBuilderBaseTrophies(),
                member.getDonations(),
                member.getDonationsReceived(),
                member.getTotalDonations(),
                member.getTotalDonationsReceived());
    }

    public String getMembersMessage(
            @NonNull List<Member> members,
            @NonNull Map<String, MemberStatistic> statisticsByTag) {
        List<String> memberLines = members.stream()
                .map(member -> {
                    MemberStatistic statistic = statisticsByTag.get(member.getTag());
                    Double rating = statistic == null ? null : statistic.totalRating().orElse(null);
                    return BotMessages.formatMemberListItem(
                            member.getClanRank(),
                            member.getName(),
                            member.getTag(),
                            rating);
                })
                .toList();
        return BotMessages.formatMembersMessage(memberLines);
    }

    @Transactional(readOnly = true)
    public String getRaidMessage(
            @NonNull RaidSeason raidSeason,
            @NonNull List<RaidSeasonParticipant> participants) {
        Long participantCount = isRaidAttackCountConsistent(raidSeason, participants)
                ? participants.stream()
                        .filter(participant -> participant.getAttackCount() > 0)
                        .count()
                : null;
        return BotMessages.formatRaidMessage(
                raidSeason.getState(),
                raidSeason.getStartTime(),
                raidSeason.getEndTime(),
                raidSeason.getCapitalTotalLoot(),
                raidSeason.getTotalAttacks(),
                participantCount,
                raidSeason.getRaidsCompleted(),
                raidSeason.getEnemyDistrictsDestroyed());
    }

    @Transactional
    public String getSetGroupMessage(long chatId, Long userId) {
        if (!telegramAccessService.isAdmin(userId)) {
            return ACCESS_DENIED;
        }
        if (chatId >= 0) {
            return "❌ Вызови команду в нужной группе";
        }
        Long previousChatId = telegramGroupChatIdService.getChatId().orElse(null);
        if (!Objects.equals(previousChatId, chatId)) {
            telegramGroupChatIdService.upsertChatId(chatId);
            telegramGroupChatThreadsService.resetThreadIds();
        }
        return "✅ Эта группа назначена группой клана";
    }

    @Transactional
    public String setWarThread(long chatId, Long userId, Integer warThreadId) {
        if (!telegramAccessService.isAdmin(userId)) {
            return ACCESS_DENIED;
        }
        Long groupChatId = telegramGroupChatIdService
                .getChatId()
                .orElse(null);
        if (groupChatId == null) {
            return GROUP_NOT_SET;
        }
        if (chatId != groupChatId) {
            return WRONG_GROUP;
        }
        if (warThreadId == null || warThreadId <= 0) {
            return INVALID_THREAD;
        }
        telegramGroupChatThreadsService.upsertWarThreadId(warThreadId);
        return "✅ Этот чат назначен для уведомлений о КВ.";
    }

    @Transactional
    public String setMainThread(long chatId, Long userId, Integer mainThreadId) {
        if (!telegramAccessService.isAdmin(userId)) {
            return ACCESS_DENIED;
        }
        Long groupChatId = telegramGroupChatIdService
                .getChatId()
                .orElse(null);
        if (groupChatId == null) {
            return GROUP_NOT_SET;
        }
        if (chatId != groupChatId) {
            return WRONG_GROUP;
        }
        if (mainThreadId != null && mainThreadId <= 0) {
            return INVALID_THREAD;
        }
        telegramGroupChatThreadsService.upsertMainThreadId(mainThreadId);
        return "✅ Этот чат назначен основным.";
    }

    @Transactional
    public String resetGroup(long chatId, Long userId) {
        if (!telegramAccessService.isAdmin(userId)) {
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
        return "✅ Группа клана и настройки тем сброшены";
    }

    @Transactional
    public String resetWarThread(long chatId, Long userId) {
        if (!telegramAccessService.isAdmin(userId)) {
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
            return "❌ Тема уведомлений о КВ не задана";
        }
        telegramGroupChatThreadsService.resetWarThreadId();
        return "✅ Тема уведомлений о КВ сброшена.";
    }

    @Transactional
    public String resetMainThread(long chatId, Long userId) {
        if (!telegramAccessService.isAdmin(userId)) {
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
            return "❌ Основной чат не задан";
        }
        telegramGroupChatThreadsService.resetMainThreadId();
        return "✅ Основной чат сброшен.";
    }

    public String start(TelegramMessage message) {
        String start = """
            Телеграм-бот клана <a href="https://t.me/gussicoc">GUCCИ</a> (<code>#2C020L9P0</code>) в Clash of Clans.
            Не работает с другими кланами.
            
            Используй /help, чтобы посмотреть доступные команды.""";
        if ("private".equals(message.telegramChat().type())) {
            start += "\n\n" + USAGE_NOTICE;
        }
        return start;
    }

    @Transactional(readOnly = true)
    public String getRatingMessage() {
        List<MemberStatistic> statistics = ratingService
                .calculateClanMemberStatistics(clanTag)
                .values().stream()
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

        StringBuilder message = new StringBuilder("<b>Рейтинг участников</b>\n\n");
        int number = 1;
        for (MemberStatistic statistic : statistics) {
            appendRatingMember(message, number++, statistic.name(), statistic.totalRating());
            message.append("\n");
        }
        return message.toString().strip();
    }

    @Transactional(readOnly = true)
    public String getWarRatingMessage(@NonNull List<MemberWarStatistic> statistics) {
        if (statistics.isEmpty()) {
            return NO_MEMBERS;
        }
        StringBuilder message = new StringBuilder("""
                <b>Рейтинг КВ</b>
                Средние значения за атаку в КВ.
                АТ — атаки.

                """);
        int number = 1;
        for (MemberWarStatistic statistic : statistics) {
            appendRatingMember(message, number++, statistic.name(), statistic.warRating());
            if (statistic.warRating().isPresent()) {
                message.append(" | ").append(formatWarAttackStatistics(statistic));
            }
            message.append("\n");
        }
        return message.toString().strip();
    }

    @Transactional(readOnly = true)
    public String getRaidRatingMessage() {
        List<MemberRaidStatistic> statistics = ratingService
                .calculateClanMemberRaidStatistics(clanTag)
                .values().stream()
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

        StringBuilder message = new StringBuilder("""
                <b>Рейтинг рейдов</b>
                Средние значения за атаку в рейде.
                АТ — атаки.

                """);
        int number = 1;
        for (MemberRaidStatistic statistic : statistics) {
            appendRatingMember(message, number++, statistic.name(), statistic.raidRating());
            if (statistic.raidRating().isPresent()) {
                message.append(" | ").append(formatRaidAttackStatistics(statistic));
            }
            message.append("\n");
        }
        return message.toString().strip();
    }

    private static void appendRatingMember(
            StringBuilder message, int number, String memberName, Optional<Double> rating) {
        String name = TelegramUtils.escapeHtml(
                TelegramUtils.replaceEmojisWithSquares(memberName)
                        .replaceAll("(?U)\\s+", " ")
                        .strip());
        String ratingText = rating
                .map(value -> String.format(Locale.ROOT, "%.2f", value))
                .orElse(NO_DATA);
        message.append("<b>%02d. %s</b> — %s".formatted(number, name, ratingText));
    }

    private static String formatWarAttackStatistics(MemberWarStatistic statistic) {
        return statistic.warCount() > 0
                ? "%s | %s | АТ %s".formatted(
                        formatStatisticValue(statistic.averageStarsPerAttack(), "%.2f★"),
                        formatStatisticValue(statistic.averageDestructionPercentagePerAttack(), "%.1f%%"),
                        String.format(
                                Locale.ROOT,
                                "%.2f",
                                (double) statistic.warAttackCount() / statistic.warCount()))
                : NO_DATA;
    }

    private static String formatRaidAttackStatistics(MemberRaidStatistic statistic) {
        return statistic.averageRaidLootPerAttack().isPresent()
                ? "%s | АТ %s".formatted(
                        formatStatisticValue(statistic.averageRaidLootPerAttack(), "%.0f/ат."),
                        formatStatisticValue(statistic.averageRaidAttacksPerSeason(), "%.2f"))
                : NO_DATA;
    }

    private static String formatStatisticValue(Optional<Double> value, String format) {
        return value.map(number -> String.format(Locale.ROOT, format, number))
                .orElse("-");
    }

    private static String formatAverageDifference(Optional<Double> value) {
        return value.map(number -> {
            if (Math.round(Math.abs(number) * 10.0) == 0) {
                return "0.0";
            }
            return String.format(Locale.ROOT, "%s%.1f", number > 0.0 ? "↑" : "↓", Math.abs(number));
        }).orElse("-");
    }

    @Transactional(readOnly = true)
    public String getCurrentWarMessage() {
        War war = warService.findCurrentByClanTag(clanTag).orElse(null);
        if (war == null) {
            return NO_ACTIVE_WAR;
        }

        boolean preparation = "preparation".equals(war.getState());
        String state = preparation ? "Подготовка" : IN_PROGRESS;
        int maximumAttacks = war.getTeamSize() * war.getAttacksPerMember();
        String timeTitle = preparation ? "Начало" : "Окончание";
        String time = DATE_TIME_FORMATTER.format(
                preparation ? war.getStartTime() : war.getEndTime());

        return """
                <b>Текущая война</b>

                <b>%s</b> — <b>%s</b>
                Статус: %s
                Размер: %d на %d

                Звёзды: %d — %d
                Разрушение: %.2f%% — %.2f%%
                Наши атаки: %d из %d
                Атаки соперника: %d из %d

                %s: %s (МСК)
                """.formatted(
                        TelegramUtils.escapeHtml(war.getClanName()),
                        TelegramUtils.escapeHtml(war.getOpponentClanName()),
                        state,
                        war.getTeamSize(),
                        war.getTeamSize(),
                        war.getClanStars(),
                        war.getOpponentClanStars(),
                        war.getClanDestructionPercentage(),
                        war.getOpponentClanDestructionPercentage(),
                        war.getClanAttacks(),
                        maximumAttacks,
                        war.getOpponentClanAttacks(),
                        maximumAttacks,
                        timeTitle,
                        time)
                .strip();
    }

    @Transactional(readOnly = true)
    public String getWarsMessage() {
        List<War> wars = warService.getRecentCompletedByClanTag(clanTag);
        if (wars.isEmpty()) {
            return "Завершённые войны не найдены";
        }

        StringBuilder message = new StringBuilder("<b>История войн</b>\n\n");
        for (War war : wars) {
            message.append("""
                    <b>%s</b> — <b>%s</b>
                    Результат: %s
                    Звёзды: %d — %d
                    Разрушение: %.2f%% — %.2f%%
                    Завершена: %s (МСК)

                    """.formatted(
                            TelegramUtils.escapeHtml(war.getClanName()),
                            TelegramUtils.escapeHtml(war.getOpponentClanName()),
                            getWarResult(war),
                            war.getClanStars(),
                            war.getOpponentClanStars(),
                            war.getClanDestructionPercentage(),
                            war.getOpponentClanDestructionPercentage(),
                            DATE_TIME_FORMATTER.format(war.getEndTime())));
        }
        return message.toString().strip();
    }

    private static String getWarResult(War war) {
        if (war.getWarLogSyncedAt() == null) {
            return DATA_PENDING;
        }
        int starsComparison = Integer.compare(
                war.getClanStars(), war.getOpponentClanStars());
        if (starsComparison > 0) {
            return WAR_VICTORY;
        }
        if (starsComparison < 0) {
            return WAR_DEFEAT;
        }

        int destructionComparison = Double.compare(
                war.getClanDestructionPercentage(),
                war.getOpponentClanDestructionPercentage());
        if (destructionComparison > 0) {
            return WAR_VICTORY;
        }
        if (destructionComparison < 0) {
            return WAR_DEFEAT;
        }
        return "Ничья";
    }

    @Transactional(readOnly = true)
    public String attacks() {
        War war = warService.findCurrentByClanTag(clanTag).orElse(null);

        if (war == null) {
            return NO_ACTIVE_WAR;
        }

        if ("preparation".equals(war.getState())) {
            return "❌ Война ещё не началась.";
        }

        List<WarParticipant> participants = warParticipantService.getByWar(war).stream()
                .filter(participant -> clanTag.equals(participant.getClanTag()))
                .sorted(Comparator.comparing(WarParticipant::getMapPosition))
                .toList();

        Map<String, Integer> usedAttacks = new HashMap<>();

        for (WarAttack attack : warAttackService.getByWar(war)) {
            usedAttacks.merge(
                    attack.getAttackerTag(),
                    1,
                    Integer::sum);
        }

        StringBuilder result = new StringBuilder(
                "<b>Неиспользованные атаки</b>\n");

        boolean hasUnusedAttacks = false;

        for (WarParticipant participant : participants) {
            int used = usedAttacks.getOrDefault(
                    participant.getPlayerTag(),
                    0);

            int remaining = Math.max(
                    0,
                    war.getAttacksPerMember() - used);

            if (remaining == 0) {
                continue;
            }

            hasUnusedAttacks = true;

            result.append("\n")
                    .append(participant.getMapPosition())
                    .append(". ")
                    .append(TelegramUtils.escapeHtml(participant.getPlayerName()))
                    .append(" — ")
                    .append(remaining)
                    .append(" из ")
                    .append(war.getAttacksPerMember());
        }

        if (!hasUnusedAttacks) {
            return "Все участники использовали свои атаки.";
        }

        return result.toString();
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
                raidSeason.getState(),
                raidSeason.getStartTime(),
                raidSeason.getEndTime(),
                membersWithoutAttacks);
    }

    @Transactional(readOnly = true)
    public String getRaidsMessage(@NonNull List<RaidSeason> raidSeasons) {
        StringBuilder message = new StringBuilder("<b>История рейдов</b>\n\n");
        for (RaidSeason raidSeason : raidSeasons) {
            message.append("""
                    <b>%s</b> (МСК)
                    Статус: %s
                    Столичное золото: %s
                    Атаки: %s
                    Завершено рейдов: %s
                    Уничтожено районов: %s

                    """.formatted(
                            formatRaidPeriod(raidSeason.getStartTime(), raidSeason.getEndTime()),
                            TelegramUtils.escapeHtml(formatRaidState(raidSeason.getState())),
                            valueOrNoData(raidSeason.getCapitalTotalLoot()),
                            valueOrNoData(raidSeason.getTotalAttacks()),
                            valueOrNoData(raidSeason.getRaidsCompleted()),
                            valueOrNoData(raidSeason.getEnemyDistrictsDestroyed())));
        }
        return message.toString().strip();
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
