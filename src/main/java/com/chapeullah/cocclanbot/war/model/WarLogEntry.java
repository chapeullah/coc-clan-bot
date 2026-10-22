package com.chapeullah.cocclanbot.war.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.Objects;

@Entity @Table(
        name = "war_log",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_war_log_clan_tag_end_time",
                columnNames = {"clan_tag", "end_time"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WarLogEntry {

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof WarLogEntry other)) {
            return false;
        }
        return Objects.equals(this.getClanTag(), other.getClanTag())
                && Objects.equals(this.getEndTime(), other.getEndTime());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getClanTag(), this.getEndTime());
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    /**
     * GENERAL
     */

    @Column(name = "result", nullable = true)
    private String result;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Column(name = "team_size", nullable = false)
    private Integer teamSize;

    @Column(name = "attacks_per_member", nullable = false)
    private Integer attacksPerMember;

    @Column(name = "battle_modifier", nullable = true)
    private String battleModifier;

    /**
     * CLAN
     */

    @Column(name = "clan_tag", nullable = false)
    private String clanTag;

    @Column(name = "clan_name", nullable = true)
    private String clanName;

    @Column(name = "clan_level", nullable = true)
    private Integer clanLevel;

    @Column(name = "clan_level_changed_at", nullable = true)
    private Instant clanLevelChangedAt;

    @Column(name = "clan_attacks", nullable = true)
    private Integer clanAttacks;

    @Column(name = "clan_attacks_changed_at", nullable = true)
    private Instant clanAttacksChangedAt;

    @Column(name = "clan_stars", nullable = true)
    private Integer clanStars;

    @Column(name = "clan_stars_changed_at", nullable = true)
    private Instant clanStarsChangedAt;

    @Column(name = "clan_destruction_percentage", nullable = true)
    private Double clanDestructionPercentage;

    @Column(name = "clan_destruction_percentage_changed_at", nullable = true)
    private Instant clanDestructionPercentageChangedAt;

    @Column(name = "clan_exp_earned")
    private Integer clanExpEarned;

    @Column(name = "clan_exp_earned_changed_at", nullable = true)
    private Instant clanExpEarnedChangedAt;

    @Embedded
    private WarLogEntryClanBadgeUrls clanBadgeUrls;

    @Column(name = "clan_badge_urls_changed_at", nullable = true)
    private Instant clanBadgeUrlsChangedAt;

    /**
     * OPPONENT
     */

    @Column(name = "opponent_clan_tag", nullable = true)
    private String opponentClanTag;

    @Column(name = "opponent_clan_name", nullable = true)
    private String opponentClanName;

    @Column(name = "opponent_clan_level", nullable = true)
    private Integer opponentClanLevel;

    @Column(name = "opponent_clan_level_changed_at", nullable = true)
    private Instant opponentClanLevelChangedAt;

    @Column(name = "opponent_clan_stars", nullable = true)
    private Integer opponentClanStars;

    @Column(name = "opponent_clan_stars_changed_at", nullable = true)
    private Instant opponentClanStarsChangedAt;

    @Column(name = "opponent_clan_destruction_percentage", nullable = true)
    private Double opponentClanDestructionPercentage;

    @Column(name = "opponent_clan_destruction_percentage_changed_at", nullable = true)
    private Instant opponentClanDestructionPercentageChangedAt;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "small", column = @Column(name = "opponent_clan_badge_small_url", length = 512)),
            @AttributeOverride(name = "large", column = @Column(name = "opponent_clan_badge_large_url", length = 512)),
            @AttributeOverride(name = "medium", column = @Column(name = "opponent_clan_badge_medium_url", length = 512))
    })
    private WarLogEntryClanBadgeUrls opponentClanBadgeUrls;

    @Column(name = "opponent_clan_badge_urls_changed_at", nullable = true)
    private Instant opponentClanBadgeUrlsChangedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public WarLogEntry(
            String result,
            @NonNull Instant endTime,
            Integer teamSize,
            Integer attacksPerMember,
            String battleModifier,

            @NonNull String clanTag,
            String clanName,
            Integer clanLevel,
            Integer clanAttacks,
            Integer clanStars,
            Double clanDestructionPercentage,
            Integer clanExpEarned,
            WarLogEntryClanBadgeUrls clanBadgeUrls,

            String opponentClanTag,
            String opponentClanName,
            Integer opponentClanLevel,
            Integer opponentClanStars,
            Double opponentClanDestructionPercentage,
            WarLogEntryClanBadgeUrls opponentClanBadgeUrls) {
        this.result = result;
        this.endTime = endTime;
        this.teamSize = teamSize;
        this.attacksPerMember = attacksPerMember;
        this.battleModifier = battleModifier;

        this.clanTag = clanTag;
        this.clanName = clanName;
        this.clanLevel = clanLevel;
        this.clanAttacks = clanAttacks;
        this.clanStars = clanStars;
        this.clanDestructionPercentage = clanDestructionPercentage;
        this.clanExpEarned = clanExpEarned;
        this.clanBadgeUrls = clanBadgeUrls;

        this.opponentClanTag = opponentClanTag;
        this.opponentClanName = opponentClanName;
        this.opponentClanLevel = opponentClanLevel;
        this.opponentClanStars = opponentClanStars;
        this.opponentClanDestructionPercentage = opponentClanDestructionPercentage;
        this.opponentClanBadgeUrls = opponentClanBadgeUrls;
    }

    public boolean updateFrom(@NonNull WarLogEntry incoming) {
        if (!Objects.equals(this.clanTag, incoming.getClanTag())
                || !Objects.equals(this.endTime, incoming.getEndTime())) {
            throw new IllegalArgumentException("Cannot update a different war log entry");
        }

        Instant now = Instant.now();
        boolean changed = false;

        if (!Objects.equals(this.clanLevel, incoming.getClanLevel())) {
            this.clanLevel = incoming.getClanLevel();
            this.clanLevelChangedAt = now;
            changed = true;
        }

        if (!Objects.equals(this.clanAttacks, incoming.getClanAttacks())) {
            this.clanAttacks = incoming.getClanAttacks();
            this.clanAttacksChangedAt = now;
            changed = true;
        }

        if (!Objects.equals(this.clanStars, incoming.getClanStars())) {
            this.clanStars = incoming.getClanStars();
            this.clanStarsChangedAt = now;
            changed = true;
        }

        if (!Objects.equals(this.clanDestructionPercentage, incoming.getClanDestructionPercentage())) {
            this.clanDestructionPercentage = incoming.getClanDestructionPercentage();
            this.clanDestructionPercentageChangedAt = now;
            changed = true;
        }

        if (!Objects.equals(this.clanExpEarned, incoming.getClanExpEarned())) {
            this.clanExpEarned = incoming.getClanExpEarned();
            this.clanExpEarnedChangedAt = now;
            changed = true;
        }

        if (!Objects.equals(this.clanBadgeUrls, incoming.getClanBadgeUrls())) {
            this.clanBadgeUrls = incoming.getClanBadgeUrls();
            this.clanBadgeUrlsChangedAt = now;
            changed = true;
        }

        if (!Objects.equals(this.opponentClanLevel, incoming.getOpponentClanLevel())) {
            this.opponentClanLevel = incoming.getOpponentClanLevel();
            this.opponentClanLevelChangedAt = now;
            changed = true;
        }

        if (!Objects.equals(this.opponentClanStars, incoming.getOpponentClanStars())) {
            this.opponentClanStars = incoming.getOpponentClanStars();
            this.opponentClanStarsChangedAt = now;
            changed = true;
        }

        if (!Objects.equals(this.opponentClanDestructionPercentage, incoming.getOpponentClanDestructionPercentage())) {
            this.opponentClanDestructionPercentage = incoming.getOpponentClanDestructionPercentage();
            this.opponentClanDestructionPercentageChangedAt = now;
            changed = true;
        }

        if (!Objects.equals(this.opponentClanBadgeUrls, incoming.getOpponentClanBadgeUrls())) {
            this.opponentClanBadgeUrls = incoming.getOpponentClanBadgeUrls();
            this.opponentClanBadgeUrlsChangedAt = now;
            changed = true;
        }

        return changed;
    }

}
