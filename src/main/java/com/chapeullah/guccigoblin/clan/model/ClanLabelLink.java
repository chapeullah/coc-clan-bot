package com.chapeullah.guccigoblin.clan.model;

import com.chapeullah.guccigoblin.label.clan.ClanLabel;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Entity @Table(
        name = "clan_label_links",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_clan_label_links_clan_tag_label_id",
                columnNames = {"clan_tag", "label_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClanLabelLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id",
            nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clan_tag", nullable = false)
    private Clan clan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "label_id", nullable = false)
    private ClanLabel clanLabel;

    public ClanLabelLink(
            @NonNull Clan clan,
            @NonNull ClanLabel clanLabel) {
        this.clan = clan;
        this.clanLabel = clanLabel;
    }

}

