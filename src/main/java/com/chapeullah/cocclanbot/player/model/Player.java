package com.chapeullah.cocclanbot.player.model;

import com.chapeullah.cocclanbot.builderbaseleague.BuilderBaseLeague;
import com.chapeullah.cocclanbot.clan.model.Clan;
import com.chapeullah.cocclanbot.leaguetier.LeagueTier;
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

@Entity @Table(name = "players")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Player {

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof Player other)) {
            return false;
        }
        return Objects.equals(this.getTag(), other.getTag());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getTag());
    }

    @Id
    @Column(name = "tag", nullable = false)
    private String tag;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "name_changed_at", nullable = true)
    private Instant nameChangedAt;

    @Column(name = "town_hall_level", nullable = false)
    private Integer townHallLevel;

    @Column(name = "town_hall_level_changed_at", nullable = true)
    private Instant townHallLevelChangedAt;

    @Column(name = "town_hall_weapon_level", nullable = true)
    private Integer townHallWeaponLevel;

    @Column(name = "town_hall_weapon_level_changed_at", nullable = true)
    private Instant townHallWeaponLevelChangedAt;

    @Column(name = "exp_level", nullable = false)
    private Integer expLevel;

    @Column(name = "exp_level_changed_at", nullable = true)
    private Instant expLevelChangedAt;

    @Column(name = "trophies", nullable = true)
    private Integer trophies;

    @Column(name = "trophies_changed_at", nullable = true)
    private Instant trophiesChangedAt;

    @Column(name = "best_trophies", nullable = true)
    private Integer bestTrophies;

    @Column(name = "best_trophies_changed_at", nullable = true)
    private Instant bestTrophiesChangedAt;

    @Column(name = "war_stars", nullable = true)
    private Integer warStars;

    @Column(name = "war_stars_changed_at", nullable = true)
    private Instant warStarsChangedAt;

    @Column(name = "attack_wins", nullable = true)
    private Integer attackWins;

    @Column(name = "attack_wins_changed_at", nullable = true)
    private Instant attackWinsChangedAt;

    @Column(name = "defense_wins", nullable = true)
    private Integer defenseWins;

    @Column(name = "defense_wins_changed_at", nullable = true)
    private Instant defenseWinsChangedAt;

    @Column(name = "builder_hall_level", nullable = true)
    private Integer builderHallLevel;

    @Column(name = "builder_hall_level_changed_at", nullable = true)
    private Instant builderHallLevelChangedAt;

    @Column(name = "builder_base_trophies", nullable = true)
    private Integer builderBaseTrophies;

    @Column(name = "builder_base_trophies_changed_at", nullable = true)
    private Instant builderBaseTrophiesChangedAt;

    @Column(name = "best_builder_base_trophies", nullable = true)
    private Integer bestBuilderBaseTrophies;

    @Column(name = "best_builder_base_trophies_changed_at", nullable = true)
    private Instant bestBuilderBaseTrophiesChangedAt;

    @Column(name = "role", nullable = true)
    private String role;

    @Column(name = "role_changed_at", nullable = true)
    private Instant roleChangedAt;

    @Column(name = "war_preference", nullable = true)
    private String warPreference;

    @Column(name = "war_preference_changed_at", nullable = true)
    private Instant warPreferenceChangedAt;

    @Column(name = "donations", nullable = true)
    private Integer donations;

    @Column(name = "donations_changed_at", nullable = true)
    private Instant donationsChangedAt;

    @Column(name = "donations_received", nullable = true)
    private Integer donationsReceived;

    @Column(name = "donations_received_changed_at", nullable = true)
    private Instant donationsReceivedChangedAt;

    @Column(name = "clan_capital_contributions", nullable = true)
    private Integer clanCapitalContributions;

    @Column(name = "clan_capital_contributions_changed_at", nullable = true)
    private Instant clanCapitalContributionsChangedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "clan_tag", nullable = true)
    private Clan clan;

    @Column(name = "clan_tag_changed_at", nullable = true)
    private Instant clanChangedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "league_tier_id", nullable = true)
    private LeagueTier leagueTier;

    @Column(name = "league_tier_id_changed_at", nullable = true)
    private Instant leagueTierChangedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "builder_base_league_id", nullable = true)
    private BuilderBaseLeague builderBaseLeague;

    @Column(name = "builder_base_league_id_changed_at", nullable = true)
    private Instant builderBaseLeagueChangedAt;

    @Column(name = "current_league_group_tag", nullable = true)
    private String currentLeagueGroupTag;

    @Column(name = "current_league_group_tag_changed_at", nullable = true)
    private Instant currentLeagueGroupTagChangedAt;

    @Column(name = "current_league_season_id", nullable = true)
    private Integer currentLeagueSeasonId;

    @Column(name = "current_league_season_id_changed_at", nullable = true)
    private Instant currentLeagueSeasonIdChangedAt;

    @Column(name = "previous_league_group_tag", nullable = true)
    private String previousLeagueGroupTag;

    @Column(name = "previous_league_group_tag_changed_at", nullable = true)
    private Instant previousLeagueGroupTagChangedAt;

    @Column(name = "previous_league_season_id", nullable = true)
    private Integer previousLeagueSeasonId;

    @Column(name = "previous_league_season_id_changed_at", nullable = true)
    private Instant previousLeagueSeasonIdChangedAt;

    @SuppressWarnings("FieldMayBeFinal")
    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlayerAchievement> achievements = new ArrayList<>();

    @SuppressWarnings("FieldMayBeFinal")
    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlayerHouseElement> houseElements = new ArrayList<>();

    @SuppressWarnings("FieldMayBeFinal")
    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlayerLabelLink> labelLinks = new ArrayList<>();

    @SuppressWarnings("FieldMayBeFinal")
    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlayerTroop> troops = new ArrayList<>();

    @SuppressWarnings("FieldMayBeFinal")
    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlayerHero> heroes = new ArrayList<>();

    @SuppressWarnings("FieldMayBeFinal")
    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlayerHeroEquipment> heroEquipment = new ArrayList<>();

    @SuppressWarnings("FieldMayBeFinal")
    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlayerSpell> spells = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Player(
            @NonNull String tag,
            @NonNull String name,
            @NonNull Integer townHallLevel,
            Integer townHallWeaponLevel,
            @NonNull Integer expLevel,
            Integer trophies,
            Integer bestTrophies,
            Integer warStars,
            Integer attackWins,
            Integer defenseWins,
            Integer builderHallLevel,
            Integer builderBaseTrophies,
            Integer bestBuilderBaseTrophies,
            String role,
            String warPreference,
            Integer donations,
            Integer donationsReceived,
            Integer clanCapitalContributions,
            Clan clan,
            LeagueTier leagueTier,
            BuilderBaseLeague builderBaseLeague,
            String currentLeagueGroupTag,
            Integer currentLeagueSeasonId,
            String previousLeagueGroupTag,
            Integer previousLeagueSeasonId,
            List<PlayerAchievement> achievements,
            List<PlayerHouseElement> houseElements,
            List<PlayerLabelLink> labelLinks,
            List<PlayerTroop> troops,
            List<PlayerHero> heroes,
            List<PlayerHeroEquipment> heroEquipment,
            List<PlayerSpell> spells) {
        if (!tag.matches("^#[A-Z0-9]+$")) {
            throw new IllegalArgumentException("Player tag must start with # followed by at least one uppercase letter or digit. Example: #ABC123");
        }
        if (name.isBlank()) {
            throw new IllegalArgumentException("Player name must not be blank");
        }
        if (townHallLevel < 1) {
            throw new IllegalArgumentException("Player town hall level must be at least 1");
        }
        if (townHallWeaponLevel != null && townHallWeaponLevel < 1) {
            throw new IllegalArgumentException("Player town hall weapon level must be at least 1");
        }
        if (expLevel < 1) {
            throw new IllegalArgumentException("Player exp level must be at least 1");
        }
        if (trophies != null && trophies < 0) {
            throw new IllegalArgumentException("Player trophies must not be negative");
        }
        if (bestTrophies != null && bestTrophies < 0) {
            throw new IllegalArgumentException("Player best trophies must not be negative");
        }
        if (warStars != null && warStars < 0) {
            throw new IllegalArgumentException("Player war stars must not be negative");
        }
        if (attackWins != null && attackWins < 0) {
            throw new IllegalArgumentException("Player attack wins must not be negative");
        }
        if (defenseWins != null && defenseWins < 0) {
            throw new IllegalArgumentException("Player defense wins must not be negative");
        }
        if (builderHallLevel != null && builderHallLevel < 1) {
            throw new IllegalArgumentException("Player builder hall level must be at least 1");
        }
        if (builderBaseTrophies != null && builderBaseTrophies < 0) {
            throw new IllegalArgumentException("Player builder base trophies must not be negative");
        }
        if (bestBuilderBaseTrophies != null && bestBuilderBaseTrophies < 0) {
            throw new IllegalArgumentException("Player best builder base trophies must not be negative");
        }
        if (role != null && role.isBlank()) {
            throw new IllegalArgumentException("Player role must not be blank");
        }
        if (warPreference != null && warPreference.isBlank()) {
            throw new IllegalArgumentException("Player war preference must not be blank");
        }
        if (donations != null && donations < 0) {
            throw new IllegalArgumentException("Player donations must not be negative");
        }
        if (donationsReceived != null && donationsReceived < 0) {
            throw new IllegalArgumentException("Player donations received must not be negative");
        }
        if (clanCapitalContributions != null && clanCapitalContributions < 0) {
            throw new IllegalArgumentException("Player clan capital contributions must not be negative");
        }
        if (currentLeagueGroupTag != null && currentLeagueGroupTag.isBlank()) {
            throw new IllegalArgumentException("Player current league group tag must not be blank");
        }
        if (previousLeagueGroupTag != null && previousLeagueGroupTag.isBlank()) {
            throw new IllegalArgumentException("Player previous league group tag must not be blank");
        }

        this.tag = tag;
        this.name = name;
        this.townHallLevel = townHallLevel;
        this.townHallWeaponLevel = townHallWeaponLevel;
        this.expLevel = expLevel;
        this.trophies = trophies;
        this.bestTrophies = bestTrophies;
        this.warStars = warStars;
        this.attackWins = attackWins;
        this.defenseWins = defenseWins;
        this.builderHallLevel = builderHallLevel;
        this.builderBaseTrophies = builderBaseTrophies;
        this.bestBuilderBaseTrophies = bestBuilderBaseTrophies;
        this.role = role;
        this.warPreference = warPreference;
        this.donations = donations;
        this.donationsReceived = donationsReceived;
        this.clanCapitalContributions = clanCapitalContributions;

        this.clan = clan;
        this.leagueTier = leagueTier;
        this.builderBaseLeague = builderBaseLeague;

        this.currentLeagueGroupTag = currentLeagueGroupTag;
        this.currentLeagueSeasonId = currentLeagueSeasonId;
        this.previousLeagueGroupTag = previousLeagueGroupTag;
        this.previousLeagueSeasonId = previousLeagueSeasonId;

        this.achievements = achievements;
        this.houseElements = houseElements;
        this.labelLinks = labelLinks;
        this.troops = troops;
        this.heroes = heroes;
        this.heroEquipment = heroEquipment;
        this.spells = spells;
    }

    public void updateFrom(Player source) {
        if (!this.tag.equals(source.tag)) {
            throw new IllegalArgumentException("Player tag mismatch");
        }

        Instant now = Instant.now();

        if (!Objects.equals(this.name, source.name)) {
            this.name = source.name;
            this.nameChangedAt = now;
        }

        if (!Objects.equals(this.townHallLevel, source.townHallLevel)) {
            this.townHallLevel = source.townHallLevel;
            this.townHallLevelChangedAt = now;
        }

        if (!Objects.equals(this.townHallWeaponLevel, source.townHallWeaponLevel)) {
            this.townHallWeaponLevel = source.townHallWeaponLevel;
            this.townHallWeaponLevelChangedAt = now;
        }

        if (!Objects.equals(this.expLevel, source.expLevel)) {
            this.expLevel = source.expLevel;
            this.expLevelChangedAt = now;
        }

        if (!Objects.equals(this.trophies, source.trophies)) {
            this.trophies = source.trophies;
            this.trophiesChangedAt = now;
        }

        if (!Objects.equals(this.bestTrophies, source.bestTrophies)) {
            this.bestTrophies = source.bestTrophies;
            this.bestTrophiesChangedAt = now;
        }

        if (!Objects.equals(this.warStars, source.warStars)) {
            this.warStars = source.warStars;
            this.warStarsChangedAt = now;
        }

        if (!Objects.equals(this.attackWins, source.attackWins)) {
            this.attackWins = source.attackWins;
            this.attackWinsChangedAt = now;
        }

        if (!Objects.equals(this.defenseWins, source.defenseWins)) {
            this.defenseWins = source.defenseWins;
            this.defenseWinsChangedAt = now;
        }

        if (!Objects.equals(this.builderHallLevel, source.builderHallLevel)) {
            this.builderHallLevel = source.builderHallLevel;
            this.builderHallLevelChangedAt = now;
        }

        if (!Objects.equals(this.builderBaseTrophies, source.builderBaseTrophies)) {
            this.builderBaseTrophies = source.builderBaseTrophies;
            this.builderBaseTrophiesChangedAt = now;
        }

        if (!Objects.equals(this.bestBuilderBaseTrophies, source.bestBuilderBaseTrophies)) {
            this.bestBuilderBaseTrophies = source.bestBuilderBaseTrophies;
            this.bestBuilderBaseTrophiesChangedAt = now;
        }

        if (!Objects.equals(this.role, source.role)) {
            this.role = source.role;
            this.roleChangedAt = now;
        }

        if (!Objects.equals(this.warPreference, source.warPreference)) {
            this.warPreference = source.warPreference;
            this.warPreferenceChangedAt = now;
        }

        if (!Objects.equals(this.donations, source.donations)) {
            this.donations = source.donations;
            this.donationsChangedAt = now;
        }

        if (!Objects.equals(this.donationsReceived, source.donationsReceived)) {
            this.donationsReceived = source.donationsReceived;
            this.donationsReceivedChangedAt = now;
        }

        if (!Objects.equals(this.clanCapitalContributions, source.clanCapitalContributions)) {
            this.clanCapitalContributions = source.clanCapitalContributions;
            this.clanCapitalContributionsChangedAt = now;
        }

        if (!Objects.equals(this.clan, source.clan)) {
            this.clan = source.clan;
            this.clanChangedAt = now;
        }

        if (!Objects.equals(this.leagueTier, source.leagueTier)) {
            this.leagueTier = source.leagueTier;
            this.leagueTierChangedAt = now;
        }

        if (!Objects.equals(this.builderBaseLeague, source.builderBaseLeague)) {
            this.builderBaseLeague = source.builderBaseLeague;
            this.builderBaseLeagueChangedAt = now;
        }

        if (!Objects.equals(this.currentLeagueGroupTag, source.currentLeagueGroupTag)) {
            this.currentLeagueGroupTag = source.currentLeagueGroupTag;
            this.currentLeagueGroupTagChangedAt = now;
        }

        if (!Objects.equals(this.currentLeagueSeasonId, source.currentLeagueSeasonId)) {
            this.currentLeagueSeasonId = source.currentLeagueSeasonId;
            this.currentLeagueSeasonIdChangedAt = now;
        }

        if (!Objects.equals(this.previousLeagueGroupTag, source.previousLeagueGroupTag)) {
            this.previousLeagueGroupTag = source.previousLeagueGroupTag;
            this.previousLeagueGroupTagChangedAt = now;
        }

        if (!Objects.equals(this.previousLeagueSeasonId, source.previousLeagueSeasonId)) {
            this.previousLeagueSeasonId = source.previousLeagueSeasonId;
            this.previousLeagueSeasonIdChangedAt = now;
        }
    }

}
