package com.chapeullah.cocclanbot.telegram.command;

import com.chapeullah.cocclanbot.clan.model.Clan;
import com.chapeullah.cocclanbot.clan.service.ClanService;
import com.chapeullah.cocclanbot.config.properties.CocProperties;
import com.chapeullah.cocclanbot.telegram.dto.TelegramMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ClanCommand implements BotCommand {

    private static final String NAME = "/clan";
    private static final String DESCRIPTION = "Информация о клане";
    private static final BotCommandType TYPE = BotCommandType.CLAN;
    private static final boolean ADMIN_ONLY = false;

    private final BotCommandMessageService botCommandMessageService;
    private final ClanService clanService;

    private final CocProperties cocProperties;

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESCRIPTION; }
    @Override public BotCommandType getType() { return TYPE; }
    @Override public boolean isAdminOnly() { return ADMIN_ONLY; }

    @Override
    @Transactional(readOnly = true)
    public String execute(String args, TelegramMessage message) {
        Clan clan = clanService.getClanByTag(cocProperties.getClanTag());
        return botCommandMessageService.getClanMessage(clan);
    }

}
