package com.chapeullah.guccigoblin.raidseason.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.Objects;

@Entity @Table(
        name = "raid_season_clan_member_snapshots",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_raid_season_clan_member_snapshots_season_tag",
                columnNames = {"raid_season_id", "tag"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RaidSeasonClanMemberSnapshot {

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof RaidSeasonClanMemberSnapshot other)) {
            return false;
        }
        return Objects.equals(this.getRaidSeason(), other.getRaidSeason())
                && Objects.equals(this.getTag(), other.getTag());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getRaidSeason(), this.getTag());
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "raid_season_id", nullable = false)
    private RaidSeason raidSeason;

    @Column(name = "tag", nullable = false)
    private String tag;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "joined_at", nullable = true)
    private Instant joinedAt;

    @Column(name = "joined_after", nullable = true)
    private Instant joinedAfter;

    @Column(name = "role", nullable = true)
    private String role;

    @Column(name = "clan_type", nullable = true)
    private String clanType;

    @Column(name = "participation_eligible", nullable = true)
    private Boolean participationEligible;

    @Column(name = "participation_opportunity_observed", nullable = true)
    private Boolean participationOpportunityObserved;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public RaidSeasonClanMemberSnapshot(
            @NonNull RaidSeason raidSeason,
            @NonNull String tag,
            @NonNull String name,
            Instant joinedAt,
            Instant joinedAfter,
            String role,
            String clanType,
            Boolean participationEligible) {
        this.raidSeason = raidSeason;
        this.tag = tag;
        this.name = name;
        this.joinedAt = joinedAt;
        this.joinedAfter = joinedAfter;
        this.role = role;
        this.clanType = clanType;
        this.participationEligible = participationEligible;
        this.participationOpportunityObserved = Boolean.TRUE.equals(participationEligible);
    }

    public void updateParticipation(
            @NonNull String role,
            @NonNull String clanType,
            Boolean participationEligible) {
        this.role = role;
        this.clanType = clanType;

        this.participationEligible = participationEligible;
        if (Boolean.TRUE.equals(participationEligible)) {
            this.participationOpportunityObserved = true;
        }
    }

}
