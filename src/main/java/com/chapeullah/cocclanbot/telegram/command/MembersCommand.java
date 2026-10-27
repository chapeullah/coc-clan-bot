package com.chapeullah.cocclanbot.telegram.command;

import com.chapeullah.cocclanbot.config.properties.CocProperties;
import com.chapeullah.cocclanbot.member.MemberService;
import com.chapeullah.cocclanbot.member.model.Member;
import com.chapeullah.cocclanbot.rating.MemberStatistic;
import com.chapeullah.cocclanbot.rating.RatingService;
import com.chapeullah.cocclanbot.telegram.dto.TelegramMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class MembersCommand implements BotCommand {

    private static final String NAME = "/members";
    private static final String DESCRIPTION = "Текущий состав клана";
    private static final BotCommandType TYPE = BotCommandType.CLAN;
    private static final boolean ADMIN_ONLY = false;

    private final BotCommandMessageService botCommandMessageService;
    private final MemberService memberService;
    private final RatingService ratingService;

    private final CocProperties cocProperties;

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESCRIPTION; }
    @Override public BotCommandType getType() { return TYPE; }
    @Override public boolean isAdminOnly() { return ADMIN_ONLY; }

    @Override
    public String execute(String args, TelegramMessage message) {
        String clanTag = cocProperties.getClanTag();
        List<Member> members = memberService.getAllMembersByClanTag(clanTag);
        if (members.isEmpty()) {
            return "Список участников клана пуст";
        }

        Map<String, MemberStatistic> statisticsByTag = ratingService.calculateClanMemberStatistics(clanTag);
        return botCommandMessageService.getMembersMessage(members, statisticsByTag);
    }

}
