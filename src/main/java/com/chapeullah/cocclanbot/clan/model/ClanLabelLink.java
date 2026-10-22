package com.chapeullah.cocclanbot.clan.model;

import com.chapeullah.cocclanbot.label.clan.ClanLabel;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.Objects;

@Entity @Table(
        name = "clan_label_links",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_clan_label_links_clan_tag_label_id",
                columnNames = {"clan_tag", "label_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClanLabelLink {

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof ClanLabelLink other)) {
            return false;
        }
        return Objects.equals(this.getClan(), other.getClan())
                && Objects.equals(this.getLabel(), other.getLabel());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getClan(), this.getLabel());
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clan_tag", nullable = false)
    private Clan clan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "label_id", nullable = false)
    private ClanLabel label;

    public ClanLabelLink(
            @NonNull Clan clan,
            @NonNull ClanLabel label) {
        this.clan = clan;
        this.label = label;
    }

}

