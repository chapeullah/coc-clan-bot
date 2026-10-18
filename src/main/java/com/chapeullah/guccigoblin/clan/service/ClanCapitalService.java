package com.chapeullah.guccigoblin.clan.service;

import com.chapeullah.guccigoblin.clan.dto.ClanCapitalResponse;
import com.chapeullah.guccigoblin.clan.model.Clan;
import com.chapeullah.guccigoblin.clan.model.ClanCapital;
import com.chapeullah.guccigoblin.clan.repository.ClanCapitalRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ClanCapitalService {

    private final ClanCapitalRepository clanCapitalRepository;
    private final ClanCapitalDistrictService clanCapitalDistrictService;

    @Transactional
    public ClanCapital syncAndGetByClan(
            @NonNull Clan clan,
            ClanCapitalResponse response) {
        ClanCapital capital = clanCapitalRepository
                .findById(clan.getTag())
                .orElse(null);

        if (response == null) {
            if (capital != null) {
                throw new IllegalStateException("Clan capital is missing: clanTag=\"" + clan.getTag() + "\"");
            }
            return null;
        }

        Integer capitalHallLevel = Objects.requireNonNull(
                response.capitalHallLevel(),
                "Capital hall level is missing");

        if (capital == null) {
            capital = clanCapitalRepository.save(new ClanCapital(
                    clan,
                    capitalHallLevel,
                    response.clanGoldSinkTotal()));
        } else {
            Instant now = Instant.now();

            if (!Objects.equals(capital.getCapitalHallLevel(), capitalHallLevel)) {
                capital.setCapitalHallLevel(capitalHallLevel);
                capital.setCapitalHallLevelChangedAt(now);
            }

            if (!Objects.equals(
                    capital.getClanGoldSinkTotal(),
                    response.clanGoldSinkTotal())) {
                capital.setClanGoldSinkTotal(response.clanGoldSinkTotal());
                capital.setClanGoldSinkTotalChangedAt(now);
            }
        }

        clanCapitalDistrictService.syncAndGetByClanCapital(
                capital,
                response.districts());

        return capital;
    }
}