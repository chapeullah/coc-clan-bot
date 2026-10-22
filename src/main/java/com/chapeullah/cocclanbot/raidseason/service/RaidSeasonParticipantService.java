package com.chapeullah.cocclanbot.raidseason.service;

import com.chapeullah.cocclanbot.raidseason.dto.RaidSeasonMemberResponse;
import com.chapeullah.cocclanbot.raidseason.model.RaidSeason;
import com.chapeullah.cocclanbot.raidseason.model.RaidSeasonParticipant;
import com.chapeullah.cocclanbot.raidseason.repository.RaidSeasonParticipantRepository;
import jakarta.transaction.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RaidSeasonParticipantService {

    private final RaidSeasonParticipantRepository raidSeasonParticipantRepository;

    @Transactional
    public List<RaidSeasonParticipant> getByRaidSeason(@NonNull RaidSeason raidSeason) {
        return raidSeasonParticipantRepository.findAllByRaidSeason(raidSeason);
    }

    @Transactional
    public List<RaidSeasonParticipant> syncRaidSeasonParticipants(
            @NonNull RaidSeason raidSeason,
            List<RaidSeasonMemberResponse> responses) {
        List<RaidSeasonParticipant> participants = raidSeasonParticipantRepository.findAllByRaidSeason(raidSeason);
        if (responses == null) {
            return participants;
        }

        for (RaidSeasonMemberResponse response : responses) {
            RaidSeasonParticipant participant = participants.stream()
                    .filter(existing -> existing.getTag().equals(response.tag()))
                    .findFirst()
                    .orElse(null);

            if (participant == null) {
                participants.add(createParticipant(raidSeason, response));
            } else {
                updateParticipant(participant, response);
            }
        }

        return participants;
    }

    private RaidSeasonParticipant createParticipant(
            @NonNull RaidSeason raidSeason,
            @NonNull RaidSeasonMemberResponse response) {
        RaidSeasonParticipant participant = new RaidSeasonParticipant(
                raidSeason,
                response.tag(),
                response.name(),
                response.attacks(),
                response.attackLimit(),
                response.bonusAttackLimit(),
                response.capitalResourcesLooted());

        return raidSeasonParticipantRepository.save(participant);
    }

    private RaidSeasonParticipant updateParticipant(
            @NonNull RaidSeasonParticipant participant,
            @NonNull RaidSeasonMemberResponse response) {
        participant.update(
                response.name(),
                response.attacks(),
                response.attackLimit(),
                response.bonusAttackLimit(),
                response.capitalResourcesLooted());

        return raidSeasonParticipantRepository.save(participant);
    }
}