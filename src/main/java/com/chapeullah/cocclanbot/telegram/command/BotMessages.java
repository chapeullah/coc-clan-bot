package com.chapeullah.cocclanbot.telegram.command;

import com.chapeullah.cocclanbot.leaguetier.LeagueTier;
import com.chapeullah.cocclanbot.location.Location;
import com.chapeullah.cocclanbot.raidseason.model.RaidSeason;
import com.chapeullah.cocclanbot.raidseason.model.RaidSeasonClanMemberSnapshot;
import com.chapeullah.cocclanbot.telegram.TelegramUtils;
import com.chapeullah.cocclanbot.war.model.War;
import com.chapeullah.cocclanbot.warleague.WarLeague;
import lombok.NonNull;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static com.chapeullah.cocclanbot.ClashOfClansTranslations.CLAN_TYPES_RU;
import static com.chapeullah.cocclanbot.ClashOfClansTranslations.LEAGUE_TIER_NAMES_RU;
import static com.chapeullah.cocclanbot.ClashOfClansTranslations.LOCATION_NAMES_RU;
import static com.chapeullah.cocclanbot.ClashOfClansTranslations.ROLE_NAMES_RU;
import static com.chapeullah.cocclanbot.ClashOfClansTranslations.WAR_LEAGUE_NAMES_RU;

public final class BotMessages {

    public static final String ERROR_EMOJI = "❌";
    public static final String SUCCESS_EMOJI = "✅";
    public static final String SPACE = " ";

    public static final String NO_DATA = "Нет данных";
    public static final String DATA_PENDING = "Уточняется";
    public static final String IN_PROGRESS = "Идёт";
    public static final String NO_MEMBERS = "Список участников клана пуст";
    public static final String NO_RAID_SEASONS = "Сезоны рейдов ещё не сохранены";
    public static final String NO_ACTIVE_WAR = "Сейчас активной войны нет";
    public static final String NO_COMPLETED_WARS = "Завершённые войны не найдены";
    public static final String WAR_VICTORY = "Победа";
    public static final String WAR_DEFEAT = "Поражение";
    public static final String WAR_DRAW = "Ничья";
    public static final String WAR_NOT_STARTED = ERROR_EMOJI + SPACE + "Война ещё не началась.";
    public static final String ALL_ATTACKS_USED = "Все участники использовали свои атаки.";

    public static final String ACCESS_DENIED = ERROR_EMOJI + SPACE + "Нет доступа";
    public static final String GROUP_NOT_SET = ERROR_EMOJI + SPACE + "Группа не задана: /set_group";
    public static final String WRONG_GROUP = ERROR_EMOJI + SPACE + "Команда доступна только в заданной группе.";
    public static final String INVALID_THREAD = ERROR_EMOJI + SPACE + "Отправь команду внутри правильной темы.";
    public static final String SET_GROUP_PRIVATE_CHAT = ERROR_EMOJI + SPACE + "Вызови команду в нужной группе";
    public static final String GROUP_SET = SUCCESS_EMOJI + SPACE + "Эта группа назначена группой клана";
    public static final String GROUP_RESET = SUCCESS_EMOJI + SPACE + "Группа клана и настройки тем сброшены";
    public static final String WAR_THREAD_SET = SUCCESS_EMOJI + SPACE + "Этот чат назначен для уведомлений о КВ.";
    public static final String WAR_THREAD_NOT_SET = ERROR_EMOJI + SPACE + "Тема уведомлений о КВ не задана";
    public static final String WAR_THREAD_RESET = SUCCESS_EMOJI + SPACE + "Тема уведомлений о КВ сброшена.";
    public static final String MAIN_THREAD_SET = SUCCESS_EMOJI + SPACE + "Этот чат назначен основным.";
    public static final String MAIN_THREAD_NOT_SET = ERROR_EMOJI + SPACE + "Основной чат не задан";
    public static final String MAIN_THREAD_RESET = SUCCESS_EMOJI + SPACE + "Основной чат сброшен.";

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter
            .ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.of("Europe/Moscow"));

    public static final String USAGE_NOTICE = """
            В личных сообщениях доступны только /start и /help.
            Остальные команды используйте в группе <a href="https://t.me/gussicoc">ГУССИ</a>.
            
            <i>В группе команды и ответы бота видны только Вам.</i>""";

    public static final String MEMBER_TAG_REQUIRED =
            ERROR_EMOJI + SPACE + "Не указан тег участника.\nФормат команды: /member #TAG";
    public static final String INVALID_MEMBER_TAG_FORMAT =
            ERROR_EMOJI + SPACE + "Некорректный формат тега участника.\nФормат тега: #ABC123";
    public static final String MEMBER_NOT_FOUND = ERROR_EMOJI + SPACE + "Участник не найден";

    public static final String RAID_NOT_FOUND =
            ERROR_EMOJI + SPACE + "Рейды не найдены.";
    public static final String RAID_ATTACK_DATA_INCONSISTENT =
            ERROR_EMOJI + SPACE + "Данные об атаках рейда неполные или несогласованные.";
    public static final String RAID_SNAPSHOT_NOT_FOUND =
            ERROR_EMOJI + SPACE + "Снимок состава клана для этого сезона рейдов не найден.";

    private static final String START_MESSAGE = """
            Телеграм-бот клана <a href="https://t.me/gussicoc">GUCCИ</a> (<code>#2C020L9P0</code>) в Clash of Clans.
            Не работает с другими кланами.

            Используй /help, чтобы посмотреть доступные команды.""";

    private static String valueOrNoData(Object value) {
        return value == null ? NO_DATA : value.toString();
    }

    public static String formatClanMessage(
            @NonNull String name,
            @NonNull String tag,
            String description,
            Integer clanLevel,
            Integer memberCount,
            String type,
            Integer clanPoints,
            Integer clanCapitalPoint,
            WarLeague warLeague,
            Double warWinRate,
            Integer warWins,
            Integer warWinStreak,
            Location location) {
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
                        TelegramUtils.escapeHtml(name),
                        TelegramUtils.escapeHtml(tag),
                        formatDescription(description),
                        valueOrNoData(clanLevel),
                        valueOrNoData(memberCount),
                        formatClanType(type, tag),
                        valueOrNoData(clanPoints),
                        valueOrNoData(clanCapitalPoint),
                        formatWarLeague(warLeague, tag),
                        formatWarWinRate(warWinRate),
                        valueOrNoData(warWins),
                        valueOrNoData(warWinStreak),
                        formatLocation(location, tag));
    }

    private static String formatDescription(String description) {
        return description == null || description.isBlank()
                ? "Без описания"
                : TelegramUtils.escapeHtml(description);
    }

    private static String formatClanType(String type, String clanTag) {
        if (type == null) {
            throw new IllegalStateException("Clan type is null: clanTag=\"" + clanTag + "\"");
        }
        return formatTranslation(CLAN_TYPES_RU, type,
                "Missing clan type translation: type=\"" + type + "\", clanTag=\"" + clanTag + "\"");
    }

    private static String formatWarLeague(WarLeague warLeague, String clanTag) {
        if (warLeague == null) {
            return NO_DATA;
        }
        Integer id = warLeague.getId();
        return formatTranslation(WAR_LEAGUE_NAMES_RU, id,
                "Missing war league translation: warLeagueId=" + id + ", clanTag=\"" + clanTag + "\"");
    }

    private static String formatLocation(Location location, String clanTag) {
        if (location == null) {
            return NO_DATA;
        }
        Integer id = location.getId();
        return formatTranslation(LOCATION_NAMES_RU, id,
                "Missing location translation: locationId=" + id + ", clanTag=\"" + clanTag + "\"");
    }

    private static String formatWarWinRate(Double warWinRate) {
        return warWinRate == null
                ? NO_DATA
                : "%.2f%%".formatted(warWinRate);
    }

    public static String formatMemberMessage(
            @NonNull String name,
            @NonNull String tag,
            String role,
            Boolean inClan,
            Integer clanRank,
            Integer townHallLevel,
            Integer expLevel,
            LeagueTier leagueTier,
            Integer trophies,
            Integer builderBaseTrophies,
            Integer donations,
            Integer donationsReceived,
            Integer totalDonations,
            Integer totalDonationsReceived) {
        String formattedName = TelegramUtils.escapeHtml(TelegramUtils.replaceEmojisWithSquares(name));
        String formattedRole = formatTranslation(ROLE_NAMES_RU, role,
                "Missing member role translation: role=\"" + role + "\", memberTag=\"" + tag + "\"");
        String league = formatLeagueTier(leagueTier, tag);

        StringBuilder message = new StringBuilder("""
                <b>%s</b>
                <code>%s</code>

                Роль: %s
                """.formatted(formattedName, TelegramUtils.escapeHtml(tag), formattedRole));
        if (Boolean.TRUE.equals(inClan)) {
            message.append("Место в клане: %s\n".formatted(valueOrNoData(clanRank)));
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
                Получено: %s""".formatted(
                valueOrNoData(townHallLevel),
                valueOrNoData(expLevel),
                league,
                valueOrNoData(trophies),
                valueOrNoData(builderBaseTrophies),
                valueOrNoData(donations),
                valueOrNoData(donationsReceived),
                valueOrNoData(totalDonations),
                valueOrNoData(totalDonationsReceived)));
        if (Boolean.FALSE.equals(inClan)) {
            message.append("\n\n<i>Последние сохранённые данные участника</i>");
        }
        return message.toString();
    }

    private static String formatLeagueTier(LeagueTier leagueTier, String memberTag) {
        if (leagueTier == null) {
            return NO_DATA;
        }
        Integer id = leagueTier.getId();
        return formatTranslation(LEAGUE_TIER_NAMES_RU, id,
                "Missing league tier translation: leagueTierId=" + id + ", memberTag=\"" + memberTag + "\"");
    }

    private static <K> String formatTranslation(Map<K, String> translations, K key, String errorMessage) {
        String translation = key == null ? null : translations.get(key);
        if (translation == null) {
            throw new IllegalStateException(errorMessage);
        }
        return TelegramUtils.escapeHtml(translation);
    }

    public static String formatMemberListItem(
            Integer clanRank,
            @NonNull String name,
            @NonNull String tag,
            Double rating) {
        String formattedName = TelegramUtils.escapeHtml(TelegramUtils.replaceEmojisWithSquares(name));
        String ratingText = rating == null ? NO_DATA : String.format(Locale.ROOT, "%.2f", rating);
        return "<b>%02d. %s</b> | <code>%s</code> | %s".formatted(
                clanRank,
                formattedName,
                TelegramUtils.escapeHtml(tag),
                ratingText);
    }

    public static String formatMembersMessage(@NonNull List<String> memberLines) {
        return ("<b>Участники клана</b>\n\n" + String.join("\n", memberLines)).strip();
    }

    public static String formatRaidMessage(
            String state,
            @NonNull Instant startTime,
            @NonNull Instant endTime,
            Integer capitalTotalLoot,
            Integer totalAttacks,
            Long participantCount,
            Integer raidsCompleted,
            Integer enemyDistrictsDestroyed) {
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
                "ongoing".equals(state) ? "Текущий сезон рейдов" : "Последний сезон рейдов",
                TelegramUtils.escapeHtml(formatRaidState(state)),
                formatRaidPeriod(startTime, endTime),
                valueOrNoData(capitalTotalLoot),
                valueOrNoData(totalAttacks),
                participantCount == null ? DATA_PENDING : participantCount.toString(),
                valueOrNoData(raidsCompleted),
                valueOrNoData(enemyDistrictsDestroyed)).strip();
    }

    public static String formatRaidMissedMessage(
            String state,
            @NonNull Instant startTime,
            @NonNull Instant endTime,
            @NonNull List<RaidSeasonClanMemberSnapshot> membersWithoutAttacks) {
        StringBuilder message = new StringBuilder("""
                <b>Участники без атак в рейдах</b>

                Период: %s (МСК)
                Статус: %s
                Состав клана, сохранённый в течение сезона
                Подтверждённо недопущенные участники исключены

                """.formatted(
                formatRaidPeriod(startTime, endTime),
                TelegramUtils.escapeHtml(formatRaidState(state))));

        int number = 1;
        for (RaidSeasonClanMemberSnapshot member : membersWithoutAttacks) {
            String name = TelegramUtils.escapeHtml(TelegramUtils.replaceEmojisWithSquares(member.getName()));
            message.append("%02d. %s | <code>%s</code>\n".formatted(
                    number++,
                    name,
                    TelegramUtils.escapeHtml(member.getTag())));
        }
        if (membersWithoutAttacks.isEmpty()) {
            message.append("Среди учитываемых участников нет игроков без атак");
        }
        return message.toString().strip();
    }

    public static String formatRaidPeriod(@NonNull Instant startTime, @NonNull Instant endTime) {
        return DATE_TIME_FORMATTER.format(startTime) + " — " + DATE_TIME_FORMATTER.format(endTime);
    }

    public static String formatRaidState(String state) {
        if (state == null) {
            return NO_DATA;
        }
        return switch (state) {
            case "ongoing" -> IN_PROGRESS;
            case "ended" -> "Завершён";
            default -> state;
        };
    }

    public static String formatStartMessage(boolean isPrivateChat) {
        return isPrivateChat ? START_MESSAGE + "\n\n" + USAGE_NOTICE : START_MESSAGE;
    }

    public static String formatRatingListItem(int number, @NonNull String memberName, Double rating) {
        String name = TelegramUtils.escapeHtml(
                TelegramUtils.replaceEmojisWithSquares(memberName)
                        .replaceAll("(?U)\\s+", " ")
                        .strip());
        String ratingText = rating == null ? NO_DATA : String.format(Locale.ROOT, "%.2f", rating);
        return "<b>%02d. %s</b> — %s".formatted(number, name, ratingText);
    }

    public static String formatWarRatingListItem(
            int number,
            @NonNull String memberName,
            Double rating,
            Double averageStars,
            Double averageDestruction,
            Double averageAttackCount) {
        String line = formatRatingListItem(number, memberName, rating);
        if (rating == null) {
            return line;
        }
        String details = averageAttackCount == null
                ? NO_DATA
                : "%s | %s | АТ %s".formatted(
                        formatStatisticValue(averageStars, "%.2f★"),
                        formatStatisticValue(averageDestruction, "%.1f%%"),
                        formatStatisticValue(averageAttackCount, "%.2f"));
        return line + " | " + details;
    }

    public static String formatRaidRatingListItem(
            int number,
            @NonNull String memberName,
            Double rating,
            Double averageLootPerAttack,
            Double averageAttacksPerSeason) {
        String line = formatRatingListItem(number, memberName, rating);
        if (rating == null) {
            return line;
        }
        String details = averageLootPerAttack == null
                ? NO_DATA
                : "%s | АТ %s".formatted(
                        formatStatisticValue(averageLootPerAttack, "%.0f/ат."),
                        formatStatisticValue(averageAttacksPerSeason, "%.2f"));
        return line + " | " + details;
    }

    private static String formatStatisticValue(Double value, String format) {
        return value == null ? "-" : String.format(Locale.ROOT, format, value);
    }

    public static String formatRatingMessage(@NonNull List<String> lines) {
        return ("<b>Рейтинг участников</b>\n\n" + String.join("\n", lines)).strip();
    }

    public static String formatWarRatingMessage(@NonNull List<String> lines) {
        return ("""
                <b>Рейтинг КВ</b>
                Средние значения за атаку в КВ.
                АТ — атаки.

                """ + String.join("\n", lines)).strip();
    }

    public static String formatRaidRatingMessage(@NonNull List<String> lines) {
        return ("""
                <b>Рейтинг рейдов</b>
                Средние значения за атаку в рейде.
                АТ — атаки.

                """ + String.join("\n", lines)).strip();
    }

    public static String formatCurrentWarMessage(@NonNull War war, int maximumAttacks) {
        boolean preparation = "preparation".equals(war.getState());
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
                preparation ? "Подготовка" : IN_PROGRESS,
                war.getTeamSize(), war.getTeamSize(),
                war.getClanStars(), war.getOpponentClanStars(),
                war.getClanDestructionPercentage(), war.getOpponentClanDestructionPercentage(),
                war.getClanAttacks(), maximumAttacks,
                war.getOpponentClanAttacks(), maximumAttacks,
                preparation ? "Начало" : "Окончание",
                DATE_TIME_FORMATTER.format(preparation ? war.getStartTime() : war.getEndTime())).strip();
    }

    public static String formatWarHistoryItem(@NonNull War war, @NonNull String result) {
        return """
                <b>%s</b> — <b>%s</b>
                Результат: %s
                Звёзды: %d — %d
                Разрушение: %.2f%% — %.2f%%
                Завершена: %s (МСК)
                """.formatted(
                TelegramUtils.escapeHtml(war.getClanName()),
                TelegramUtils.escapeHtml(war.getOpponentClanName()),
                result,
                war.getClanStars(), war.getOpponentClanStars(),
                war.getClanDestructionPercentage(), war.getOpponentClanDestructionPercentage(),
                DATE_TIME_FORMATTER.format(war.getEndTime())).strip();
    }

    public static String formatWarsMessage(@NonNull List<String> lines) {
        return ("<b>История войн</b>\n\n" + String.join("\n\n", lines)).strip();
    }

    public static String formatRaidsMessage(@NonNull List<RaidSeason> raidSeasons) {
        StringBuilder message = new StringBuilder("<b>История рейдов</b>\n\n");
        for (RaidSeason season : raidSeasons) {
            message.append("""
                    <b>%s</b> (МСК)
                    Статус: %s
                    Столичное золото: %s
                    Атаки: %s
                    Завершено рейдов: %s
                    Уничтожено районов: %s

                    """.formatted(
                    formatRaidPeriod(season.getStartTime(), season.getEndTime()),
                    TelegramUtils.escapeHtml(formatRaidState(season.getState())),
                    valueOrNoData(season.getCapitalTotalLoot()),
                    valueOrNoData(season.getTotalAttacks()),
                    valueOrNoData(season.getRaidsCompleted()),
                    valueOrNoData(season.getEnemyDistrictsDestroyed())));
        }
        return message.toString().strip();
    }

    public static String formatUnusedAttackListItem(
            Integer mapPosition, @NonNull String playerName, int remaining, int attacksPerMember) {
        return "%s. %s — %s из %s".formatted(
                mapPosition, TelegramUtils.escapeHtml(playerName), remaining, attacksPerMember);
    }

    public static String formatUnusedAttacksMessage(@NonNull List<String> lines) {
        return "<b>Неиспользованные атаки</b>\n\n" + String.join("\n", lines);
    }

    public static String formatHelpMessage(
            @NonNull List<List<BotCommand>> commandGroups,
            boolean isPrivateChat) {
        StringBuilder message = new StringBuilder("<b>Доступные команды</b>\n\n");
        message.append(commandGroups.stream()
                .map(commands -> commands.stream()
                        .map(command -> command.getName() + " — " + command.getDescription())
                        .collect(Collectors.joining("\n")))
                .collect(Collectors.joining("\n\n")));
        if (isPrivateChat) {
            message.append("\n\n").append(USAGE_NOTICE);
        }
        return message.toString();
    }

}
