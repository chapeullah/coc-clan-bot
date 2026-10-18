package com.chapeullah.guccigoblin.raidseason.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(
        name = "raid_season_participants",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_raid_season_participants_season_tag",
                columnNames = {"raid_season_id", "tag"}))
@Getter
@NoArgsConstructor
public class RaidSeasonParticipant {

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof RaidSeasonParticipant other)) {
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
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "raid_season_id", nullable = false)
    private RaidSeason raidSeason;

    @Column(name = "tag", nullable = false)
    private String tag;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "name_changed_at", nullable = true)
    private Instant nameChangedAt;

    @Column(name = "attack_count", nullable = true)
    private Integer attackCount;

    @Column(name = "attack_count_changed_at", nullable = true)
    private Instant attackCountChangedAt;

    @Column(name = "attack_limit", nullable = true)
    private Integer attackLimit;

    @Column(name = "attack_limit_changed_at", nullable = true)
    private Instant attackLimitChangedAt;

    @Column(name = "bonus_attack_limit", nullable = true)
    private Integer bonusAttackLimit;

    @Column(name = "bonus_attack_limit_changed_at", nullable = true)
    private Instant bonusAttackLimitChangedAt;

    @Column(name = "capital_resources_looted", nullable = true)
    private Integer capitalResourcesLooted;

    @Column(name = "capital_resources_looted_changed_at", nullable = true)
    private Instant capitalResourcesLootedChangedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public RaidSeasonParticipant(
            @NonNull RaidSeason raidSeason,
            @NonNull String tag,
            @NonNull String name,
            Integer attackCount,
            Integer attackLimit,
            Integer bonusAttackLimit,
            Integer capitalResourcesLooted) {
        this.raidSeason = raidSeason;
        this.tag = tag;
        this.name = name;
        this.attackCount = attackCount;
        this.attackLimit = attackLimit;
        this.bonusAttackLimit = bonusAttackLimit;
        this.capitalResourcesLooted = capitalResourcesLooted;
    }

    public void update(
            @NonNull String name,
            Integer attackCount,
            Integer attackLimit,
            Integer bonusAttackLimit,
            Integer capitalResourcesLooted) {
        Instant now = Instant.now();

        if (!Objects.equals(this.name, name)) {
            this.name = name;
            this.nameChangedAt = now;
        }

        if (!Objects.equals(this.attackCount, attackCount)) {
            this.attackCount = attackCount;
            this.attackCountChangedAt = now;
        }

        if (!Objects.equals(this.attackLimit, attackLimit)) {
            this.attackLimit = attackLimit;
            this.attackLimitChangedAt = now;
        }

        if (!Objects.equals(this.bonusAttackLimit, bonusAttackLimit)) {
            this.bonusAttackLimit = bonusAttackLimit;
            this.bonusAttackLimitChangedAt = now;
        }

        if (!Objects.equals(this.capitalResourcesLooted, capitalResourcesLooted)) {
            this.capitalResourcesLooted = capitalResourcesLooted;
            this.capitalResourcesLootedChangedAt = now;
        }
    }

}
