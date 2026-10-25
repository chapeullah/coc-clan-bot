package com.chapeullah.cocclanbot.telegram.memberlink;

import com.chapeullah.cocclanbot.member.MemberService;
import com.chapeullah.cocclanbot.member.model.Member;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberTelegramLinkService {

    @Value("${coc.clan-tag}")
    private String clanTag;

    private final MemberService memberService;

    private final MemberTelegramLinkRepository memberTelegramLinkRepository;

    @Transactional(readOnly = true)
    public boolean hasMemberTelegramLinks(@NonNull Long telegramUserId) {
        return memberTelegramLinkRepository.existsByTelegramUserId(telegramUserId);
    }

    @Transactional(readOnly = true)
    public List<MemberTelegramLink> getMemberTelegramLinks(
            @NonNull Long telegramUserId) {
        return memberTelegramLinkRepository.findAllByTelegramUserId(telegramUserId);
    }

    @Transactional
    public MemberTelegramLinkCreateResult createMemberTelegramLink(
            @NonNull String memberTag,
            @NonNull Long telegramUserId) {
        Member member = memberService
                .findByTagAndClanTag(memberTag, clanTag)
                .orElse(null);
        if (member == null) {
            return MemberTelegramLinkCreateResult.MEMBER_NOT_FOUND;
        }
        if (Boolean.FALSE.equals(member.getInClan())) {
            return MemberTelegramLinkCreateResult.MEMBER_NOT_IN_CLAN;
        }
        if (memberTelegramLinkRepository.existsById(memberTag)) {
            return MemberTelegramLinkCreateResult.ALREADY_LINKED;
        }
        memberTelegramLinkRepository.save(new MemberTelegramLink(memberTag, telegramUserId));
        return MemberTelegramLinkCreateResult.CREATED;
    }

    @Transactional
    public MemberTelegramLinkDeleteResult deleteMemberTelegramLink(@NonNull String memberTag) {
        return memberTelegramLinkRepository.deleteByMemberTag(memberTag) > 0
                ? MemberTelegramLinkDeleteResult.DELETED
                : MemberTelegramLinkDeleteResult.NOT_LINKED;
    }

}
