package com.chapeullah.guccigoblin.capitalleague;

import com.chapeullah.guccigoblin.ClashOfClansClient;
import com.chapeullah.guccigoblin.capitalleague.dto.CapitalLeagueResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CapitalLeagueService {

    private final ClashOfClansClient client;

    private final CapitalLeagueRepository capitalLeagueRepository;

    @Transactional(readOnly = true)
    public Optional<CapitalLeague> findById(@NonNull Integer id) {
        return capitalLeagueRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public CapitalLeague getById(@NonNull Integer id) {
        return capitalLeagueRepository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Capital league not found: id=" + id));
    }

    @Transactional
    public List<CapitalLeague> syncAll() {
        return capitalLeagueRepository.saveAll(client.getCapitalLeagues().items()
                .stream()
                .map(this::toCapitalLeague)
                .toList());
    }

    public CapitalLeague syncAllAndGetById(@NonNull Integer id) {
        return capitalLeagueRepository.saveAll(client.getCapitalLeagues().items()
                        .stream()
                        .map(this::toCapitalLeague)
                        .toList())
                .stream()
                .filter(capitalLeague -> capitalLeague.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Capital league not found: id=" + id));
    }

    private CapitalLeague toCapitalLeague(CapitalLeagueResponse response) {
        return new CapitalLeague(response.id(), response.name());
    }

}
