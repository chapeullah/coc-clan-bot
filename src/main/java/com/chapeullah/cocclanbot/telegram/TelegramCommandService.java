package com.chapeullah.cocclanbot.telegram;

import com.chapeullah.cocclanbot.clan.service.ClanService;
import com.chapeullah.cocclanbot.clan.model.Clan;
import com.chapeullah.cocclanbot.member.MemberService;
import com.chapeullah.cocclanbot.member.model.Member;
import com.chapeullah.cocclanbot.player.exception.PlayerNotFoundException;
import com.chapeullah.cocclanbot.player.model.Player;
import com.chapeullah.cocclanbot.player.model.PlayerHero;
import com.chapeullah.cocclanbot.player.service.PlayerService;
import com.chapeullah.cocclanbot.raidseason.model.RaidSeason;
import com.chapeullah.cocclanbot.raidseason.model.RaidSeasonClanMemberSnapshot;
import com.chapeullah.cocclanbot.raidseason.model.RaidSeasonParticipant;
import com.chapeullah.cocclanbot.raidseason.service.RaidSeasonParticipantService;
import com.chapeullah.cocclanbot.raidseason.service.RaidSeasonClanMemberSnapshotService;
import com.chapeullah.cocclanbot.raidseason.service.RaidSeasonService;
import com.chapeullah.cocclanbot.rating.MemberStatistic;
import com.chapeullah.cocclanbot.rating.MemberWarStatistic;
import com.chapeullah.cocclanbot.rating.MemberRaidStatistic;
import com.chapeullah.cocclanbot.rating.RatingService;
import com.chapeullah.cocclanbot.telegram.dto.TelegramMessage;
import com.chapeullah.cocclanbot.telegram.dto.TelegramUser;
import com.chapeullah.cocclanbot.telegram.groupchatid.TelegramGroupChatIdService;
import com.chapeullah.cocclanbot.telegram.groupchatthreads.TelegramGroupChatThreadsService;
import com.chapeullah.cocclanbot.telegram.memberlink.MemberTelegramLinkService;
import com.chapeullah.cocclanbot.war.model.War;
import com.chapeullah.cocclanbot.war.model.WarAttack;
import com.chapeullah.cocclanbot.war.model.WarParticipant;
import com.chapeullah.cocclanbot.war.service.WarAttackService;
import com.chapeullah.cocclanbot.war.service.WarParticipantService;
import com.chapeullah.cocclanbot.war.service.WarService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.chapeullah.cocclanbot.telegram.ClashOfClansTranslations.CLAN_TYPES_RU;
import static com.chapeullah.cocclanbot.telegram.ClashOfClansTranslations.LOCATION_NAMES_RU;
import static com.chapeullah.cocclanbot.telegram.ClashOfClansTranslations.LEAGUE_TIER_NAMES_RU;
import static com.chapeullah.cocclanbot.telegram.ClashOfClansTranslations.ROLE_NAMES_RU;
import static com.chapeullah.cocclanbot.telegram.ClashOfClansTranslations.WAR_LEAGUE_NAMES_RU;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@DependsOn("environmentVariablesService")
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
    private final MemberTelegramLinkService memberTelegramLinkService;

    private final TelegramGroupChatThreadsService telegramGroupChatThreadsService;
    private final TelegramGroupChatIdService telegramGroupChatIdService;

    private final TelegramAccessService telegramAccessService;

    @Value("${coc.clan-tag}")
    private String clanTag;

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.of("Europe/Moscow"));

    @Transactional
    public String setGroup(long chatId, Long userId) {
        if (!telegramAccessService.isAdmin(userId)) {
            return "Нет доступа";
        }
        if (chatId >= 0) {
            return "Вызови команду в нужной группе";
        }
        Long previousChatId = telegramGroupChatIdService.getChatId().orElse(null);
        if (!Objects.equals(previousChatId, chatId)) {
            telegramGroupChatIdService.upsertChatId(chatId);
            telegramGroupChatThreadsService.resetThreadIds();
        }
        return "Эта группа назначена группой клана";
    }

    @Transactional
    public String setWarThread(long chatId, Long userId, Integer warThreadId) {
        if (!telegramAccessService.isAdmin(userId)) {
            return "Нет доступа";
        }
        Long groupChatId = telegramGroupChatIdService
                .getChatId()
                .orElse(null);
        if (groupChatId == null) {
            return "Группа не задана: /set_group";
        }
        if (chatId != groupChatId) {
            return "Команда доступна только в заданной группе";
        }
        if (warThreadId == null || warThreadId <= 0) {
            return "Отправь команду внутри правильной темы";
        }
        telegramGroupChatThreadsService.upsertWarThreadId(warThreadId);
        return "Эта тема назначена для уведомлений о КВ";
    }

    @Transactional
    public String setMainThread(long chatId, Long userId, Integer mainThreadId) {
        if (!telegramAccessService.isAdmin(userId)) {
            return "Нет доступа";
        }
        Long groupChatId = telegramGroupChatIdService
                .getChatId()
                .orElse(null);
        if (groupChatId == null) {
            return "Группа не задана: /set_group";
        }
        if (chatId != groupChatId) {
            return "Команда доступна только в заданной группе";
        }
        if (mainThreadId != null && mainThreadId <= 0) {
            return "Отправь команду внутри правильной темы";
        }
        telegramGroupChatThreadsService.upsertMainThreadId(mainThreadId);
        return "Этот чат назначен основным. Команды бота здесь запрещены";
    }

    @Transactional
    public String resetGroup(long chatId, Long userId) {
        if (!telegramAccessService.isAdmin(userId)) {
            return "Нет доступа";
        }
        Long groupChatId = telegramGroupChatIdService.getChatId().orElse(null);
        if (groupChatId == null) {
            return "Группа не задана";
        }
        if (chatId != groupChatId) {
            return "Команда доступна только в заданной группе";
        }
        telegramGroupChatIdService.resetChatId();
        telegramGroupChatThreadsService.resetThreadIds();
        return "Группа клана и настройки тем сброшены";
    }

    @Transactional
    public String resetWarThread(long chatId, Long userId) {
        if (!telegramAccessService.isAdmin(userId)) {
            return "Нет доступа";
        }
        Long groupChatId = telegramGroupChatIdService.getChatId().orElse(null);
        if (groupChatId == null) {
            return "Группа не задана: /set_group";
        }
        if (chatId != groupChatId) {
            return "Команда доступна только в заданной группе";
        }
        if (telegramGroupChatThreadsService.getWarThreadId().isEmpty()) {
            return "Тема уведомлений о КВ не задана";
        }
        telegramGroupChatThreadsService.resetWarThreadId();
        return "Тема уведомлений о КВ сброшена";
    }

    @Transactional
    public String resetMainThread(long chatId, Long userId) {
        if (!telegramAccessService.isAdmin(userId)) {
            return "Нет доступа";
        }
        Long groupChatId = telegramGroupChatIdService.getChatId().orElse(null);
        if (groupChatId == null) {
            return "Группа не задана: /set_group";
        }
        if (chatId != groupChatId) {
            return "Команда доступна только в заданной группе";
        }
        if (telegramGroupChatThreadsService.getMainThreadId().isEmpty()) {
            return "Основной чат не задан";
        }
        telegramGroupChatThreadsService.resetMainThreadId();
        return "Основной чат сброшен. Команды доступны во всех темах группы";
    }

    public String getLinkMemberMessage(String args, TelegramMessage message) {
        Long userId = message == null || message.telegramUser() == null
                ? null : message.telegramUser().id();
        if (!telegramAccessService.isAdmin(userId)) {
            return "Нет доступа";
        }

        String tag = normalizeMemberLinkTag(args);
        if (tag == null) {
            return "Используй: /link_member #TAG ответом на сообщение участника";
        }
        TelegramMessage reply = message.replyToMessage();
        if (reply == null || reply.telegramUser() == null) {
            return "Ответь командой /link_member #TAG на сообщение человека, которого нужно привязать";
        }
        TelegramUser targetUser = reply.telegramUser();
        if (targetUser.id() <= 0 || Boolean.TRUE.equals(targetUser.isBot())) {
            return "Нельзя привязать участника к боту. Ответь на сообщение человека";
        }

        return switch (memberTelegramLinkService.createMemberTelegramLink(tag, targetUser.id())) {
            case CREATED -> "Участник <code>%s</code> привязан к Telegram ID <code>%d</code>"
                    .formatted(tag, targetUser.id());
            case MEMBER_NOT_FOUND -> "Участник <code>%s</code> не найден в сохранённых данных клана"
                    .formatted(tag);
            case MEMBER_NOT_IN_CLAN -> "Участник <code>%s</code> больше не состоит в клане"
                    .formatted(tag);
            case ALREADY_LINKED -> "Участник <code>%s</code> уже привязан к Telegram. "
                    .formatted(tag) + "Чтобы изменить привязку, используй /unlink_member " + tag;
        };
    }

    public String getUnlinkMemberMessage(String args, TelegramMessage message) {
        Long userId = message == null || message.telegramUser() == null
                ? null : message.telegramUser().id();
        if (!telegramAccessService.isAdmin(userId)) {
            return "Нет доступа";
        }

        String tag = normalizeMemberLinkTag(args);
        if (tag == null) {
            return "Используй: /unlink_member #TAG";
        }

        return switch (memberTelegramLinkService.deleteMemberTelegramLink(tag)) {
            case DELETED -> "Привязка участника <code>%s</code> к Telegram удалена".formatted(tag);
            case NOT_LINKED -> "У участника <code>%s</code> нет привязки к Telegram".formatted(tag);
        };
    }

    private String normalizeMemberLinkTag(String args) {
        if (args == null || args.isBlank()) {
            return null;
        }
        String tag = args.strip().toUpperCase(Locale.ROOT);
        if (!tag.startsWith("#")) {
            tag = "#" + tag;
        }
        return tag.matches("#[A-Z0-9]+") ? tag : null;
    }

    public String start(TelegramMessage message) {
        String start = """
            Телеграм-бот клана <a href="https://t.me/gussicoc">GUCCИ</a> (<code>#2C020L9P0</code>) в Clash of Clans.
            Не работает с другими кланами.
            
            Используй /help, чтобы посмотреть доступные команды.
            """.strip();
        if ("private".equals(message.telegramChat().type())) {
            start += "\n\n" + getUsageNotice(message);
        }
        return start;
    }

    public String help(TelegramMessage message) {
        String help = """
                <b>Доступные команды</b>

                /start — Начало работы с ботом
                /help — Список команд
                """.strip();
        help += "\n\n" + """
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
                """.strip();
        Long userId = message.telegramUser() == null ? null : message.telegramUser().id();
        if (telegramAccessService.isAdmin(userId)) {
            help += "\n\n" + """
                    <b>Команды администратора</b>
                    /link_member #TAG — Привязать участника к Telegram ответом на его сообщение
                    /unlink_member #TAG — Удалить привязку участника к Telegram
                    /set_group — Назначить текущую группу группой клана
                    /set_war_thread — Назначить текущую тему для уведомлений
                    /set_main_thread — Назначить текущий чат основным и запретить в нём команды
                    /reset_group — Сбросить группу клана и все настройки тем
                    /reset_war_thread — Сбросить тему уведомлений о КВ
                    /reset_main_thread — Сбросить основной чат и разрешить в нём команды
                    """.strip();
        }
        if ("private".equals(message.telegramChat().type())) {
            help += "\n\n" + getUsageNotice(message);
        }
        return help;
    }

    private boolean isLimitedPrivateChat(TelegramMessage message) {
        Long userId = message.telegramUser() == null ? null : message.telegramUser().id();
        return "private".equals(message.telegramChat().type()) && !telegramAccessService.isAdmin(userId);
    }

    private String getUsageNotice(TelegramMessage message) {
        String privacyNotice = "<i>В группе ваши команды и ответы бота видны только вам.</i>";
        if (isLimitedPrivateChat(message)) {
            return "В личных сообщениях работают только /start и /help.\n"
                    + "Остальные команды используй в группе <a href=\"https://t.me/gussicoc\">ГУССИ</a>.\n\n" + privacyNotice;
        }
        return privacyNotice;
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
            message.append("\n");
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
            return "Список участников клана пуст";
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
                .orElse("Нет данных");
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
