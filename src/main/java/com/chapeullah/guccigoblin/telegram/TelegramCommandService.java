package com.chapeullah.guccigoblin.telegram;

import com.chapeullah.guccigoblin.clan.service.ClanService;
import com.chapeullah.guccigoblin.clan.model.Clan;
import com.chapeullah.guccigoblin.member.MemberService;
import com.chapeullah.guccigoblin.member.model.Member;
import com.chapeullah.guccigoblin.player.exception.PlayerNotFoundException;
import com.chapeullah.guccigoblin.player.model.Player;
import com.chapeullah.guccigoblin.player.model.PlayerHero;
import com.chapeullah.guccigoblin.player.service.PlayerService;
import com.chapeullah.guccigoblin.raidseason.model.RaidSeason;
import com.chapeullah.guccigoblin.raidseason.model.RaidSeasonClanMemberSnapshot;
import com.chapeullah.guccigoblin.raidseason.model.RaidSeasonParticipant;
import com.chapeullah.guccigoblin.raidseason.service.RaidSeasonParticipantService;
import com.chapeullah.guccigoblin.raidseason.service.RaidSeasonClanMemberSnapshotService;
import com.chapeullah.guccigoblin.raidseason.service.RaidSeasonService;
import com.chapeullah.guccigoblin.rating.MemberStatistic;
import com.chapeullah.guccigoblin.rating.MemberWarStatistic;
import com.chapeullah.guccigoblin.rating.MemberRaidStatistic;
import com.chapeullah.guccigoblin.rating.RatingService;
import com.chapeullah.guccigoblin.war.model.War;
import com.chapeullah.guccigoblin.war.model.WarAttack;
import com.chapeullah.guccigoblin.war.model.WarParticipant;
import com.chapeullah.guccigoblin.war.service.WarAttackService;
import com.chapeullah.guccigoblin.war.service.WarParticipantService;
import com.chapeullah.guccigoblin.war.service.WarService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.chapeullah.guccigoblin.telegram.ClashOfClansTranslations.CLAN_TYPES_RU;
import static com.chapeullah.guccigoblin.telegram.ClashOfClansTranslations.LOCATION_NAMES_RU;
import static com.chapeullah.guccigoblin.telegram.ClashOfClansTranslations.LEAGUE_TIER_NAMES_RU;
import static com.chapeullah.guccigoblin.telegram.ClashOfClansTranslations.ROLE_NAMES_RU;
import static com.chapeullah.guccigoblin.telegram.ClashOfClansTranslations.WAR_LEAGUE_NAMES_RU;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@DependsOn("environmentVariablesValidator")
public class TelegramCommandService {

    private final WarService warService;
    private final WarParticipantService warParticipantService;
    private final WarAttackService warAttackService;
    private final RaidSeasonService raidSeasonService;
    private final RaidSeasonParticipantService raidSeasonParticipantService;
    private final RaidSeasonClanMemberSnapshotService raidSeasonClanMemberSnapshotService;
    private final MemberService memberService;
    private final RatingService ratingService;
    private final ClanService clanService;
    private final PlayerService playerService;

    private final TelegramChatSettingRepository telegramChatSettingRepository;
    private final TelegramAccessService telegramAccessService;

    @Value("${coc.clan-tag}")
    private String clanTag;

    @Value("${TELEGRAM_CLAN_CHAT_LINK}")
    private String telegramClanChatLink;

    @Value("${TELEGRAM_GROUP_CHAT_ID}")
    private long groupChatId;

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
                    .withZone(ZoneId.of("Europe/Moscow"));

    @Transactional
    public String setWarThread(long chatId, Long userId, Integer threadId) {
        if (userId == null || !telegramAccessService.isAdmin(userId)) {
            return "Нет доступа";
        }

        if (chatId != groupChatId) {
            return "Команда доступна только в группе клана";
        }

        if (threadId == null || threadId <= 0) {
            return "Отправь команду внутри нужной темы";
        }

        TelegramChatSetting settings = telegramChatSettingRepository
                .findById(chatId)
                .orElse(null);

        if (settings == null) {
            settings = new TelegramChatSetting(chatId, threadId);
        } else {
            settings.changeWarThread(threadId);
        }

        telegramChatSettingRepository.save(settings);

        return "Эта тема назначена для уведомлений о КВ";
    }

    public String start() {
        return """
            Телеграм-бот клана <a href="https://t.me/gussicoc">GUCCИ</a> (<code>#2C020L9P0</code>) в Clash of Clans.
            Не работает с другими кланами.
            
            Используй /help, чтобы посмотреть доступные команды.
            """.strip();
    }

    public String help() {
        return """
                <b>Доступные команды</b>
                
                
                /start — Начало работы с ботом
                /help — Список команд
                
                /rating — Общий рейтинг участников клана
                /rating_cw — Рейтинг участников клановых войн
                /rating_raids — Рейтинг участников рейдов

                /clan — Информация о клане
                /members — Текущий состав клана
                /member #TAG — Информация об участнике клана
                /player #TAG — Информация об игроке
                /war — Текущая война клана
                /wars — История войн клана
                /raid — Текущий сезон рейдов
                /raid_missed — Участники без атак в рейдах
                /raids — История рейдов
                
                /war_thread — Назначить текущую тему для уведомлений
                """.strip();
    }

    public String getChatId(long chatId) {
        return "ID чата: <code>" + chatId + "</code>";
    }
    public String getMembersMessage() {
        List<Member> members = memberService.getCurrentMembersByClanTag(clanTag);
        if (members.isEmpty()) {
            return "Список участников клана пуст";
        }

        Map<String, MemberStatistic> statisticsByTag =
                ratingService.calculateClanMemberStatistics(clanTag);

        StringBuilder message = new StringBuilder("<b>Участники клана</b>\n\n");
        for (Member member : members) {
            String name = TelegramUtils.escapeHtml(
                    TelegramUtils.replaceEmojisWithSquares(member.getName()));
            MemberStatistic statistic = statisticsByTag.get(member.getTag());
            String ratingText = statistic == null
                    ? "Нет данных"
                    : statistic.totalRating()
                            .map(rating -> String.format(Locale.ROOT, "%.2f", rating))
                            .orElse("Нет данных");
            message.append("<b>%02d. %s</b> | <code>%s</code> | %s\n"
                    .formatted(
                            member.getClanRank(),
                            name,
                            TelegramUtils.escapeHtml(member.getTag()),
                            ratingText));
        }
        return message.toString().strip();
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
            return "Список участников клана пуст";
        }

        StringBuilder message = new StringBuilder("<b>Рейтинг участников</b>\n\n");
        int number = 1;
        for (MemberStatistic statistic : statistics) {
            appendRatingMember(message, number++, statistic.name(), statistic.totalRating());
        }
        return message.toString().strip();
    }

    @Transactional(readOnly = true)
    public String getWarRatingMessage() {
        List<MemberWarStatistic> statistics = ratingService
                .calculateClanMemberWarStatistics(clanTag)
                .values().stream()
                .sorted(Comparator
                        .comparingDouble((MemberWarStatistic statistic) ->
                                statistic.warRating().orElse(Double.NEGATIVE_INFINITY))
                        .reversed()
                        .thenComparing(MemberWarStatistic::name, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(MemberWarStatistic::tag))
                .toList();
        if (statistics.isEmpty()) {
            return "Список участников клана пуст";
        }

        StringBuilder message = new StringBuilder("""
                <b>Рейтинг КВ</b>
                Рейтинг учитывает разницу ТХ; средние звёзды и разрушение показаны без поправок.
                ТХ — средняя ратуша целей; ↑/↓ — разница со своей.
                ПОЗ — позиция целей относительно своей: ↑ выше, ↓ ниже.
                АТ — выполнено/доступно.

                """);
        int number = 1;
        for (MemberWarStatistic statistic : statistics) {
            appendRatingMember(message, number++, statistic.name(), statistic.warRating());
            message.append(formatWarAttackStatistics(statistic)).append("\n\n");
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
            return "Список участников клана пуст";
        }

        StringBuilder message = new StringBuilder("""
                <b>Рейтинг рейдов</b>
                Среднее золото за атаку; АТ — среднее число атак за рейд.

                """);
        int number = 1;
        for (MemberRaidStatistic statistic : statistics) {
            appendRatingMember(message, number++, statistic.name(), statistic.raidRating());
            message.append(formatRaidAttackStatistics(statistic)).append("\n\n");
        }
        return message.toString().strip();
    }

    private static void appendRatingMember(
            StringBuilder message, int number, String memberName, Optional<Double> rating) {
        String name = TelegramUtils.escapeHtml(
                TelegramUtils.replaceEmojisWithSquares(memberName)
                        .replaceAll("(?U)\\s+", " ")
                        .strip());
        message.append("<b>%02d. %s</b> — %s\n"
                .formatted(number, name, formatStatisticValue(rating, "%.2f")));
    }

    private static String formatWarAttackStatistics(MemberWarStatistic statistic) {
        return statistic.averageStarsPerAttack().isPresent()
                || statistic.averageDestructionPercentagePerAttack().isPresent()
                ? "%s | %s | ТХ %s %s | ПОЗ %s | АТ %s".formatted(
                        formatStatisticValue(statistic.averageStarsPerAttack(), "%.2f★"),
                        formatStatisticValue(statistic.averageDestructionPercentagePerAttack(), "%.1f%%"),
                        formatStatisticValue(statistic.averageDefenderTownHallLevelPerAttack(), "%.1f"),
                        formatAverageDifference(statistic.averageTownHallLevelDifferencePerAttack()),
                        formatAverageDifference(statistic.averageMapPositionDifferencePerAttack()),
                        formatAttackUsage(statistic.warAttackCount(), statistic.missedWarAttackCount()))
                : "Нет данных";
    }

    private static String formatRaidAttackStatistics(MemberRaidStatistic statistic) {
        return statistic.averageRaidLootPerAttack().isPresent()
                ? "%s | АТ %s".formatted(
                        formatStatisticValue(statistic.averageRaidLootPerAttack(), "%.0f/ат."),
                        formatStatisticValue(statistic.averageRaidAttacksPerSeason(), "%.2f"))
                : "Нет данных";
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

    private static String formatAttackUsage(int attackCount, int missedAttackCount) {
        int availableAttackCount = attackCount + missedAttackCount;
        return availableAttackCount == 0
                ? "-"
                : attackCount + "/" + availableAttackCount;
    }

    @Transactional(readOnly = true)
    public String getMemberMessage(String args) {
        if (args == null || args.isBlank()) {
            return "Используй: /member #TAG";
        }

        String tag = args.strip().toUpperCase(Locale.ROOT);
        if (!tag.startsWith("#")) {
            tag = "#" + tag;
        }
        if (!tag.matches("#[A-Z0-9]+")) {
            return "Некорректный тег. Используй: /member #TAG";
        }

        Member member = memberService.findByTagAndClanTag(tag, clanTag).orElse(null);
        if (member == null) {
            return "Участник <code>%s</code> не найден в сохранённых данных клана"
                    .formatted(TelegramUtils.escapeHtml(tag));
        }

        String name = TelegramUtils.escapeHtml(
                TelegramUtils.replaceEmojisWithSquares(member.getName()));
        String role = TelegramUtils.escapeHtml(
                ROLE_NAMES_RU.getOrDefault(member.getRole(), member.getRole()));
        String status = member.getInClan() == null
                ? "Нет данных"
                : member.getInClan() ? "В клане" : "Покинул клан";
        String league = member.getLeagueTier() == null
                ? "Нет данных"
                : TelegramUtils.escapeHtml(LEAGUE_TIER_NAMES_RU.getOrDefault(
                        member.getLeagueTier().getId(), member.getLeagueTier().getName()));

        StringBuilder message = new StringBuilder("""
                <b>%s</b>
                <code>%s</code>

                Роль: %s
                """.formatted(
                name, TelegramUtils.escapeHtml(member.getTag()), role));
        if (Boolean.TRUE.equals(member.getInClan())) {
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
        if (Boolean.FALSE.equals(member.getInClan())) {
            message.append("\n<i>Последние сохранённые данные участника</i>");
        }
        return message.toString().strip();
    }

    public String getPlayerMessage(String args) {
        if (args == null || args.isBlank()) {
            return "Используй: /player #TAG";
        }

        String input = args.strip().toUpperCase(Locale.ROOT);
        String tag = input.startsWith("#") ? input : "#" + input;

        if (!tag.matches("#[A-Z0-9]+")) {
            return "Некорректный тег. Используй: /player #TAG";
        }

        try {
            Player player = playerService.syncPlayer(tag);
            return formatPlayerMessage(player);
        } catch (PlayerNotFoundException e) {
            return "Игрок <code>%s</code> не найден"
                    .formatted(TelegramUtils.escapeHtml(tag));
        }
    }

    private String formatPlayerMessage(Player player) {
        String name = TelegramUtils.escapeHtml(
                TelegramUtils.replaceEmojisWithSquares(player.getName()));

        String league = player.getLeagueTier() == null
                ? "Нет данных"
                : TelegramUtils.escapeHtml(LEAGUE_TIER_NAMES_RU.getOrDefault(
                player.getLeagueTier().getId(),
                player.getLeagueTier().getName()));

        String warPreference = switch (
                player.getWarPreference() == null ? "" : player.getWarPreference()) {
            case "in" -> "Хочет участвовать";
            case "out" -> "Не хочет участвовать";
            default -> "Нет данных";
        };

        StringBuilder message = new StringBuilder("""
            <b>%s</b>
            <code>%s</code>

            Ратуша: %s
            Уровень игрока: %s
            Лига: %s
            Трофеи: %s · Рекорд: %s

            """.formatted(
                name,
                TelegramUtils.escapeHtml(player.getTag()),
                valueOrNoData(player.getTownHallLevel()),
                valueOrNoData(player.getExpLevel()),
                league,
                valueOrNoData(player.getTrophies()),
                valueOrNoData(player.getBestTrophies())));

        Clan clan = player.getClan();
        if (clan == null) {
            message.append("Клан: Без клана\n");
        } else {
            String clanName = TelegramUtils.escapeHtml(
                    TelegramUtils.replaceEmojisWithSquares(clan.getName()));

            String role = player.getRole() == null
                    ? "Нет данных"
                    : TelegramUtils.escapeHtml(ROLE_NAMES_RU.getOrDefault(
                    player.getRole(), player.getRole()));

            message.append("""
                <b>Клан</b>
                %s · <code>%s</code>
                Роль: %s
                """.formatted(
                    clanName,
                    TelegramUtils.escapeHtml(clan.getTag()),
                    role));
        }

        message.append("""
            Войны: %s

            <b>Боевая статистика</b>
            Звёзды войн: %s
            Победы в атаках: %s
            Победы в защите: %s

            <b>Пожертвования за сезон</b>
            Отдано: %s
            Получено: %s

            Вклад в столицу клана: %s
            """.formatted(
                warPreference,
                valueOrNoData(player.getWarStars()),
                valueOrNoData(player.getAttackWins()),
                valueOrNoData(player.getDefenseWins()),
                valueOrNoData(player.getDonations()),
                valueOrNoData(player.getDonationsReceived()),
                valueOrNoData(player.getClanCapitalContributions())));

        List<PlayerHero> heroes = player.getHeroes().stream()
                .filter(hero -> "home".equals(hero.getVillage()))
                .sorted(Comparator.comparing(PlayerHero::getName))
                .toList();

        if (!heroes.isEmpty()) {
            message.append("\n<b>Герои основной деревни</b>\n");
            for (PlayerHero hero : heroes) {
                message.append("%s: %s\n".formatted(
                        TelegramUtils.escapeHtml(hero.getName()),
                        valueOrNoData(hero.getLevel())));
            }
        }

        if (player.getBuilderHallLevel() != null
                || player.getBuilderBaseTrophies() != null
                || player.getBestBuilderBaseTrophies() != null) {
            message.append("""
                
                <b>Деревня строителя</b>
                Дом строителя: %s
                Трофеи: %s · Рекорд: %s
                """.formatted(
                    valueOrNoData(player.getBuilderHallLevel()),
                    valueOrNoData(player.getBuilderBaseTrophies()),
                    valueOrNoData(player.getBestBuilderBaseTrophies())));
        }

        return message.toString().strip();
    }

    @Transactional(readOnly = true)
    public String getClanMessage() {
        Clan clan = clanService.getClanByTag(clanTag);

        String type = CLAN_TYPES_RU.getOrDefault(clan.getType(), clan.getType());

        String warLeague = clan.getWarLeague() == null
                ? "Нет данных"
                : WAR_LEAGUE_NAMES_RU.get(clan.getWarLeague().getId());

        String location = clan.getLocation() == null
                ? "Нет данных"
                : LOCATION_NAMES_RU.get(clan.getLocation().getId());

        String description = clan.getDescription() == null
                || clan.getDescription().isBlank()
                ? "Без описания"
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
                
                Расположение: %s
                """.formatted(
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
                    location)
                .strip();
    }

    private static String valueOrNoData(Object value) {
        return value == null ? "Нет данных" : value.toString();
    }

    private static String formatWarWinRate(Clan clan) {
        Integer wins = clan.getWarWins();
        Integer ties = clan.getWarTies();
        Integer losses = clan.getWarLosses();
        if (wins == null || ties == null || losses == null) {
            return "Нет данных";
        }
        int totalWars = wins + ties + losses;
        if (totalWars == 0) {
            return "Нет сыгранных войн";
        }
        double winRate = (wins + ties) * 100.0 / totalWars;
        return "%.1f%%".formatted(winRate);
    }

    @Transactional(readOnly = true)
    public String getCurrentWarMessage() {
        War war = warService.findCurrentByClanTag(clanTag).orElse(null);
        if (war == null) {
            return "Сейчас активной войны нет";
        }

        boolean preparation = "preparation".equals(war.getState());
        String state = preparation ? "Подготовка" : "Идёт";
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
            return "Уточняется";
        }
        int starsComparison = Integer.compare(
                war.getClanStars(), war.getOpponentClanStars());
        if (starsComparison > 0) {
            return "Победа";
        }
        if (starsComparison < 0) {
            return "Поражение";
        }

        int destructionComparison = Double.compare(
                war.getClanDestructionPercentage(),
                war.getOpponentClanDestructionPercentage());
        if (destructionComparison > 0) {
            return "Победа";
        }
        if (destructionComparison < 0) {
            return "Поражение";
        }
        return "Ничья";
    }

    @Transactional(readOnly = true)
    public String attacks() {
        War war = warService.findCurrentByClanTag(clanTag).orElse(null);

        if (war == null) {
            return "Сейчас активной войны нет.";
        }

        if ("preparation".equals(war.getState())) {
            return "Война ещё не началась.";
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
    public String getRaidMessage() {
        RaidSeason season = raidSeasonService.findLatestByClanTag(clanTag).orElse(null);
        if (season == null) {
            return "Сезоны рейдов ещё не сохранены";
        }

        List<RaidSeasonParticipant> participants =
                raidSeasonParticipantService.getByRaidSeason(season);
        String participantCount = hasCompleteRaidParticipants(season, participants)
                ? Long.toString(participants.stream()
                        .filter(participant -> participant.getAttackCount() > 0)
                        .count())
                : "Уточняется";

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
            return "Сезоны рейдов ещё не сохранены";
        }

        List<RaidSeasonParticipant> participants =
                raidSeasonParticipantService.getByRaidSeason(season);
        if (!hasCompleteRaidParticipants(season, participants)) {
            return "Данные об участниках рейда пока неполные";
        }

        List<RaidSeasonClanMemberSnapshot> members =
                raidSeasonClanMemberSnapshotService.getByRaidSeason(season);
        if (members.isEmpty()) {
            return "Снимок состава клана для этого сезона не сохранён";
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
            return "Сезоны рейдов ещё не сохранены";
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
            return "Нет данных";
        }
        return switch (season.getState()) {
            case "ongoing" -> "Идёт";
            case "ended" -> "Завершён";
            default -> season.getState();
        };
    }

}
