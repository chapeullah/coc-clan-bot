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

import static com.chapeullah.cocclanbot.ClashOfClansTranslations.CLAN_TYPES_RU;
import static com.chapeullah.cocclanbot.ClashOfClansTranslations.LOCATION_NAMES_RU;
import static com.chapeullah.cocclanbot.ClashOfClansTranslations.LEAGUE_TIER_NAMES_RU;
import static com.chapeullah.cocclanbot.ClashOfClansTranslations.ROLE_NAMES_RU;
import static com.chapeullah.cocclanbot.ClashOfClansTranslations.WAR_LEAGUE_NAMES_RU;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@DependsOn("environmentVariablesService")
public class BotCommandMessageService {

    private static final String ACCESS_DENIED = "❌ Нет доступа";
    private static final String GROUP_NOT_SET = "❌ Группа не задана: /set_group";
    private static final String WRONG_GROUP = "❌ Команда доступна только в заданной группе.";
    private static final String INVALID_THREAD = "❌ Отправь команду внутри правильной темы.";
    private static final String NO_DATA = "Нет данных";
    private static final String NO_MEMBERS = "Список участников клана пуст";
    private static final String NO_RAID_SEASONS = "Сезоны рейдов ещё не сохранены";
    private static final String NO_ACTIVE_WAR = "Сейчас активной войны нет";
    private static final String DATA_PENDING = "Уточняется";
    private static final String IN_PROGRESS = "Идёт";
    private static final String WAR_VICTORY = "Победа";
    private static final String WAR_DEFEAT = "Поражение";

    private static final String USAGE_NOTICE = """
            В личных сообщениях доступны только /start и /help.
            Остальные команды используйте в группе <a href="https://t.me/gussicoc">ГУССИ</a>.
            
            <i>В группе команды и ответы бота видны только Вам.</i>""";

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
        String type = clan.getType();
        if (type == null) {
            throw new IllegalStateException("Clan type is null: clanTag=\"" + clan.getTag() + "\"");
        }
        type = CLAN_TYPES_RU.get(type);
        if (type == null) {
            throw new IllegalStateException(
                    "Missing clan type translation: type=\"" + clan.getType() + "\", clanTag=\"" + clan.getTag() + "\"");
        }

        String warLeague = NO_DATA;
        if (clan.getWarLeague() != null) {
            Integer id = clan.getWarLeague().getId();
            warLeague = WAR_LEAGUE_NAMES_RU.get(id);
            if (warLeague == null) {
                throw new IllegalStateException(
                        "Missing war league translation: warLeagueId=" + id + ", clanTag=\"" + clan.getTag() + "\"");
            }
        }

        String location = NO_DATA;
        if (clan.getLocation() != null) {
            Integer id = clan.getLocation().getId();
            location = LOCATION_NAMES_RU.get(id);
            if (location == null) {
                throw new IllegalStateException(
                        "Missing location translation: locationId=" + id + ", clanTag=\"" + clan.getTag() + "\"");
            }
        }

        String description = clan.getDescription() == null || clan.getDescription().isBlank() ? "Без описания"
                : TelegramUtils.escapeHtml(clan.getDescription());
        return """
                <b>%s</b>
                <code>%s</code>
                
                %s
                
                Уровень: %s
                Участники: %s из 50
                Тип: %s
                Очки клана: %s
                Очки столицы: %s
                
                Военная лига: %s
                Винрейт: %s
                Победы: %s
                Серия побед: %s
                
                Расположение: %s"""
                .formatted(
                        TelegramUtils.escapeHtml(clan.getName()),
                        TelegramUtils.escapeHtml(clan.getTag()),
                        description,
                        valueOrNoData(clan.getClanLevel()),
                        valueOrNoData(clan.getMemberCount()),
                        TelegramUtils.escapeHtml(type),
                        valueOrNoData(clan.getClanPoints()),
                        valueOrNoData(clan.getClanCapitalPoints()),
                        warLeague,
                        formatWarWinRate(clan),
                        valueOrNoData(clan.getWarWins()),
                        valueOrNoData(clan.getWarWinStreak()),
                        location);
    }

    private static String formatWarWinRate(@NonNull Clan clan) {
        Integer wins = clan.getWarWins();
        Integer losses = clan.getWarLosses();
        if (wins == null || losses == null) {
            return NO_DATA;
        }

        int totalWarsCount = wins + losses;
        if (totalWarsCount == 0) {
            return NO_DATA;
        }

        double winRate = (double) wins * 100 / totalWarsCount; // Excluding ties
        return "%.2f%%".formatted(winRate);
    }

    public String getHelpMessage(
            @NonNull Map<BotCommandType, List<BotCommand>> commandsByType,
            boolean isAdmin,
            boolean isPrivateChat) {
        StringBuilder message = new StringBuilder("<b>Доступные команды</b>\n\n");
        message.append(commandsByType.values()
                .stream()
                .map(commands -> commands
                        .stream()
                        .filter(command -> (isAdmin || !command.isAdminOnly()))
                        .map(command -> command.getName() + " — " + command.getDescription())
                        .collect(Collectors.joining("\n")))
                .filter(group -> !group.isEmpty())
                .collect(Collectors.joining("\n\n")));
        if (isPrivateChat) {
            message.append("\n\n").append(USAGE_NOTICE);
        }
        return message.toString();
    }

    @Transactional(readOnly = true)
    public String getMemberMessage(@NonNull Member member) {
        String name = TelegramUtils.escapeHtml(TelegramUtils.replaceEmojisWithSquares(member.getName()));

        String role = ROLE_NAMES_RU.get(member.getRole());
        if (role == null) {
            throw new IllegalStateException("Missing member role translation: role=\"" + member.getRole()
                    + "\", memberTag=\"" + member.getTag() + "\"");
        }
        role = TelegramUtils.escapeHtml(role);

        String league = member.getLeagueTier() == null ? NO_DATA : TelegramUtils
                .escapeHtml(LEAGUE_TIER_NAMES_RU.getOrDefault(
                        member.getLeagueTier().getId(), member.getLeagueTier().getName()));

        StringBuilder message = new StringBuilder("""
                <b>%s</b>
                <code>%s</code>

                Роль: %s
                """.formatted(
                name, TelegramUtils.escapeHtml(member.getTag()), role));
        if (Objects.equals(Boolean.TRUE, member.getInClan())) {
            message.append("Место в клане: %s\n".formatted(valueOrNoData(member.getClanRank())));
        }

        message.append("""
                Ратуша: %s
                Уровень: %s
                Лига: %s
                Трофеи: %s
                Трофеи деревни строителя: %s

                <b>Пожертвования за сезон</b>
                Отдано: %s
                Получено: %s

                <b>Пожертвования за всё время</b>
                Отдано: %s
                Получено: %s
                """.formatted(
                valueOrNoData(member.getTownHallLevel()),
                valueOrNoData(member.getExpLevel()),
                league,
                valueOrNoData(member.getTrophies()),
                valueOrNoData(member.getBuilderBaseTrophies()),
                valueOrNoData(member.getDonations()),
                valueOrNoData(member.getDonationsReceived()),
                valueOrNoData(member.getTotalDonations()),
                valueOrNoData(member.getTotalDonationsReceived())));
        if (Objects.equals(Boolean.FALSE, member.getInClan())) {
            message.append("\n<i>Последние сохранённые данные участника</i>");
        }
        return message.toString().strip();
    }

    private static String valueOrNoData(Object value) {
        return value == null ? NO_DATA : value.toString();
    }

    public String getMembersMessage(
            @NonNull List<Member> members,
            @NonNull Map<String, MemberStatistic> statisticsByTag) {
        StringBuilder message = new StringBuilder("<b>Участники клана</b>\n\n");
        members.forEach(member -> {
            String name = TelegramUtils.escapeHtml(TelegramUtils.replaceEmojisWithSquares(member.getName()));
            MemberStatistic statistic = statisticsByTag.get(member.getTag());
            String ratingText = statistic == null ? NO_DATA : statistic.totalRating()
                    .map(rating -> String.format(Locale.ROOT, "%.2f", rating))
                    .orElse(NO_DATA);
            message.append("<b>%02d. %s</b> | <code>%s</code> | %s\n"
                    .formatted(
                            member.getClanRank(),
                            name,
                            TelegramUtils.escapeHtml(member.getTag()),
                            ratingText));
        });
        return message.toString().strip();
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
    public String getRaidMessage(@NonNull) {
        RaidSeason season = raidSeasonService.findLatestByClanTag(clanTag).orElse(null);
        if (season == null) {
            return NO_RAID_SEASONS;
        }

        List<RaidSeasonParticipant> participants =
                raidSeasonParticipantService.getByRaidSeason(season);
        String participantCount = hasCompleteRaidParticipants(season, participants)
                ? Long.toString(participants.stream()
                        .filter(participant -> participant.getAttackCount() > 0)
                        .count())
                : DATA_PENDING;

        return """
                <b>%s</b>

                Статус: %s
                Период: %s (МСК)

                Столичное золото: %s
                Атаки: %s
                Участников с атаками: %s
                Завершено рейдов: %s
                Уничтожено районов: %s
                """.formatted(
                        "ongoing".equals(season.getState())
                                ? "Текущий сезон рейдов"
                                : "Последний сезон рейдов",
                        TelegramUtils.escapeHtml(getRaidState(season)),
                        getRaidPeriod(season),
                        valueOrNoData(season.getCapitalTotalLoot()),
                        valueOrNoData(season.getTotalAttacks()),
                        participantCount,
                        valueOrNoData(season.getRaidsCompleted()),
                        valueOrNoData(season.getEnemyDistrictsDestroyed()))
                .strip();
    }

    @Transactional(readOnly = true)
    public String getRaidMissedMessage() {
        RaidSeason season = raidSeasonService.findLatestByClanTag(clanTag).orElse(null);
        if (season == null) {
            return NO_RAID_SEASONS;
        }

        List<RaidSeasonParticipant> participants =
                raidSeasonParticipantService.getByRaidSeason(season);
        if (!hasCompleteRaidParticipants(season, participants)) {
            return "❌ Данные об участниках рейда пока неполные";
        }

        List<RaidSeasonClanMemberSnapshot> members =
                raidSeasonClanMemberSnapshotService.getByRaidSeason(season);
        if (members.isEmpty()) {
            return "❌ Снимок состава клана для этого сезона не сохранён";
        }

        Set<String> attackedTags = new HashSet<>();
        for (RaidSeasonParticipant participant : participants) {
            if (participant.getAttackCount() > 0) {
                attackedTags.add(participant.getTag());
            }
        }

        StringBuilder message = new StringBuilder("""
                <b>Участники без атак в рейдах</b>

                Период: %s (МСК)
                Статус: %s
                Состав клана, сохранённый в течение сезона
                Подтверждённо недопущенные участники исключены

                """.formatted(
                        getRaidPeriod(season),
                        TelegramUtils.escapeHtml(getRaidState(season))));

        int number = 1;
        for (RaidSeasonClanMemberSnapshot member : members) {
            if (attackedTags.contains(member.getTag())
                    || Boolean.FALSE.equals(member.getParticipationEligible())) {
                continue;
            }

            String name = TelegramUtils.escapeHtml(
                    TelegramUtils.replaceEmojisWithSquares(member.getName()));
            message.append("%02d. %s | <code>%s</code>\n"
                    .formatted(
                            number++,
                            name,
                            TelegramUtils.escapeHtml(member.getTag())));
        }
        if (number == 1) {
            message.append("Среди учитываемых участников нет игроков без атак");
        }
        return message.toString().strip();
    }

    @Transactional(readOnly = true)
    public String getRaidsMessage() {
        List<RaidSeason> seasons = raidSeasonService.getRecentByClanTag(clanTag);
        if (seasons.isEmpty()) {
            return NO_RAID_SEASONS;
        }

        StringBuilder message = new StringBuilder("<b>История рейдов</b>\n\n");
        for (RaidSeason season : seasons) {
            message.append("""
                    <b>%s</b> (МСК)
                    Статус: %s
                    Столичное золото: %s
                    Атаки: %s
                    Завершено рейдов: %s
                    Уничтожено районов: %s

                    """.formatted(
                            getRaidPeriod(season),
                            TelegramUtils.escapeHtml(getRaidState(season)),
                            valueOrNoData(season.getCapitalTotalLoot()),
                            valueOrNoData(season.getTotalAttacks()),
                            valueOrNoData(season.getRaidsCompleted()),
                            valueOrNoData(season.getEnemyDistrictsDestroyed())));
        }
        return message.toString().strip();
    }

    private static boolean hasCompleteRaidParticipants(
            RaidSeason season, List<RaidSeasonParticipant> participants) {
        Integer expectedAttacks = season.getTotalAttacks();
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

    private static String getRaidPeriod(RaidSeason season) {
        return DATE_TIME_FORMATTER.format(season.getStartTime()) + " — "
                + DATE_TIME_FORMATTER.format(season.getEndTime());
    }

    private static String getRaidState(RaidSeason season) {
        if (season.getState() == null) {
            return NO_DATA;
        }
        return switch (season.getState()) {
            case "ongoing" -> IN_PROGRESS;
            case "ended" -> "Завершён";
            default -> season.getState();
        };
    }

}
