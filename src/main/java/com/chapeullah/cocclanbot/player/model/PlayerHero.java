package com.chapeullah.cocclanbot.player.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(
        name = "player_heroes",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_player_heroes_player_tag_name",
                columnNames = {"player_tag", "name"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlayerHero {

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof PlayerHero other)) {
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

    @Column(name = "level", nullable = false)
    private Integer level;

    @Column(name = "level_changed_at", nullable = true)
    private Instant levelChangedAt;

    @Column(name = "max_level", nullable = false)
    private Integer maxLevel;

    @Column(name = "max_level_changed_at", nullable = true)
    private Instant maxLevelChangedAt;

    @SuppressWarnings("FieldMayBeFinal")
    @OneToMany(mappedBy = "hero", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlayerHeroEquipmentLink> playerHeroEquipmentLinks = new ArrayList<>();

    @Column(name = "village", nullable = false)
    private String village;

    @Column(name = "village_changed_at", nullable = true)
    private Instant villageChangedAt;

    public PlayerHero(
            @NonNull Player player,
            @NonNull String name,
            @NonNull Integer level,
            @NonNull Integer maxLevel,
            List<PlayerHeroEquipmentLink> playerHeroEquipmentLinks,
            @NonNull String village) {
        if (name.isBlank()) {
            throw new IllegalArgumentException("Player hero name must not be blank");
        }
        if (village.isBlank()) {
            throw new IllegalArgumentException("Player hero village must not be blank");
        }
        this.player = player;
        this.name = name;
        this.level = level;
        this.maxLevel = maxLevel;
        this.playerHeroEquipmentLinks = playerHeroEquipmentLinks;
        this.village = village;
    }

    public void updateFrom(PlayerHero source) {
        Instant now = Instant.now();

        if (!Objects.equals(this.level, source.level)) {
            this.level = source.level;
            this.levelChangedAt = now;
        }

        if (!Objects.equals(this.maxLevel, source.maxLevel)) {
            this.maxLevel = source.maxLevel;
            this.maxLevelChangedAt = now;
        }

        if (!Objects.equals(this.village, source.village)) {
            this.village = source.village;
            this.villageChangedAt = now;
        }
    }

}
