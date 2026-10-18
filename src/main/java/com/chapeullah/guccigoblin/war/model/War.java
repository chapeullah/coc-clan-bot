package com.chapeullah.guccigoblin.war.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(
        name = "wars",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_wars_clans_start",
                columnNames = {"clan_tag", "opponent_clan_tag", "start_time"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class War {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    /**
     * CLAN
     */

    @Column(name = "clan_tag", nullable = false)
    private String clanTag;

    @Column(name = "clan_name", nullable = false)
    private String clanName;

    @Setter
    @Embedded
    private WarClanBadgeUrls warClanBadgeUrls;

    @Setter
    @Column(name = "war_clan_badge_urls_changed_at", nullable = true)
    private Instant warClanBadgeUrlsChangedAt;

    @Column(name = "clan_attacks", nullable = false)
    private Integer clanAttacks;

    @Column(name = "clan_attacks_changed_at", nullable = true)
    private Instant clanAttacksChangedAt;

    @Column(name = "clan_stars", nullable = false)
    private Integer clanStars;

    @Column(name = "clan_stars_changed_at", nullable = true)
    private Instant clanStarsChangedAt;

    @Column(name = "clan_destruction_percentage", nullable = false)
    private Double clanDestructionPercentage;

    @Column(name = "clan_destruction_percentage_changed_at", nullable = true)
    private Instant clanDestructionPercentageChangedAt;

    @Column(name = "clan_level", nullable = false)
    private Integer clanLevel;

    @Column(name = "clan_level_changed_at", nullable = true)
    private Instant clanLevelChangedAt;

    /**
     * OPPONENT
     */

    @Column(name = "opponent_clan_tag", nullable = false)
    private String opponentClanTag;

    @Column(name = "opponent_clan_name", nullable = false)
    private String opponentClanName;

    @Column(name = "opponent_clan_attacks", nullable = false)
    private Integer opponentClanAttacks;

    @Column(name = "opponent_clan_attacks_changed_at", nullable = true)
    private Instant opponentClanAttacksChangedAt;

    @Column(name = "opponent_clan_stars", nullable = false)
    private Integer opponentClanStars;

    @Column(name = "opponent_clan_stars_changed_at", nullable = true)
    private Instant opponentClanStarsChangedAt;

    @Column(name = "opponent_clan_destruction_percentage", nullable = false)
    private Double opponentClanDestructionPercentage;

    @Column(name = "opponent_clan_destruction_percentage_changed_at", nullable = true)
    private Instant opponentClanDestructionPercentageChangedAt;

    @Column(name = "opponent_clan_level", nullable = false)
    private Integer opponentClanLevel;

    @Column(name = "opponent_clan_level_changed_at", nullable = true)
    private Instant opponentClanLevelChangedAt;

    /**
     * GENERAL
     */

    @Column(name = "state", nullable = false)
    private String state;

    @Column(name = "state_changed_at", nullable = true)
    private Instant stateChangedAt;

    @Column(name = "team_size", nullable = false)
    private Integer teamSize;

    @Column(name = "attacks_per_member", nullable = false)
    private Integer attacksPerMember;

    @Column(name = "battle_modifier", nullable = false)
    private String battleModifier;

    @Column(name = "preparation_start_time", nullable = false)
    private Instant preparationStartTime;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = true)
    private Instant updatedAt;

    @Column(name = "war_log_synced_at", nullable = true)
    private Instant warLogSyncedAt;

    public War(
            @NonNull String clanTag,
            @NonNull String clanName,
            @NonNull Integer clanAttacks,
            @NonNull Integer clanStars,
            @NonNull Double clanDestructionPercentage,
            @NonNull Integer clanLevel,

            @NonNull String opponentClanTag,
            @NonNull String opponentClanName,
            @NonNull Integer opponentClanAttacks,
            @NonNull Integer opponentClanStars,
            @NonNull Double opponentClanDestructionPercentage,
            @NonNull Integer opponentClanLevel,

            @NonNull String state,
            @NonNull Integer teamSize,
            @NonNull Integer attacksPerMember,
            @NonNull String battleModifier,
            @NonNull Instant preparationStartTime,
            @NonNull Instant startTime,
            @NonNull Instant endTime) {
        this.clanTag = clanTag;
        this.clanName = clanName;
        this.clanAttacks = clanAttacks;
        this.clanStars = clanStars;
        this.clanDestructionPercentage = clanDestructionPercentage;
        this.clanLevel = clanLevel;

        this.opponentClanTag = opponentClanTag;
        this.opponentClanName = opponentClanName;
        this.opponentClanAttacks = opponentClanAttacks;
        this.opponentClanStars = opponentClanStars;
        this.opponentClanDestructionPercentage = opponentClanDestructionPercentage;
        this.opponentClanLevel = opponentClanLevel;

        this.state = state;
        this.teamSize = teamSize;
        this.attacksPerMember = attacksPerMember;
        this.battleModifier = battleModifier;
        this.preparationStartTime = preparationStartTime;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public void update(
            @NonNull Integer clanAttacks,
            @NonNull Integer clanStars,
            @NonNull Double clanDestructionPercentage,
            @NonNull Integer clanLevel,

            @NonNull Integer opponentClanAttacks,
            @NonNull Integer opponentClanStars,
            @NonNull Double opponentClanDestructionPercentage,
            @NonNull Integer opponentClanLevel,

            @NonNull String state) {
        Instant now = Instant.now();

        if (!Objects.equals(this.clanAttacks, clanAttacks)) {
            this.clanAttacks = clanAttacks;
            this.clanAttacksChangedAt = now;
        }

        if (!Objects.equals(this.clanStars, clanStars)) {
            this.clanStars = clanStars;
            this.clanStarsChangedAt = now;
        }

        if (!Objects.equals(this.clanDestructionPercentage, clanDestructionPercentage)) {
            this.clanDestructionPercentage = clanDestructionPercentage;
            this.clanDestructionPercentageChangedAt = now;
        }

        if (!Objects.equals(this.clanLevel, clanLevel)) {
            this.clanLevel = clanLevel;
            this.clanLevelChangedAt = now;
        }

        if (!Objects.equals(this.opponentClanAttacks, opponentClanAttacks)) {
            this.opponentClanAttacks = opponentClanAttacks;
            this.opponentClanAttacksChangedAt = now;
        }

        if (!Objects.equals(this.opponentClanStars, opponentClanStars)) {
            this.opponentClanStars = opponentClanStars;
            this.opponentClanStarsChangedAt = now;
        }

        if (!Objects.equals(this.opponentClanDestructionPercentage, opponentClanDestructionPercentage)) {
            this.opponentClanDestructionPercentage = opponentClanDestructionPercentage;
            this.opponentClanDestructionPercentageChangedAt = now;
        }

        if (!Objects.equals(this.opponentClanLevel, opponentClanLevel)) {
            this.opponentClanLevel = opponentClanLevel;
            this.opponentClanLevelChangedAt = now;
        }

        if (!Objects.equals(this.state, state)) {
            this.state = state;
            this.stateChangedAt = now;
        }
    }

    public War finish() {
        if (!Objects.equals(this.state, "warEnded")) {
            this.state = "warEnded";
            this.stateChangedAt = Instant.now();
        }
        return this;
    }

    public void markWarLogSynced(@NonNull Instant syncedAt) {
        this.warLogSyncedAt = syncedAt;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof War other)) {
            return false;
        }
        return Objects.equals(this.getClanTag(), other.getClanTag())
                && Objects.equals(this.getOpponentClanTag(), other.getOpponentClanTag())
                && Objects.equals(this.getStartTime(), other.getStartTime());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getClanTag(), this.getOpponentClanTag(), this.getStartTime());
    }

}
