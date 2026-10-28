package com.chapeullah.cocclanbot.telegram.command;

import com.chapeullah.cocclanbot.config.properties.CocProperties;
import com.chapeullah.cocclanbot.raidseason.model.RaidSeason;
import com.chapeullah.cocclanbot.raidseason.model.RaidSeasonParticipant;
import com.chapeullah.cocclanbot.raidseason.service.RaidSeasonParticipantService;
import com.chapeullah.cocclanbot.raidseason.service.RaidSeasonService;
import com.chapeullah.cocclanbot.telegram.dto.TelegramMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RaidCommand implements BotCommand {

    private static final String NAME = "/raid";
    private static final String DESCRIPTION = "Текущий сезон рейдов";
    private static final BotCommandType TYPE = BotCommandType.RAID;
    private static final boolean ADMIN_ONLY = false;

    private final BotCommandMessageService botCommandMessageService;
    private final RaidSeasonService raidSeasonService;
    private final RaidSeasonParticipantService raidSeasonParticipantService;

    private final CocProperties cocProperties;

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESCRIPTION; }
    @Override public BotCommandType getType() { return TYPE; }
    @Override public boolean isAdminOnly() { return ADMIN_ONLY; }

    @Override
    public String execute(String args, TelegramMessage message) {
        String clanTag = cocProperties.getClanTag();
        RaidSeason raidSeason = raidSeasonService.findLatestByClanTag(clanTag).orElse(null);
        if (raidSeason == null) {
            return "Сезоны рейдов ещё не сохранены";
        }
        List<RaidSeasonParticipant> participants = raidSeasonParticipantService.getByRaidSeason(raidSeason);
        return botCommandMessageService.getRaidMessage(raidSeason, participants);
    }

}
