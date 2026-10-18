package com.chapeullah.guccigoblin.player.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "player_troops", uniqueConstraints = @UniqueConstraint(
        name = "uk_player_troops_player_tag_name",
        columnNames = {"player_tag", "name", "village"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlayerTroop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_tag", nullable = false)
    private Player player;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "level", nullable = false )
    private Integer level;

    @Column(name = "level_changed_at", nullable = true)
    private Instant levelChangedAt;

    @Column(name = "max_level", nullable = false)
    private Integer maxLevel;

    @Column(name = "max_level_changed_at", nullable = true)
    private Instant maxLevelChangedAt;

    @Column(name = "village", nullable = false)
    private String village;

    public PlayerTroop(
            @NonNull Player player,
            @NonNull String name,
            @NonNull Integer level,
            @NonNull Integer maxLevel,
            @NonNull String village) {
        if (name.isBlank()) {
            throw new IllegalArgumentException("Player troop name must not be blank");
        }
        this.player = player;
        this.name = name;
        this.level = level;
        this.maxLevel = maxLevel;
        this.village = village;
    }

    public void updateFrom(PlayerTroop source) {
        Instant now = Instant.now();

        if (!Objects.equals(this.level, source.level)) {
            this.level = source.level;
            this.levelChangedAt = now;
        }

        if (!Objects.equals(this.maxLevel, source.maxLevel)) {
            this.maxLevel = source.maxLevel;
            this.maxLevelChangedAt = now;
        }
    }

}
