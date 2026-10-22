package com.chapeullah.cocclanbot.player.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "player_spells", uniqueConstraints = @UniqueConstraint(
        name = "uk_player_spells_player_tag_name",
        columnNames = {"player_tag", "name", "village"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlayerSpell {

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof PlayerSpell other)) {
            return false;
        }
        return Objects.equals(this.getPlayer(), other.getPlayer())
                && Objects.equals(this.getName(), other.getName())
                && Objects.equals(this.getVillage(), other.getVillage());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getPlayer(), this.getName(), this.getVillage());
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

    @Column(name = "village", nullable = false)
    private String village;

    public PlayerSpell(
            Player player,
            String name,
            Integer level,
            Integer maxLevel,
            String village) {
        if (name.isBlank()) {
            throw new IllegalArgumentException("Player spell name must not be blank");
        }
        this.player = player;
        this.name = name;
        this.level = level;
        this.maxLevel = maxLevel;
        this.village = village;
    }

    public void updateFrom(PlayerSpell source) {
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
