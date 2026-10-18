package com.chapeullah.guccigoblin.member;

import com.chapeullah.guccigoblin.member.model.Member;
import com.chapeullah.guccigoblin.member.model.MemberDelta;
import com.chapeullah.guccigoblin.member.model.MemberDelta.Snapshot;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class MemberDeltaService {

    public void memberDeltaOutput(
            @NonNull Map<String, Snapshot> oldMembers,
            @NonNull Map<String, Member> newMembers) {
        if (!log.isDebugEnabled()) {
            return;
        }
        List<MemberDelta> memberDeltas = memberDeltasFrom(oldMembers, newMembers);
        if (memberDeltas.isEmpty()) {
            log.debug("Members sync: no changes");
            return;
        }
        for (MemberDelta memberDelta : memberDeltas) {
            printMemberDelta(memberDelta);
        }
    }

    public static List<MemberDelta> memberDeltasFrom(
            @NonNull Map<String, Snapshot> oldMembers,
            @NonNull Map<String, Member> newMembers) {
        Map<String, Snapshot> remainingOldMembers = new LinkedHashMap<>(oldMembers);
        List<MemberDelta> memberDeltas = new ArrayList<>();
        for (Map.Entry<String, Member> entry : newMembers.entrySet()) {
            String tag = entry.getKey();
            Member newMember = entry.getValue();
            Snapshot oldMember = oldMembers.get(tag);
            if (oldMember == null
                    || !oldMember.inClan()) {
                memberDeltas.add(MemberDelta.joined(newMember));
                remainingOldMembers.remove(tag);
                continue;
            }
            MemberDelta memberDelta = MemberDelta.merge(oldMember, newMember);
            if (memberDelta.hasChanges()) {
                memberDeltas.add(memberDelta);
            }
            remainingOldMembers.remove(tag);
        }
        for (Snapshot oldMember : remainingOldMembers.values()) {
            if (oldMember.inClan()) {
                memberDeltas.add(MemberDelta.left(oldMember));
            }
        }
        return memberDeltas;
    }

    private void printMemberDelta(@NonNull MemberDelta memberDelta) {
        switch (memberDelta.getMembershipStatus()) {
            case JOINED -> log.debug(
                    "Member joined clan: memberTag=\"{}\", memberName=\"{}\"",
                    memberDelta.getTag(),
                    memberDelta.getNameDelta().newValue());
            case LEFT -> log.debug(
                    "Member left clan: memberTag=\"{}\", memberName=\"{}\"",
                    memberDelta.getTag(),
                    memberDelta.getNameDelta().oldValue());
            case UNCHANGED -> {
                String memberName =
                        memberDelta.getNameDelta().newValue();
                logIfChange(memberDelta, memberName, "name",
                        memberDelta.getNameDelta());
                logIfChange(memberDelta, memberName, "role",
                        memberDelta.getRoleDelta());
                logIfChange(memberDelta, memberName, "townHallLevel",
                        memberDelta.getTownHallLevelDelta());
                logIfChange(memberDelta, memberName, "expLevel",
                        memberDelta.getExpLevelDelta());
                logIfChange(memberDelta, memberName, "leagueTierId",
                        memberDelta.getLeagueTierIdDelta());
                logIfChange(memberDelta, memberName, "trophies",
                        memberDelta.getTrophiesDelta());
                logIfChange(memberDelta, memberName, "builderBaseTrophies",
                        memberDelta.getBuilderBaseTrophiesDelta());
                logIfChange(memberDelta, memberName, "builderBaseLeagueId",
                        memberDelta.getBuilderBaseLeagueIdDelta());
                logIfChange(memberDelta, memberName, "clanRank",
                        memberDelta.getClanRankDelta());
                logIfChange(memberDelta, memberName,"previousClanRank",
                        memberDelta.getPreviousClanRankDelta());
                logIfChange(memberDelta, memberName, "donations",
                        memberDelta.getDonationsDelta());
                logIfChange(memberDelta, memberName, "donationsReceived",
                        memberDelta.getDonationsReceivedDelta());
            }
        }
    }

    private <T> void logIfChange(
            @NonNull MemberDelta memberDelta,
            @NonNull String memberName,
            @NonNull String field,
            @NonNull MemberDelta.Delta<T> delta) {
        if (!delta.changed()) {
            return;
        }
        log.debug(
                "Member changed: memberTag=\"{}\", memberName=\"{}\", field=\"{}\", oldValue={}, newValue={}",
                memberDelta.getTag(),
                memberName,
                field,
                formatLogValue(delta.oldValue()),
                formatLogValue(delta.newValue()));
    }

    private static Object formatLogValue(Object value) {
        return value instanceof String text ? "\"" + text + "\"" : value;
    }
}
