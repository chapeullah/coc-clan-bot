package com.chapeullah.guccigoblin.player.model;

import com.chapeullah.guccigoblin.label.player.PlayerLabel;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Entity @Table(
        name = "player_label_links",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_player_label_links_player_label",
                columnNames = {"player_tag", "label_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlayerLabelLink {

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
    private PlayerLabel playerLabel;

    public PlayerLabelLink(
            @NonNull Player player,
            @NonNull PlayerLabel playerLabel) {
        this.player = player;
        this.playerLabel = playerLabel;
    }

}
