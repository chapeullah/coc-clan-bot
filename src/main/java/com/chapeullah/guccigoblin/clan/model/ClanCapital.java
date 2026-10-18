package com.chapeullah.guccigoblin.clan.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "clan_capitals")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClanCapital {

    @Id
    @Column(name = "clan_tag", nullable = false)
    private String clanTag;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clan_tag", nullable = false)
    private Clan clan;

    @Column(name = "capital_hall_level", nullable = false)
    private Integer capitalHallLevel;

    @Column(name = "capital_hall_level_changed_at", nullable = true)
    private Instant capitalHallLevelChangedAt;

    @Column(name = "clan_gold_sink_total", nullable = true)
    private Long clanGoldSinkTotal;

    @Column(name = "clan_gold_sink_total_changed_at", nullable = true)
    private Instant clanGoldSinkTotalChangedAt;

    @SuppressWarnings("FieldMayBeFinal")
    @OneToMany(
            mappedBy = "clanCapital",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<ClanCapitalDistrict> districts = new ArrayList<>();

    public ClanCapital(
            @NonNull Clan clan,
            @NonNull Integer capitalHallLevel,
            Long clanGoldSinkTotal) {
        this.clan = clan;
        this.capitalHallLevel = capitalHallLevel;
        this.clanGoldSinkTotal = clanGoldSinkTotal;
    }

    public void setCapitalHallLevel(Integer capitalHallLevel) {
        this.capitalHallLevel = capitalHallLevel;
    }

    public void setCapitalHallLevelChangedAt(Instant capitalHallLevelChangedAt) {
        this.capitalHallLevelChangedAt = capitalHallLevelChangedAt;
    }

    public void setClanGoldSinkTotal(Long clanGoldSinkTotal) {
        this.clanGoldSinkTotal = clanGoldSinkTotal;
    }

    public void setClanGoldSinkTotalChangedAt(Instant clanGoldSinkTotalChangedAt) {
        this.clanGoldSinkTotalChangedAt = clanGoldSinkTotalChangedAt;
    }
}
