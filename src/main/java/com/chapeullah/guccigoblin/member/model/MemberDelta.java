package com.chapeullah.guccigoblin.member.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Objects;
import java.util.function.Function;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class MemberDelta {

    public record Delta<T>(T oldValue, T newValue) {
        public boolean changed() {
            return !Objects.equals(oldValue, newValue);
        }
    }

    public record Snapshot(
            String tag,
            String clanTag,
            boolean inClan,
            String name,
            String role,
            Integer townHallLevel,
            Integer expLevel,
            Integer leagueTierId,
            Integer trophies,
            Integer builderBaseTrophies,
            Integer builderBaseLeagueId,
            Integer clanRank,
            Integer previousClanRank,
            Integer donations,
            Integer donationsReceived) {

        public static Snapshot from(@NonNull Member member) {
            return new Snapshot(
                    member.getTag(),
                    member.getClan().getTag(),
                    Boolean.TRUE.equals(member.getInClan()),
                    member.getName(),
                    member.getRole(),
                    member.getTownHallLevel(),
                    member.getExpLevel(),
                    member.getLeagueTier() == null ? null : member.getLeagueTier().getId(),
                    member.getTrophies(),
                    member.getBuilderBaseTrophies(),
                    member.getBuilderBaseLeague() == null ? null : member.getBuilderBaseLeague().getId(),
                    member.getClanRank(),
                    member.getPreviousClanRank(),
                    member.getDonations(),
                    member.getDonationsReceived());
        }
    }

    public enum MembershipStatus {
        JOINED,
        UNCHANGED,
        LEFT
    }

    private final String tag;
    private final MembershipStatus membershipStatus;
    private final Delta<String> nameDelta;
    private final Delta<String> roleDelta;
    private final Delta<Integer> townHallLevelDelta;
    private final Delta<Integer> expLevelDelta;
    private final Delta<Integer> leagueTierIdDelta;
    private final Delta<Integer> trophiesDelta;
    private final Delta<Integer> builderBaseTrophiesDelta;
    private final Delta<Integer> builderBaseLeagueIdDelta;
    private final Delta<Integer> clanRankDelta;
    private final Delta<Integer> previousClanRankDelta;
    private final Delta<Integer> donationsDelta;
    private final Delta<Integer> donationsReceivedDelta;

    public static MemberDelta merge(
            @NonNull Snapshot oldMember,
            @NonNull Member newMember) {
        Snapshot current = Snapshot.from(newMember);
        if (!Objects.equals(oldMember.tag(), current.tag())
                || !Objects.equals(oldMember.clanTag(), current.clanTag())) {
            throw new IllegalArgumentException("Members must belong to the same player and clan");
        }
        return between(MembershipStatus.UNCHANGED, oldMember, current);
    }

    public static MemberDelta joined(@NonNull Member newMember) {
        return between(MembershipStatus.JOINED, null, Snapshot.from(newMember));
    }

    public static MemberDelta left(@NonNull Snapshot oldMember) {
        return between(MembershipStatus.LEFT, oldMember, null);
    }

    private static MemberDelta between(
            @NonNull MembershipStatus membershipStatus,
            Snapshot oldMember,
            Snapshot newMember) {
        Snapshot identifiedMember = newMember != null ? newMember : oldMember;
        if (identifiedMember == null) {
            throw new IllegalArgumentException("Members must not be null");
        }
        return new MemberDelta(
                identifiedMember.tag(),
                membershipStatus,
                delta(oldMember, newMember, Snapshot::name),
                delta(oldMember, newMember, Snapshot::role),
                delta(oldMember, newMember, Snapshot::townHallLevel),
                delta(oldMember, newMember, Snapshot::expLevel),
                delta(oldMember, newMember, Snapshot::leagueTierId),
                delta(oldMember, newMember, Snapshot::trophies),
                delta(oldMember, newMember, Snapshot::builderBaseTrophies),
                delta(oldMember, newMember, Snapshot::builderBaseLeagueId),
                delta(oldMember, newMember, Snapshot::clanRank),
                delta(oldMember, newMember, Snapshot::previousClanRank),
                delta(oldMember, newMember, Snapshot::donations),
                delta(oldMember, newMember, Snapshot::donationsReceived));
    }

    private static <T> Delta<T> delta(
            Snapshot oldMember,
            Snapshot newMember,
            Function<Snapshot, T> extractor) {
        T oldValue = oldMember == null ? null : extractor.apply(oldMember);
        T newValue = newMember == null ? null : extractor.apply(newMember);
        return new Delta<>(oldValue, newValue);
    }

    public boolean hasChanges() {
        return membershipStatus != MembershipStatus.UNCHANGED
                || nameDelta.changed()
                || roleDelta.changed()
                || townHallLevelDelta.changed()
                || expLevelDelta.changed()
                || leagueTierIdDelta.changed()
                || trophiesDelta.changed()
                || builderBaseTrophiesDelta.changed()
                || builderBaseLeagueIdDelta.changed()
                || clanRankDelta.changed()
                || previousClanRankDelta.changed()
                || donationsDelta.changed()
                || donationsReceivedDelta.changed();
    }
}