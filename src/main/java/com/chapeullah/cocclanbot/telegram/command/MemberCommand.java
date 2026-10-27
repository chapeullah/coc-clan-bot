package com.chapeullah.cocclanbot.telegram.command;

import com.chapeullah.cocclanbot.config.properties.CocProperties;
import com.chapeullah.cocclanbot.member.MemberService;
import com.chapeullah.cocclanbot.member.model.Member;
import com.chapeullah.cocclanbot.telegram.dto.TelegramMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Component
@RequiredArgsConstructor
public class MemberCommand implements BotCommand {

    private static final String NAME = "/member";
    private static final String DESCRIPTION = "Информация об участнике клана";
    private static final BotCommandType TYPE = BotCommandType.CLAN;
    private static final boolean ADMIN_ONLY = false;

    private final BotCommandMessageService botCommandMessageService;
    private final MemberService memberService;

    private final CocProperties cocProperties;

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESCRIPTION; }
    @Override public BotCommandType getType() { return TYPE; }
    @Override public boolean isAdminOnly() { return ADMIN_ONLY; }

    @Override
    @Transactional(readOnly = true)
    public String execute(String args, TelegramMessage message) {
        if (args == null || args.isBlank()) {
            return "❌ Не указан тег участника.\nФормат команды: /member #TAG";
        }

        String tag = args.strip().toUpperCase(Locale.ROOT);
        if (!tag.startsWith("#")) {
            tag = "#" + tag;
        }
        if (!tag.matches("#[A-Z0-9]+")) {
            return "❌ Некорректный формат тега участника.\nФормат команды: /member #TAG";
        }

        Member member = memberService.findByTagAndClanTag(tag, cocProperties.getClanTag()).orElse(null);
        if (member == null) {
            return "❌ Участник с тегом " + tag + " не найден в клане.";
        }

        return botCommandMessageService.getMemberMessage(member);
    }

}
