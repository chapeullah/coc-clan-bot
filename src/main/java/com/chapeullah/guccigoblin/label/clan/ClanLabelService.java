package com.chapeullah.guccigoblin.label.clan;

import com.chapeullah.guccigoblin.ClashOfClansClient;
import com.chapeullah.guccigoblin.label.IconUrls;
import com.chapeullah.guccigoblin.label.dto.LabelIconUrlsResponse;
import com.chapeullah.guccigoblin.label.dto.LabelResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ClanLabelService {

    private final ClashOfClansClient client;

    private final ClanLabelRepository clanLabelRepository;

    @Transactional(readOnly = true)
    public Optional<ClanLabel> findById(@NonNull Integer id) {
        return clanLabelRepository.findById(id);
    }

    @Transactional
    public void syncAll() {
        clanLabelRepository.saveAll(client.getClanLabelsResponse().items()
                .stream()
                .map(this::toClanLabel)
                .toList());
    }

    private ClanLabel toClanLabel(LabelResponse response) {
        return new ClanLabel(
                response.id(),
                response.name(),
                this.toIconUrls(response.iconUrls()));
    }

    private IconUrls toIconUrls(LabelIconUrlsResponse response) {
        return new IconUrls(response.small(), response.medium());
    }

}
