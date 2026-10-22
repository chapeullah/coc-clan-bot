package com.chapeullah.cocclanbot.player.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.Objects;

@Entity @Table(
        name = "player_hero_equipment_links",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_player_hero_equipment_links_hero_equipment",
                columnNames = {"hero_id", "hero_equipment_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlayerHeroEquipmentLink {

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof PlayerHeroEquipmentLink other)) {
            return false;
        }
        return Objects.equals(this.getHero(), other.getHero())
                && Objects.equals(this.getHeroEquipment(), other.getHeroEquipment());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getHero(), this.getHeroEquipment());
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hero_id", nullable = false)
    private PlayerHero hero;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hero_equipment_id", nullable = false)
    private PlayerHeroEquipment heroEquipment;

    public PlayerHeroEquipmentLink(
            @NonNull PlayerHero hero,
            @NonNull PlayerHeroEquipment heroEquipment) {
        this.hero = hero;
        this.heroEquipment = heroEquipment;
    }

}
