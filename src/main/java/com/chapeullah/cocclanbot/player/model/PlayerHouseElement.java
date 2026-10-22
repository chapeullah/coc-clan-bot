package com.chapeullah.cocclanbot.player.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(
        name = "player_house_elements",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_player_house_elements_player_element",
                columnNames = {"player_tag", "element_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlayerHouseElement {

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof PlayerHouseElement other)) {
            return false;
        }
        return Objects.equals(this.getPlayer(), other.getPlayer())
                && Objects.equals(this.getElementId(), other.getElementId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getPlayer(), this.getElementId());
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_tag", nullable = false)
    private Player player;

    @Column(name = "element_id", nullable = false)
    private Integer elementId;

    @Column(name = "element_type", nullable = false)
    private String elementType;

    @Column(name = "element_type_changed_at", nullable = true)
    private Instant elementTypeChangedAt;

    public PlayerHouseElement(
            @NonNull Player player,
            @NonNull Integer elementId,
            @NonNull String elementType) {
        if (elementType.isBlank()) {
            throw new IllegalArgumentException("Player house element type must not be blank");
        }
        this.player = player;
        this.elementId = elementId;
        this.elementType = elementType;
    }

    public void updateFrom(PlayerHouseElement source) {
        if (!this.elementId.equals(source.elementId)) {
            throw new IllegalArgumentException("Player house element IDs mismatch: " +
                    "currentElementId=" + elementId
                    + ", sourceElementId=" + source.elementId);
        }
        Instant now = Instant.now();

        if (!Objects.equals(this.elementType, source.elementType)) {
            this.elementType = source.elementType;
            this.elementTypeChangedAt = now;
        }
    }

}
