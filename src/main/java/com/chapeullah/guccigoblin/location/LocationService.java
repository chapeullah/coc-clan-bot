package com.chapeullah.guccigoblin.location;

import com.chapeullah.guccigoblin.ClashOfClansClient;
import com.chapeullah.guccigoblin.location.dto.LocationResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final ClashOfClansClient client;

    private final LocationRepository locationRepository;

    @Transactional(readOnly = true)
    public Optional<Location> findById(@NonNull Integer id) {
        return locationRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Location getById(@NonNull Integer id) {
        return locationRepository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Location not found: id=" + id));
    }

    @Transactional
    public List<Location> syncAll() {
        return locationRepository.saveAll(client.getLocations()
                .items()
                .stream()
                .map(this::toLocation)
                .toList());
    }

    public Location syncAllAndGetById(@NonNull Integer id) {
        return locationRepository.saveAll(client.getLocations()
                        .items()
                        .stream()
                        .map(this::toLocation)
                        .toList())
                .stream()
                .filter(location -> location.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Location not found: id=" + id));
    }

    private Location toLocation(LocationResponse response) {
        return new Location(
                response.id(),
                response.name(),
                response.isCountry(),
                response.countryCode());
    }

}
