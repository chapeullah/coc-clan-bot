package com.chapeullah.guccigoblin.leaguetier;

import com.chapeullah.guccigoblin.ClashOfClansClient;
import com.chapeullah.guccigoblin.leaguetier.dto.LeagueTierResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LeagueTierService {

    private final ClashOfClansClient client;

    private final LeagueTierRepository leagueTierRepository;

    @Transactional(readOnly = true)
    public Optional<LeagueTier> findById(@NonNull Integer id) {
        return leagueTierRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public LeagueTier getById(@NonNull Integer id) {
        return leagueTierRepository
                .findById(id)
                .orElseThrow(() -> new IllegalStateException("League tier not found: id=" + id));
    }

    @Transactional
    public List<LeagueTier> syncAll() {
        return leagueTierRepository.saveAll(client.getLeagueTiers()
                .items()
                .stream()
                .map(this::toLeagueTier)
                .toList());
    }

    public LeagueTier syncAllAndGetById(@NonNull Integer id) {
        return leagueTierRepository.saveAll(client.getLeagueTiers()
                        .items()
                        .stream()
                        .map(this::toLeagueTier)
                        .toList())
                .stream()
                .filter(tier -> tier.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("League tier not found: leagueTierId=" + id));

    }

    private LeagueTier toLeagueTier(LeagueTierResponse response) {
        return new LeagueTier(
                response.id(),
                response.name(),
                response.iconUrls().small(),
                response.iconUrls().large());
    }

}
