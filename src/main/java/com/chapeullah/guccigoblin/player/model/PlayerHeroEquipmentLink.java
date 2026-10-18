package com.chapeullah.guccigoblin.player.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Entity @Table(
        name = "player_hero_equipment_links",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_player_hero_equipment_links_hero_equipment",
                columnNames = {"hero_id", "hero_equipment_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlayerHeroEquipmentLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hero_id", nullable = false)
    private PlayerHero playerHero;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hero_equipment_id", nullable = false)
    private PlayerHeroEquipment playerHeroEquipment;

    public PlayerHeroEquipmentLink(
            @NonNull PlayerHero playerHero,
            @NonNull PlayerHeroEquipment playerHeroEquipment) {
        this.playerHero = playerHero;
        this.playerHeroEquipment = playerHeroEquipment;
    }

}
