package com.chapeullah.cocclanbot.telegram.command;

import com.chapeullah.cocclanbot.config.properties.CocProperties;
import com.chapeullah.cocclanbot.raidseason.model.RaidSeason;
import com.chapeullah.cocclanbot.raidseason.service.RaidSeasonService;
import com.chapeullah.cocclanbot.telegram.dto.TelegramMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RaidsCommand implements BotCommand {

    private static final String NAME = "/raids";
    private static final String DESCRIPTION = "История рейдов";
    private static final BotCommandType TYPE = BotCommandType.RAID;
    private static final boolean ADMIN_ONLY = false;

    private final BotCommandMessageService botCommandMessageService;
    private final RaidSeasonService raidSeasonService;

    private final CocProperties cocProperties;

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESCRIPTION; }
    @Override public BotCommandType getType() { return TYPE; }
    @Override public boolean isAdminOnly() { return ADMIN_ONLY; }

    @Override
    public String execute(String args, TelegramMessage message) {
        String clanTag = cocProperties.getClanTag();

        List<RaidSeason> raidSeasons = raidSeasonService.getRecentByClanTag(clanTag);
        if (raidSeasons.isEmpty()) {
            return "❌ Рейды не найдены.";
        }

        return botCommandMessageService.getRaidsMessage(raidSeasons);
    }

}
