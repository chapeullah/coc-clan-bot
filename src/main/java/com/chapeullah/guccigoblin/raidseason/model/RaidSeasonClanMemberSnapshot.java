package com.chapeullah.guccigoblin.raidseason.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity @Table(
        name = "raid_season_clan_member_snapshots",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_raid_season_clan_member_snapshots_season_tag",
                columnNames = {"raid_season_id", "tag"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RaidSeasonClanMemberSnapshot {

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

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public RaidSeasonClanMemberSnapshot(
            @NonNull RaidSeason raidSeason,
            @NonNull String tag,
            @NonNull String name) {
        this.raidSeason = raidSeason;
        this.tag = tag;
        this.name = name;
    }

}