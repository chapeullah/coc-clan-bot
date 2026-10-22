package com.chapeullah.cocclanbot.war.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(
        name = "war_attacks",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_war_attack_war_order",
                columnNames = {"war_id", "attack_order"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WarAttack {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "war_id", nullable = false)
    private War war;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attacker_participant_id", nullable = false)
    private WarParticipant attacker;

    @Column(name = "attacker_tag", nullable = false)
    private String attackerTag;

    @Column(name = "attacker_town_hall_level", nullable = false)
    private Integer attackerTownHallLevel;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "defender_participant_id", nullable = false)
    private WarParticipant defender;

    @Column(name = "defender_tag", nullable = false)
    private String defenderTag;

    @Column(name = "defender_town_hall_level", nullable = false)
    private Integer defenderTownHallLevel;

    @Column(name = "stars", nullable = false)
    private Integer stars;

    @Column(name = "destruction_percentage", nullable = false)
    private Double destructionPercentage;

    @Column(name = "attack_order", nullable = false)
    private Integer attackOrder;

    @Column(name = "duration_seconds", nullable = false)
    private Integer durationSeconds;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public WarAttack(
            @NonNull War war,
            @NonNull WarParticipant attacker,
            @NonNull WarParticipant defender,
            @NonNull Integer attackerTownHallLevel,
            @NonNull Integer defenderTownHallLevel,
            @NonNull Integer stars,
            @NonNull Double destructionPercentage,
            @NonNull Integer attackOrder,
            @NonNull Integer durationSeconds) {
        if (!Objects.equals(war, attacker.getWar()) || !Objects.equals(war, defender.getWar())) {
            throw new IllegalArgumentException("Attack participants must belong to the specified war");
        }

        this.war = war;
        this.attacker = attacker;
        this.attackerTag = attacker.getPlayerTag();
        this.defender = defender;
        this.defenderTag = defender.getPlayerTag();
        this.attackerTownHallLevel = attackerTownHallLevel;
        this.defenderTownHallLevel = defenderTownHallLevel;
        this.stars = stars;
        this.destructionPercentage = destructionPercentage;
        this.attackOrder = attackOrder;
        this.durationSeconds = durationSeconds;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof WarAttack other)) {
            return false;
        }
        return Objects.equals(this.getWar(), other.getWar())
                && Objects.equals(this.getAttackOrder(), other.getAttackOrder());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getWar(), this.getAttackOrder());
    }

}
