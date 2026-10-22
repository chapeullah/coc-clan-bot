package com.chapeullah.cocclanbot.clan.model;

import com.chapeullah.cocclanbot.capitalleague.CapitalLeague;
import com.chapeullah.cocclanbot.leaguetier.LeagueTier;
import com.chapeullah.cocclanbot.location.Location;
import com.chapeullah.cocclanbot.member.model.Member;
import com.chapeullah.cocclanbot.warleague.WarLeague;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity @Table(name = "clans")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Clan {

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof Clan other)) {
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

    @Setter
    @Column(name = "type", nullable = false)
    private String type;

    @Setter
    @Column(name = "type_changed_at", nullable = true)
    private Instant typeChangedAt;

    @Setter
    @Column(name = "description", nullable = true)
    private String description;

    @Setter
    @Column(name = "description_changed_at", nullable = true)
    private Instant descriptionChangedAt;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "location_id", nullable = true)
    private Location location;

    @Setter
    @Column(name = "location_changed_at", nullable = true)
    private Instant locationChangedAt;

    @Column(name = "is_family_friendly", nullable = true)
    private Boolean isFamilyFriendly;

    @Setter
    @Embedded
    private ClanBadgeUrls clanBadgeUrls;

    @Setter
    @Column(name = "clan_badge_urls_changed_at", nullable = true)
    private Instant clanBadgeUrlsChangedAt;

    @Setter
    @Column(name = "clan_level", nullable = true)
    private Integer clanLevel;

    @Setter
    @Column(name = "clan_level_changed_at", nullable = true)
    private Instant clanLevelChangedAt;

    @Setter
    @Column(name = "clan_points", nullable = true)
    private Integer clanPoints;

    @Setter
    @Column(name = "clan_points_changed_at", nullable = true)
    private Instant clanPointsChangedAt;

    @Setter
    @Column(name = "clan_builder_base_points", nullable = true)
    private Integer clanBuilderBasePoints;

    @Setter
    @Column(name = "clan_builder_base_points_changed_at", nullable = true)
    private Instant clanBuilderBasePointsChangedAt;

    @Setter
    @Column(name = "clan_capital_points", nullable = true)
    private Integer clanCapitalPoints;

    @Setter
    @Column(name = "clan_capital_points_changed_at", nullable = true)
    private Instant clanCapitalPointsChangedAt;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "capital_league_id", nullable = true)
    private CapitalLeague capitalLeague;

    @Setter
    @Column(name = "capital_league_changed_at", nullable = true)
    private Instant capitalLeagueChangedAt;

    @Setter
    @Column(name = "required_trophies", nullable = true)
    private Integer requiredTrophies;

    @Setter
    @Column(name = "required_trophies_changed_at", nullable = true)
    private Instant requiredTrophiesChangedAt;

    @Setter
    @Column(name = "war_frequency", nullable = true)
    private String warFrequency;

    @Setter
    @Column(name = "war_frequency_changed_at", nullable = true)
    private Instant warFrequencyChangedAt;

    @Setter
    @Column(name = "war_win_streak", nullable = true)
    private Integer warWinStreak;

    @Setter
    @Column(name = "war_win_streak_changed_at", nullable = true)
    private Instant warWinStreakChangedAt;

    @Setter
    @Column(name = "war_wins", nullable = true)
    private Integer warWins;

    @Setter
    @Column(name = "war_wins_changed_at", nullable = true)
    private Instant warWinsChangedAt;

    @Setter
    @Column(name = "war_ties", nullable = true)
    private Integer warTies;

    @Setter
    @Column(name = "war_ties_changed_at", nullable = true)
    private Instant warTiesChangedAt;

    @Setter
    @Column(name = "war_losses", nullable = true)
    private Integer warLosses;

    @Setter
    @Column(name = "war_losses_changed_at", nullable = true)
    private Instant warLossesChangedAt;

    @Setter
    @Column(name = "is_war_log_public", nullable = true)
    private Boolean isWarLogPublic;

    @Setter
    @Column(name = "is_war_log_public_changed_at", nullable = true)
    private Instant isWarLogPublicChangedAt;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "war_league_id", nullable = true)
    private WarLeague warLeague;

    @Setter
    @Column(name = "war_league_changed_at", nullable = true)
    private Instant warLeagueChangedAt;

    @Setter
    @Column(name = "member_count", nullable = true)
    private Integer memberCount;

    @Setter
    @Column(name = "member_count_changed_at", nullable = true)
    private Instant memberCountChangedAt;

    @SuppressWarnings("FieldMayBeFinal")
    @OneToMany(
            mappedBy = "clan",
            cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<Member> memberList = new ArrayList<>();

    @Setter
    @Column(name = "member_list_changed_at", nullable = true)
    private Instant memberListChangedAt;

    @Setter
    @Column(name = "member_list_synced_at", nullable = true)
    private Instant memberListSyncedAt;

    @SuppressWarnings("FieldMayBeFinal")
    @OneToMany(mappedBy = "clan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ClanLabelLink> labelLinks = new ArrayList<>();

    @Setter
    @Column(name = "label_links_changed_at", nullable = true)
    private Instant labelLinksChangedAt;

    @Setter
    @Column(name = "required_builder_base_trophies", nullable = true)
    private Integer requiredBuilderBaseTrophies;

    @Setter
    @Column(name = "required_builder_base_trophies_changed_at", nullable = true)
    private Instant requiredBuilderBaseTrophiesChangedAt;

    @Setter
    @Column(name = "required_townhall_level", nullable = true)
    private Integer requiredTownHallLevel;

    @Setter
    @Column(name = "required_townhall_level_changed_at", nullable = true)
    private Instant requiredTownHallLevelChangedAt;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "required_league_tier_id", nullable = true)
    private LeagueTier requiredLeagueTier;

    @Setter
    @Column(name = "required_league_tier_changed_at", nullable = true)
    private Instant requiredLeagueTierChangedAt;

    @Setter
    @OneToOne(mappedBy = "clan")
    private ClanCapital clanCapital;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "chat_language_id", nullable = true)
    private ClanChatLanguage clanChatLanguage;

    @Setter
    @Column(name = "clan_chat_language_changed_at", nullable = true)
    private Instant clanChatLanguageChangedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = true)
    private Instant updatedAt;

    public Clan(
            @NonNull String tag,
            @NonNull String name,
            @NonNull String type,
            String description,
            Location location,
            Boolean isFamilyFriendly,
            Integer clanLevel,
            Integer clanPoints,
            Integer clanBuilderBasePoints,
            Integer clanCapitalPoints,
            CapitalLeague capitalLeague,
            Integer requiredTrophies,
            String warFrequency,
            Integer warWinStreak,
            Integer warWins,
            Integer warTies,
            Integer warLosses,
            Boolean isWarLogPublic,
            WarLeague warLeague,
            Integer memberCount,
            Integer requiredBuilderBaseTrophies,
            Integer requiredTownHallLevel,
            LeagueTier requiredLeagueTier,
            ClanChatLanguage clanChatLanguage) {
        this.tag = tag;
        this.name = name;
        this.type = type;
        this.description = description;
        this.location = location;
        this.isFamilyFriendly = isFamilyFriendly;
        this.clanLevel = clanLevel;
        this.clanPoints = clanPoints;
        this.clanBuilderBasePoints = clanBuilderBasePoints;
        this.clanCapitalPoints = clanCapitalPoints;
        this.capitalLeague = capitalLeague;
        this.requiredTrophies = requiredTrophies;
        this.warFrequency = warFrequency;
        this.warWinStreak = warWinStreak;
        this.warWins = warWins;
        this.warTies = warTies;
        this.warLosses = warLosses;
        this.isWarLogPublic = isWarLogPublic;
        this.warLeague = warLeague;
        this.memberCount = memberCount;
        this.requiredBuilderBaseTrophies = requiredBuilderBaseTrophies;
        this.requiredTownHallLevel = requiredTownHallLevel;
        this.requiredLeagueTier = requiredLeagueTier;
        this.clanChatLanguage = clanChatLanguage;
    }

}
