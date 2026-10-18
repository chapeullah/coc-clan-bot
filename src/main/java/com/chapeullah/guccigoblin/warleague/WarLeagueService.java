package com.chapeullah.guccigoblin.warleague;

import com.chapeullah.guccigoblin.ClashOfClansClient;
import com.chapeullah.guccigoblin.warleague.dto.WarLeagueResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WarLeagueService {

    private final ClashOfClansClient client;

    private final WarLeagueRepository warLeagueRepository;

    @Transactional(readOnly = true)
    public Optional<WarLeague> findById(@NonNull Integer id) {
        return warLeagueRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public WarLeague getById(@NonNull Integer id) {
        return warLeagueRepository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("War league not found: id=" + id));
    }

    @Transactional
    public List<WarLeague> syncAll() {
        return warLeagueRepository.saveAll(client.getWarLeagues().items()
                .stream()
                .map(this::toWarLeague)
                .toList());
    }

    @Transactional
    public WarLeague syncAllAndGetById(@NonNull Integer id) {
        return warLeagueRepository.saveAll(client.getWarLeagues()
                        .items()
                        .stream()
                        .map(this::toWarLeague)
                        .toList())
                .stream()
                .filter(warLeague -> warLeague.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("War league not found: id=" + id));
    }

    private WarLeague toWarLeague(WarLeagueResponse response) {
        return new WarLeague(response.id(), response.name());
    }

}
