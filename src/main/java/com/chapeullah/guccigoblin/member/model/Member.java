package com.chapeullah.guccigoblin.member.model;

import com.chapeullah.guccigoblin.builderbaseleague.BuilderBaseLeague;
import com.chapeullah.guccigoblin.clan.model.Clan;
import com.chapeullah.guccigoblin.leaguetier.LeagueTier;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity @Table(
        name = "members",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_members_tag_clan_tag",
                columnNames = {"tag", "clan_tag"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tag", nullable = false)
    private String tag;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clan_tag", nullable = false)
    private Clan clan;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "name_changed_at", nullable = true)
    private Instant nameChangedAt;

    @Column(name = "role", nullable = false)
    private String role;

    @Column(name = "role_changed_at", nullable = true)
    private Instant roleChangedAt;

    @Column(name = "town_hall_level", nullable = false)
    private Integer townHallLevel;

    @Column(name = "town_hall_level_changed_at", nullable = true)
    private Instant townHallLevelChangedAt;

    @Column(name = "exp_level", nullable = false)
    private Integer expLevel;

    @Column(name = "exp_level_changed_at", nullable = true)
    private Instant expLevelChangedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "league_tier_id", nullable = true)
    private LeagueTier leagueTier;

    @Column(name = "league_tier_id_changed_at", nullable = true)
    private Instant leagueTierChangedAt;

    @Column(name = "trophies", nullable = true)
    private Integer trophies;

    @Column(name = "trophies_changed_at", nullable = true)
    private Instant trophiesChangedAt;

    @Column(name = "builder_base_trophies", nullable = true)
    private Integer builderBaseTrophies;

    @Column(name = "builder_base_trophies_changed_at", nullable = true)
    private Instant builderBaseTrophiesChangedAt;

    @Column(name = "clan_rank", nullable = false)
    private Integer clanRank;

    @Column(name = "clan_rank_changed_at", nullable = true)
    private Instant clanRankChangedAt;

    @Column(name = "previous_clan_rank", nullable = true)
    private Integer previousClanRank;

    @Column(name = "previous_clan_rank_changed_at", nullable = true)
    private Instant previousClanRankChangedAt;

    @Column(name = "donations", nullable = false)
    private Integer donations;

    @Column(name = "donations_changed_at", nullable = true)
    private Instant donationsChangedAt;

    @Column(name = "total_donations", nullable = false)
    private Integer totalDonations;

    @Column(name = "total_donations_changed_at", nullable = true)
    private Instant totalDonationsChangedAt;

    @Column(name = "donations_received", nullable = false)
    private Integer donationsReceived;

    @Column(name = "donations_received_changed_at", nullable = true)
    private Instant donationsReceivedChangedAt;

    @Column(name = "total_donations_received", nullable = false)
    private Integer totalDonationsReceived;

    @Column(name = "total_donations_received_changed_at", nullable = true)
    private Instant totalDonationsReceivedChangedAt;

    @SuppressWarnings("FieldMayBeFinal")
    @OneToMany(
            mappedBy = "member",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<MemberHouseElement> memberHouseElements = new ArrayList<>();

    @Column(name = "member_house_elements_changed_at", nullable = true)
    private Instant memberHouseElementsChangedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "builder_base_league_id", nullable = true)
    private BuilderBaseLeague builderBaseLeague;

    @Column(name = "builder_base_league_id_changed_at")
    private Instant builderBaseLeagueChangedAt;

    @Column(name = "in_clan", nullable = false)
    private Boolean inClan;

    @Column(name = "in_clan_changed_at", nullable = true)
    private Instant inClanChangedAt;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    @Column(name = "left_at", nullable = true)
    private Instant leftAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Member(
            @NonNull String tag,
            @NonNull Clan clan,
            @NonNull String name,
            @NonNull String role,
            @NonNull Integer townHallLevel,
            @NonNull Integer expLevel,
            LeagueTier leagueTier,
            Integer trophies,
            Integer builderBaseTrophies,
            @NonNull Integer clanRank,
            Integer previousClanRank,
            @NonNull Integer donations,
            @NonNull Integer donationsReceived,
            BuilderBaseLeague builderBaseLeague) {
        this.tag = tag;
        this.clan = clan;
        this.name = name;
        this.role = role;
        this.townHallLevel = townHallLevel;
        this.expLevel = expLevel;
        this.leagueTier = leagueTier;
        this.trophies = trophies;
        this.builderBaseTrophies = builderBaseTrophies;
        this.clanRank = clanRank;
        this.previousClanRank = previousClanRank;
        this.donations = donations;
        this.totalDonations = donations;
        this.donationsReceived = donationsReceived;
        this.totalDonationsReceived = donationsReceived;
        this.builderBaseLeague = builderBaseLeague;
        this.inClan = true;
        this.joinedAt = Instant.now();
    }

    public void update(
            @NonNull String name,
            @NonNull String role,
            @NonNull Integer townHallLevel,
            @NonNull Integer expLevel,
            LeagueTier leagueTier,
            Integer trophies,
            Integer builderBaseTrophies,
            @NonNull Integer clanRank,
            Integer previousClanRank,
            @NonNull Integer donations,
            @NonNull Integer donationsReceived,
            BuilderBaseLeague builderBaseLeague,
            @NonNull Instant now) {
        if (!Objects.equals(this.name, name)) {
            this.name = name;
            this.nameChangedAt = now;
        }
        if (!Objects.equals(this.role, role)) {
            this.role = role;
            this.roleChangedAt = now;
        }
        if (!Objects.equals(this.townHallLevel, townHallLevel)) {
            this.townHallLevel = townHallLevel;
            this.townHallLevelChangedAt = now;
        }
        if (!Objects.equals(this.expLevel, expLevel)) {
            this.expLevel = expLevel;
            this.expLevelChangedAt = now;
        }
        if (!Objects.equals(this.leagueTier, leagueTier)) {
            this.leagueTier = leagueTier;
            this.leagueTierChangedAt = now;
        }
        if (!Objects.equals(this.trophies, trophies)) {
            this.trophies = trophies;
            this.trophiesChangedAt = now;
        }
        if (!Objects.equals(this.builderBaseTrophies, builderBaseTrophies)) {
            this.builderBaseTrophies = builderBaseTrophies;
            this.builderBaseTrophiesChangedAt = now;
        }
        if (!Objects.equals(this.clanRank, clanRank)) {
            this.clanRank = clanRank;
            this.clanRankChangedAt = now;
        }
        if (!Objects.equals(this.previousClanRank, previousClanRank)) {
            this.previousClanRank = previousClanRank;
            this.previousClanRankChangedAt = now;
        }

        int addedDonations = (!Boolean.TRUE.equals(this.inClan) || donations < this.donations)
                ? donations
                : donations - this.donations;
        if (!Objects.equals(this.donations, donations)) {
            this.donations = donations;
            this.donationsChangedAt = now;
        }
        if (addedDonations > 0) {
            this.totalDonations += addedDonations;
            this.totalDonationsChangedAt = now;
        }

        int addedDonationsReceived =
                (!Boolean.TRUE.equals(this.inClan) || donationsReceived < this.donationsReceived)
                        ? donationsReceived
                        : donationsReceived - this.donationsReceived;
        if (!Objects.equals(this.donationsReceived, donationsReceived)) {
            this.donationsReceived = donationsReceived;
            this.donationsReceivedChangedAt = now;
        }
        if (addedDonationsReceived > 0) {
            this.totalDonationsReceived += addedDonationsReceived;
            this.totalDonationsReceivedChangedAt = now;
        }

        if (!Objects.equals(this.builderBaseLeague, builderBaseLeague)) {
            this.builderBaseLeague = builderBaseLeague;
            this.builderBaseLeagueChangedAt = now;
        }
    }

    public void leave(@NonNull Instant now) {
        if (!Boolean.TRUE.equals(this.inClan)) {
            return;
        }
        this.inClan = false;
        this.inClanChangedAt = now;
        this.leftAt = now;
    }

    public void rejoin(@NonNull Instant now) {
        if (Boolean.TRUE.equals(this.inClan)) {
            return;
        }
        this.inClan = true;
        this.inClanChangedAt = now;
        this.joinedAt = now;
    }

    public void markHouseElementsChanged(@NonNull Instant now) {
        this.memberHouseElementsChangedAt = now;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof Member other)) {
            return false;
        }
        return Objects.equals(this.getTag(), other.getTag())
                && Objects.equals(this.getClan().getTag(), other.getClan().getTag());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getTag(), this.getClan().getTag());
    }

}
