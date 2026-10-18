package com.chapeullah.guccigoblin.war.service;

import com.chapeullah.guccigoblin.war.dto.WarClanResponse;
import com.chapeullah.guccigoblin.war.dto.WarMemberResponse;
import com.chapeullah.guccigoblin.war.model.War;
import com.chapeullah.guccigoblin.war.model.WarParticipant;
import com.chapeullah.guccigoblin.war.repository.WarParticipantRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class WarParticipantService {

    private final WarParticipantRepository warParticipantRepository;

    @Transactional(readOnly = true)
    public List<WarParticipant> getByWar(@NonNull War war) {
        return warParticipantRepository.findByWar(war);
    }

    @Transactional
    public void syncWarParticipants(
            @NonNull War war,
            @NonNull WarClanResponse clanResponse,
            @NonNull WarClanResponse opponentResponse) {
        if (clanResponse.members() == null || clanResponse.members().isEmpty()) {
            throw new IllegalArgumentException(
                    "Clan members cannot be null or empty: clanTag=" + clanResponse.tag());
        }
        if (opponentResponse.members() == null || opponentResponse.members().isEmpty()) {
            throw new IllegalArgumentException(
                    "Opponent clan members cannot be null or empty: clanTag=" + opponentResponse.tag());
        }
        if (!Objects.equals(war.getClanTag(), clanResponse.tag())) {
            throw new IllegalArgumentException(
                    "Clan tag mismatch: expected clanTag=\""
                            + war.getClanTag()
                            + "\", actual clanTag=\""
                            + clanResponse.tag() + "\"");
        }
        if (!Objects.equals(war.getOpponentClanTag(), opponentResponse.tag())) {
            throw new IllegalArgumentException(
                    "Opponent clan tag mismatch: expected clanTag=\""
                            + war.getOpponentClanTag()
                            + "\", actual clanTag=\""
                            + opponentResponse.tag() + "\"");
        }
        if (clanResponse.members().size() != war.getTeamSize()
                || opponentResponse.members().size() != war.getTeamSize()) {
            throw new IllegalArgumentException(
                    "War participants count mismatch: expected teamSize=" + war.getTeamSize()
                            + ", clanTeamSize=" + clanResponse.members().size()
                            + ", opponentTeamSize=" + opponentResponse.members().size()
            );
        }

        if (!warParticipantRepository.existsByWar(war)) {
            createParticipants(war, clanResponse, opponentResponse);
        } else {
            updateParticipants(war, clanResponse, opponentResponse);
        }
    }

    @Transactional
    private void createParticipants(
            War war,
            WarClanResponse clanResponse,
            WarClanResponse opponentResponse) {
        List<WarParticipant> warParticipants = new ArrayList<>(war.getTeamSize() * 2);

        clanResponse.members().forEach(member -> warParticipants.add(
                new WarParticipant(
                        war,
                        member.tag(),
                        member.name(),
                        clanResponse.tag(),
                        member.townHallLevel(),
                        member.mapPosition())));
        opponentResponse.members().forEach(member -> warParticipants.add(
                new WarParticipant(war,
                        member.tag(),
                        member.name(),
                        opponentResponse.tag(),
                        member.townHallLevel(),
                        member.mapPosition())));

        warParticipantRepository.saveAll(warParticipants);
    }

    @Transactional
    private void updateParticipants(
            War war,
            WarClanResponse clanResponse,
            WarClanResponse opponentResponse) {
        List<WarParticipant> warParticipants = warParticipantRepository.findByWar(war);
        Map<String, WarMemberResponse> incomingWarMemberResponsesByTag =
                HashMap.newHashMap(war.getTeamSize() * 2);
        clanResponse.members().forEach(
                member -> incomingWarMemberResponsesByTag.put(member.tag(), member));
        opponentResponse.members().forEach(
                member -> incomingWarMemberResponsesByTag.put(member.tag(), member));

        if (warParticipants.size() != war.getTeamSize() * 2) {
            throw new IllegalStateException(
                    "War participants count mismatch: warId=" + war.getId()
                            + ", expected teamSize=" + war.getTeamSize() * 2
                            + ", actual teamSize=" + warParticipants.size());
        }

        for (WarParticipant warParticipant : warParticipants) {
            WarMemberResponse warMemberResponse = incomingWarMemberResponsesByTag
                    .get(warParticipant.getPlayerTag());
            if (warMemberResponse == null) {
                throw new IllegalStateException(
                        "War participant not found in API response: warId=" + war.getId()
                                + ", playerTag=" + warParticipant.getPlayerTag());
            }

            warParticipant.update(
                    warMemberResponse.name(),
                    warMemberResponse.townHallLevel());
        }

        warParticipantRepository.saveAll(warParticipants);
    }

}
