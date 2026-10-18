package com.chapeullah.guccigoblin.clan.service;

import com.chapeullah.guccigoblin.clan.repository.ClanChatLanguageRepository;
import com.chapeullah.guccigoblin.clan.dto.ClanChatLanguageResponse;
import com.chapeullah.guccigoblin.clan.model.ClanChatLanguage;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClanChatLanguageService {

    private final ClanChatLanguageRepository clanChatLanguageRepository;

    @Transactional
    public ClanChatLanguage getOrCreate(
            @NonNull ClanChatLanguageResponse response) {
        return clanChatLanguageRepository
                .findById(response.id())
                .orElseGet(() -> clanChatLanguageRepository.save(
                        new ClanChatLanguage(
                                response.id(),
                                response.name(),
                                response.languageCode())));
    }

}
