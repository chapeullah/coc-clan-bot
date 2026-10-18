package com.chapeullah.guccigoblin.clan.service;

import com.chapeullah.guccigoblin.clan.dto.ClanCapitalDistrictResponse;
import com.chapeullah.guccigoblin.clan.model.ClanCapital;
import com.chapeullah.guccigoblin.clan.model.ClanCapitalDistrict;
import com.chapeullah.guccigoblin.clan.repository.ClanCapitalDistrictRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ClanCapitalDistrictService {

    private final ClanCapitalDistrictRepository clanCapitalDistrictRepository;

    @Transactional
    public List<ClanCapitalDistrict> syncAndGetByClanCapital(
            @NonNull ClanCapital capital,
            List<ClanCapitalDistrictResponse> responses) {
        List<ClanCapitalDistrict> current = capital.getDistricts();
        if (responses == null) {
            if (!current.isEmpty()) {
                throw new IllegalStateException(
                        "Clan capital districts are missing: clanTag=\"" + capital.getClanTag() + "\"");
            }
            return new ArrayList<>(current);
        }
        List<ClanCapitalDistrict> incoming = new ArrayList<>();
        List<ClanCapitalDistrict> added = new ArrayList<>();
        Set<Long> incomingIds = new HashSet<>();
        Instant now = Instant.now();
        for (ClanCapitalDistrictResponse response : responses) {
            Objects.requireNonNull(response, "Capital district is missing");
            Long districtId = Objects.requireNonNull(response.id(), "Capital district ID is missing");
            if (!incomingIds.add(districtId)) {
                throw new IllegalArgumentException("Duplicate capital district ID: " + districtId);
            }
            ClanCapitalDistrict district = current.stream()
                    .filter(existing ->
                            existing.getDistrictId().equals(districtId))
                    .findFirst()
                    .orElse(null);
            if (district == null) {
                district = new ClanCapitalDistrict(
                        capital,
                        districtId,
                        response.name(),
                        response.districtHallLevel());
                added.add(district);
            } else if (!Objects.equals(
                    district.getDistrictHallLevel(),
                    response.districtHallLevel())) {
                district.setDistrictHallLevel(response.districtHallLevel());
                district.setDistrictHallLevelChangedAt(now);
            }
            incoming.add(district);
        }
        current.retainAll(incoming);
        if (!added.isEmpty()) {
            current.addAll(clanCapitalDistrictRepository.saveAll(added));
        }
        return new ArrayList<>(current);
    }
}