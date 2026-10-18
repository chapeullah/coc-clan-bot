package com.chapeullah.guccigoblin.label.player;

import com.chapeullah.guccigoblin.ClashOfClansClient;
import com.chapeullah.guccigoblin.label.IconUrls;
import com.chapeullah.guccigoblin.label.dto.LabelIconUrlsResponse;
import com.chapeullah.guccigoblin.label.dto.LabelResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlayerLabelService {

    private final ClashOfClansClient client;

    private final PlayerLabelRepository playerLabelRepository;

    @Transactional
    public List<PlayerLabel> syncAll() {
        return playerLabelRepository.saveAll(client.getPlayerLabelsResponse().items()
                .stream()
                .map(this::toPlayerLabel)
                .toList());
    }

    private PlayerLabel toPlayerLabel(LabelResponse response) {
        return new PlayerLabel(
                response.id(),
                response.name(),
                this.toIconUrls(response.iconUrls()));
    }

    private IconUrls toIconUrls(LabelIconUrlsResponse response) {
        return new IconUrls(response.small(), response.medium());
    }

}
