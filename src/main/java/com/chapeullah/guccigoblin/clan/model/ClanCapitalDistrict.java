package com.chapeullah.guccigoblin.clan.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.time.Instant;

@Entity
@Table(
        name = "clan_capital_districts",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_clan_capital_districts_clan_tag_district_id",
                columnNames = {"clan_tag", "district_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClanCapitalDistrict {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clan_tag", nullable = false)
    private ClanCapital clanCapital;

    @Column(name = "district_id", nullable = false)
    private Long districtId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "district_hall_level", nullable = false)
    private Integer districtHallLevel;

    @Column(name = "district_hall_level_changed_at", nullable = true)
    private Instant districtHallLevelChangedAt;

    public ClanCapitalDistrict(
            @NonNull ClanCapital clanCapital,
            @NonNull Long districtId,
            @NonNull String name,
            @NonNull Integer districtHallLevel) {
        this.clanCapital = clanCapital;
        this.districtId = districtId;
        this.name = name;
        this.districtHallLevel = districtHallLevel;
    }

    public void setDistrictHallLevel(@NonNull Integer districtHallLevel) {
        this.districtHallLevel = districtHallLevel;
    }

    public void setDistrictHallLevelChangedAt(@NonNull Instant districtHallLevelChangedAt) {
        this.districtHallLevelChangedAt = districtHallLevelChangedAt;
    }

}
