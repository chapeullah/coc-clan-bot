package com.chapeullah.guccigoblin.builderbaseleague;

import com.chapeullah.guccigoblin.ClashOfClansClient;
import com.chapeullah.guccigoblin.builderbaseleague.dto.BuilderBaseLeagueResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BuilderBaseLeagueService {

    private final ClashOfClansClient client;

    private final BuilderBaseLeagueRepository builderBaseLeagueRepository;

    @Transactional(readOnly = true)
    public Optional<BuilderBaseLeague> findById(@NonNull Integer id) {
        return builderBaseLeagueRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public BuilderBaseLeague getById(@NonNull Integer id) {
        return builderBaseLeagueRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Builder base league not found: id=" + id));
    }

    @Transactional
    public List<BuilderBaseLeague> syncAll() {
        return builderBaseLeagueRepository.saveAll(client.getBuilderBaseLeagues().items()
                .stream()
                .map(this::toBuilderBaseLeague)
                .toList());
    }

    @Transactional
    public BuilderBaseLeague syncAllAndGetById(@NonNull Integer id) {
        return builderBaseLeagueRepository.saveAll(client.getBuilderBaseLeagues().items()
                        .stream()
                        .map(this::toBuilderBaseLeague)
                        .toList())
                .stream()
                .filter(item -> item.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Builder base league not found: id=" + id));
    }

    private BuilderBaseLeague toBuilderBaseLeague(BuilderBaseLeagueResponse response) {
        return new BuilderBaseLeague(response.id(), response.name());
    }

}
