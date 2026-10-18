package com.chapeullah.guccigoblin.war.service;

import com.chapeullah.guccigoblin.war.dto.WarAttackResponse;
import com.chapeullah.guccigoblin.war.dto.WarClanResponse;
import com.chapeullah.guccigoblin.war.dto.WarMemberResponse;
import com.chapeullah.guccigoblin.war.model.War;
import com.chapeullah.guccigoblin.war.model.WarAttack;
import com.chapeullah.guccigoblin.war.model.WarParticipant;
import com.chapeullah.guccigoblin.war.repository.WarAttackRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WarAttackService {

    private final WarAttackRepository warAttackRepository;

    private final WarParticipantService warParticipantService;

    @Transactional(readOnly = true)
    public List<WarAttack> getByWar(@NonNull War war) {
        return warAttackRepository.findAllByWar(war);
    }

    @Transactional
    public void syncAttacks(
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

        Map<String, WarParticipant> participantsByTag = warParticipantService
                .getByWar(war)
                .stream()
                .collect(Collectors.toMap(
                        WarParticipant::getPlayerTag,
                        Function.identity()));

        List<WarAttack> warAttacks = warAttackRepository.findAllByWar(war);
        Map<Integer, WarAttack> existingWarAttacksByOrder = HashMap.newHashMap(warAttacks.size());

        warAttacks.forEach(attack -> existingWarAttacksByOrder
                .put(attack.getAttackOrder(), attack));

        for (WarMemberResponse clanMember : clanResponse.members()) {
            if (clanMember.attacks() == null) {
                continue;
            }
            for (WarAttackResponse attack : clanMember.attacks()) {
                if (existingWarAttacksByOrder.containsKey(attack.order())) {
                    continue;
                }
                WarParticipant attacker =
                        participantsByTag.get(attack.attackerTag());
                WarParticipant defender =
                        participantsByTag.get(attack.defenderTag());

                if (attacker == null || defender == null) {
                    throw new IllegalStateException("War participant not found: warId=\"" + war.getId()
                            + "\", attackerTag=\"" + attack.attackerTag()
                            + "\", defenderTag=\"" + attack.defenderTag() + "\"");
                }
                existingWarAttacksByOrder.put(
                        attack.order(),
                        new WarAttack(
                                war,
                                attacker,
                                defender,
                                attacker.getTownHallLevel(),
                                defender.getTownHallLevel(),
                                attack.stars(),
                                attack.destructionPercentage(),
                                attack.order(),
                                attack.duration()));
            }
        }
        for (WarMemberResponse opponentMember : opponentResponse.members()) {
            if (opponentMember.attacks() == null) {
                continue;
            }
            for (WarAttackResponse attack : opponentMember.attacks()) {
                if (existingWarAttacksByOrder.containsKey(attack.order())) {
                    continue;
                }
                WarParticipant attacker =
                        participantsByTag.get(attack.attackerTag());
                WarParticipant defender =
                        participantsByTag.get(attack.defenderTag());

                if (attacker == null || defender == null) {
                    throw new IllegalStateException("War participant not found: warId=\"" + war.getId()
                            + "\", attackerTag=\"" + attack.attackerTag()
                            + "\", defenderTag=\"" + attack.defenderTag() + "\"");
                }
                existingWarAttacksByOrder.put(
                        attack.order(),
                        new WarAttack(
                                war,
                                attacker,
                                defender,
                                attacker.getTownHallLevel(),
                                defender.getTownHallLevel(),
                                attack.stars(),
                                attack.destructionPercentage(),
                                attack.order(),
                                attack.duration()));
            }
        }

        long actualClanAttacks = existingWarAttacksByOrder.values().stream()
                .filter(attack -> Objects.equals(
                        attack.getAttacker().getClanTag(), war.getClanTag()))
                .count();

        long actualOpponentAttacks = existingWarAttacksByOrder.values().stream()
                .filter(attack -> Objects.equals(
                        attack.getAttacker().getClanTag(), war.getOpponentClanTag()))
                .count();

        if (clanResponse.attacks() == null
                || opponentResponse.attacks() == null
                || actualClanAttacks != clanResponse.attacks()
                || actualOpponentAttacks != opponentResponse.attacks()) {
            throw new IllegalStateException(
                    "War attacks count mismatch: warId=" + war.getId()
                            + ", clanExpected=" + clanResponse.attacks()
                            + ", clanActual=" + actualClanAttacks
                            + ", opponentExpected=" + opponentResponse.attacks()
                            + ", opponentActual=" + actualOpponentAttacks);
        }

        warAttackRepository.saveAll(existingWarAttacksByOrder.values());
    }

}
