package com.chapeullah.guccigoblin.raidseason.model;

import com.chapeullah.guccigoblin.clan.model.Clan;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(
        name = "raid_seasons",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_raids_clan_start",
                columnNames = {"clan_tag", "start_time"}))
@Getter @Setter
@NoArgsConstructor
public class RaidSeason {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clan_tag", nullable = false)
    private Clan clan;

    @Column(name = "state", nullable = true)
    private String state;

    @Column(name = "state_changed_at", nullable = true)
    private Instant stateChangedAt;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Column(name = "capital_total_loot", nullable = true)
    private Integer capitalTotalLoot;

    @Column(name = "capital_total_loot_changed_at", nullable = true)
    private Instant capitalTotalLootChangedAt;

    @Column(name = "raids_completed", nullable = true)
    private Integer raidsCompleted;

    @Column(name = "raids_completed_changed_at", nullable = true)
    private Instant raidsCompletedChangedAt;

    @Column(name = "total_attacks", nullable = true)
    private Integer totalAttacks;

    @Column(name = "total_attacks_changed_at", nullable = true)
    private Instant totalAttacksChangedAt;

    @Column(name = "enemy_districts_destroyed", nullable = true)
    private Integer enemyDistrictsDestroyed;

    @Column(name = "enemy_districts_destroyed_changed_at", nullable = true)
    private Instant enemyDistrictsDestroyedChangedAt;

    @Column(name = "offensive_reward", nullable = true)
    private Integer offensiveReward;

    @Column(name = "offensive_reward_changed_at", nullable = true)
    private Instant offensiveRewardChangedAt;

    @Column(name = "defensive_reward", nullable = true)
    private Integer defensiveReward;

    @Column(name = "defensive_reward_changed_at", nullable = true)
    private Instant defensiveRewardChangedAt;

    @OneToMany(mappedBy = "raidSeason", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<RaidSeasonParticipant> participants = new ArrayList<>();

    @Column(name = "raw_json", columnDefinition = "text", nullable = false)
    private String rawJson;

    @Column(name = "raw_json_changed_at", nullable = true)
    private Instant rawJsonChangedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public RaidSeason(
            @NonNull Clan clan,
            String state,
            @NonNull Instant startTime,
            @NonNull Instant endTime,
            Integer capitalTotalLoot,
            Integer raidsCompleted,
            Integer totalAttacks,
            Integer enemyDistrictsDestroyed,
            Integer offensiveReward,
            Integer defensiveReward,
            @NonNull String rawJson) {
        this.clan = clan;
        this.state = state;
        this.startTime = startTime;
        this.endTime = endTime;
        this.capitalTotalLoot = capitalTotalLoot;
        this.raidsCompleted = raidsCompleted;
        this.totalAttacks = totalAttacks;
        this.enemyDistrictsDestroyed = enemyDistrictsDestroyed;
        this.offensiveReward = offensiveReward;
        this.defensiveReward = defensiveReward;
        this.rawJson = rawJson;
    }

    public void update(
            String state,
            Integer capitalTotalLoot,
            Integer raidsCompleted,
            Integer totalAttacks,
            Integer enemyDistrictsDestroyed,
            Integer offensiveReward,
            Integer defensiveReward,
            @NonNull String rawJson) {
        Instant now = Instant.now();

        if (!Objects.equals(this.state, state)) {
            this.state = state;
            this.stateChangedAt = now;
        }

        if (!Objects.equals(this.capitalTotalLoot, capitalTotalLoot)) {
            this.capitalTotalLoot = capitalTotalLoot;
            this.capitalTotalLootChangedAt = now;
        }

        if (!Objects.equals(this.raidsCompleted, raidsCompleted)) {
            this.raidsCompleted = raidsCompleted;
            this.raidsCompletedChangedAt = now;
        }

        if (!Objects.equals(this.totalAttacks, totalAttacks)) {
            this.totalAttacks = totalAttacks;
            this.totalAttacksChangedAt = now;
        }

        if (!Objects.equals(this.enemyDistrictsDestroyed, enemyDistrictsDestroyed)) {
            this.enemyDistrictsDestroyed = enemyDistrictsDestroyed;
            this.enemyDistrictsDestroyedChangedAt = now;
        }

        if (!Objects.equals(this.offensiveReward, offensiveReward)) {
            this.offensiveReward = offensiveReward;
            this.offensiveRewardChangedAt = now;
        }

        if (!Objects.equals(this.defensiveReward, defensiveReward)) {
            this.defensiveReward = defensiveReward;
            this.defensiveRewardChangedAt = now;
        }

        if (!Objects.equals(this.rawJson, rawJson)) {
            this.rawJson = rawJson;
            this.rawJsonChangedAt = now;
        }
    }

}
