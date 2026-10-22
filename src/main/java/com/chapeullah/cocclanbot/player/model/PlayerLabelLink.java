package com.chapeullah.cocclanbot.player.model;

import com.chapeullah.cocclanbot.label.player.PlayerLabel;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.Objects;

@Entity @Table(
        name = "player_label_links",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_player_label_links_player_label",
                columnNames = {"player_tag", "label_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlayerLabelLink {

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof PlayerLabelLink other)) {
            return false;
        }
        return Objects.equals(this.getPlayer(), other.getPlayer())
                && Objects.equals(this.getLabel(), other.getLabel());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getPlayer(), this.getLabel());
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id",
            nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_tag", nullable = false)
    private Player player;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "label_id", nullable = false)
    private PlayerLabel label;

    public PlayerLabelLink(
            @NonNull Player player,
            @NonNull PlayerLabel label) {
        this.player = player;
        this.label = label;
    }

}
