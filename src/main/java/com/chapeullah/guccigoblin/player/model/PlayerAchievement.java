package com.chapeullah.guccigoblin.player.model;

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
        name = "player_achievements",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_player_achievements_player_tag_name_village",
                columnNames = {"player_tag", "name"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlayerAchievement {

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof PlayerAchievement other)) {
            return false;
        }
        return Objects.equals(this.getPlayer(), other.getPlayer())
                && Objects.equals(this.getName(), other.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getPlayer(), this.getName());
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_tag", nullable = false)
    private Player player;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "stars", nullable = true)
    private Integer stars;

    @Column(name = "stars_changed_at", nullable = true)
    private Instant starsChangedAt;

    @Column(name = "value", nullable = true)
    private Integer value;

    @Column(name = "value_changed_at", nullable = true)
    private Instant valueChangedAt;

    @Column(name = "target", nullable = true)
    private Integer target;

    @Column(name = "target_changed_at", nullable = true)
    private Instant targetChangedAt;

    @Column(name = "info", nullable = true)
    private String info;

    @Column(name = "info_changed_at", nullable = true)
    private Instant infoChangedAt;

    @Column(name = "completion_info", nullable = true)
    private String completionInfo;

    @Column(name = "completion_info_changed_at", nullable = true)
    private Instant completionInfoChangedAt;

    @Column(name = "village", nullable = true)
    private String village;

    @Column(name = "village_changed_at", nullable = true)
    private Instant villageChangedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public PlayerAchievement(
            @NonNull Player player,
            @NonNull String name,
            Integer stars,
            Integer value,
            Integer target,
            String info,
            String completionInfo,
            String village) {
        if (name.isBlank()) {
            throw new IllegalArgumentException("Player achievement name must not be blank");
        }
        if (stars != null && (stars < 0 || stars > 3)) {
            throw new IllegalArgumentException("Player achievement stars must be between 0 and 3");
        }
        if (info != null && info.isBlank()) {
            throw new IllegalArgumentException("Player achievement info must not be blank");
        }
        if (completionInfo != null && completionInfo.isBlank()) {
            throw new IllegalArgumentException("Player achievement completion info must not be blank");
        }
        if (village != null && village.isBlank()) {
            throw new IllegalArgumentException("Player achievement village must not be blank");
        }
        this.player = player;
        this.name = name;
        this.stars = stars;
        this.value = value;
        this.target = target;
        this.info = info;
        this.completionInfo = completionInfo;
        this.village = village;
    }

    public void updateFrom(PlayerAchievement playerAchievement) {
        Instant now = Instant.now();

        if (!Objects.equals(this.stars, playerAchievement.stars)) {
            this.stars = playerAchievement.stars;
            this.starsChangedAt = now;
        }

        if (!Objects.equals(this.value, playerAchievement.value)) {
            this.value = playerAchievement.value;
            this.valueChangedAt = now;
        }

        if (!Objects.equals(this.target, playerAchievement.target)) {
            this.target = playerAchievement.target;
            this.targetChangedAt = now;
        }

        if (!Objects.equals(this.info, playerAchievement.info)) {
            this.info = playerAchievement.info;
            this.infoChangedAt = now;
        }

        if (!Objects.equals(this.completionInfo, playerAchievement.completionInfo)) {
            this.completionInfo = playerAchievement.completionInfo;
            this.completionInfoChangedAt = now;
        }

        if (!Objects.equals(this.village, playerAchievement.village)) {
            this.village = playerAchievement.village;
            this.villageChangedAt = now;
        }
    }

}
