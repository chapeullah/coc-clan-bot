package com.chapeullah.cocclanbot.war;

import com.chapeullah.cocclanbot.war.dto.WarClanResponse;
import com.chapeullah.cocclanbot.war.dto.WarResponse;
import com.chapeullah.cocclanbot.war.model.War;
import com.chapeullah.cocclanbot.war.model.WarLogEntry;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.ZoneOffset;
import java.util.List;

public final class WarFixtures {

    public static final String CLAN = "#2C020L9P0";
    public static final String OPPONENT = "#R8QQUQVG";
    public static final Instant PREPARATION = Instant.parse("2026-10-19T08:03:23Z");
    public static final Instant ORIGINAL_START = Instant.parse("2026-10-20T07:03:23Z");
    public static final Instant ORIGINAL_END = Instant.parse("2026-10-21T07:03:23Z");
    public static final Instant SHIFTED_START = Instant.parse("2026-10-20T08:35:31Z");
    public static final Instant SHIFTED_END = Instant.parse("2026-10-21T08:35:31Z");
    public static final Instant FINAL_END = Instant.parse("2026-10-21T09:27:26Z");
    public static final Instant NEXT_PREPARATION = Instant.parse("2026-10-21T10:06:05Z");
    public static final Instant NOW = Instant.parse("2026-10-23T09:00:00Z");
    private static final DateTimeFormatter API_TIME = DateTimeFormatter
            .ofPattern("uuuuMMdd'T'HHmmss.SSSX").withZone(ZoneOffset.UTC);

    private WarFixtures() {
    }

    public static War war(Instant start, Instant end) {
        return new War(CLAN, "GUCCI", 32, 60, 100.0, 10,
                OPPONENT, "Opponent", 31, 60, 100.0, 28,
                "warEnded", 20, 2, "none", PREPARATION, start, end);
    }

    public static War nextWar() {
        return new War(CLAN, "GUCCI", 0, 0, 0.0, 10,
                "#NEXT", "Next opponent", 0, 0, 0.0, 14,
                "preparation", 20, 2, "none", NEXT_PREPARATION,
                Instant.parse("2026-10-22T09:06:05Z"), Instant.parse("2026-10-23T09:06:05Z"));
    }

    public static WarResponse response(String state, Instant preparation, Instant start, Instant end) {
        return new WarResponse(state, 20, 2, "none", API_TIME.format(preparation),
                API_TIME.format(start), API_TIME.format(end),
                new WarClanResponse(CLAN, "GUCCI", null, 10, 32, 60, 100.0, List.of()),
                new WarClanResponse(OPPONENT, "Opponent", null, 28, 31, 60, 100.0, List.of()));
    }

    public static WarLogEntry finalEntry(Instant end, Integer attacks, String opponent) {
        return new WarLogEntry("tie", end, 20, 2, "none",
                CLAN, "GUCCI", 10, attacks, 60, 100.0, 0, null,
                opponent, "Opponent", 28, 60, 100.0, null);
    }
}
