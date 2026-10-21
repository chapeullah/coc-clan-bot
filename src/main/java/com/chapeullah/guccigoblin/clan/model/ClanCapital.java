package com.chapeullah.guccigoblin.clan.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "clan_capitals")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClanCapital {

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof ClanCapital other)) {
            return false;
        }
        return Objects.equals(this.getClanTag(), other.getClanTag());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getClanTag());
    }

    @Id
    @Column(name = "clan_tag", nullable = false)
    private String clanTag;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clan_tag", nullable = false)
    private Clan clan;

    @Setter
    @Column(name = "capital_hall_level", nullable = false)
    private Integer capitalHallLevel;

    @Setter
    @Column(name = "capital_hall_level_changed_at", nullable = true)
    private Instant capitalHallLevelChangedAt;

    @Setter
    @Column(name = "clan_gold_sink_total", nullable = true)
    private Long clanGoldSinkTotal;

    @Setter
    @Column(name = "clan_gold_sink_total_changed_at", nullable = true)
    private Instant clanGoldSinkTotalChangedAt;

    @SuppressWarnings("FieldMayBeFinal")
    @OneToMany(mappedBy = "clanCapital", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ClanCapitalDistrict> districts = new ArrayList<>();

    public ClanCapital(
            @NonNull Clan clan,
            @NonNull Integer capitalHallLevel,
            Long clanGoldSinkTotal) {
        this.clanTag = clan.getTag();
        this.clan = clan;
        this.capitalHallLevel = capitalHallLevel;
        this.clanGoldSinkTotal = clanGoldSinkTotal;
    }

}
