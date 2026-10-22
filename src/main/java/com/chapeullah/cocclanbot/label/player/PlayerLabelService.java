package com.chapeullah.cocclanbot.label.player;

import com.chapeullah.cocclanbot.ClashOfClansClient;
import com.chapeullah.cocclanbot.label.IconUrls;
import com.chapeullah.cocclanbot.label.dto.LabelIconUrlsResponse;
import com.chapeullah.cocclanbot.label.dto.LabelResponse;
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
