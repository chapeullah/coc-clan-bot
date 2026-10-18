package com.chapeullah.guccigoblin.clan.model;

import com.chapeullah.guccigoblin.capitalleague.CapitalLeague;
import com.chapeullah.guccigoblin.leaguetier.LeagueTier;
import com.chapeullah.guccigoblin.location.Location;
import com.chapeullah.guccigoblin.member.model.Member;
import com.chapeullah.guccigoblin.warleague.WarLeague;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity @Table(name = "clans")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Clan {

    @Id
    @Column(name = "tag", nullable = false)
    private String tag;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "type_changed_at", nullable = true)
    private Instant typeChangedAt;

    @Column(name = "description", nullable = true)
    private String description;

    @Column(name = "description_changed_at", nullable = true)
    private Instant descriptionChangedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "location_id", nullable = true)
    private Location location;

    @Column(name = "location_changed_at", nullable = true)
    private Instant locationChangedAt;

    @Column(name = "is_family_friendly", nullable = true)
    private Boolean isFamilyFriendly;

    @Embedded
    private ClanBadgeUrls clanBadgeUrls;

    @Column(name = "clan_badge_urls_changed_at", nullable = true)
    private Instant clanBadgeUrlsChangedAt;

    @Column(name = "clan_level", nullable = true)
    private Integer clanLevel;

    @Column(name = "clan_level_changed_at", nullable = true)
    private Instant clanLevelChangedAt;

    @Column(name = "clan_points", nullable = true)
    private Integer clanPoints;

    @Column(name = "clan_points_changed_at", nullable = true)
    private Instant clanPointsChangedAt;

    @Column(name = "clan_builder_base_points", nullable = true)
    private Integer clanBuilderBasePoints;

    @Column(name = "clan_builder_base_points_changed_at", nullable = true)
    private Instant clanBuilderBasePointsChangedAt;

    @Column(name = "clan_capital_points", nullable = true)
    private Integer clanCapitalPoints;

    @Column(name = "clan_capital_points_changed_at", nullable = true)
    private Instant clanCapitalPointsChangedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "capital_league_id", nullable = true)
    private CapitalLeague capitalLeague;

    @Column(name = "capital_league_changed_at", nullable = true)
    private Instant capitalLeagueChangedAt;

    @Column(name = "required_trophies", nullable = true)
    private Integer requiredTrophies;

    @Column(name = "required_trophies_changed_at", nullable = true)
    private Instant requiredTrophiesChangedAt;

    @Column(name = "war_frequency", nullable = true)
    private String warFrequency;

    @Column(name = "war_frequency_changed_at", nullable = true)
    private Instant warFrequencyChangedAt;

    @Column(name = "war_win_streak", nullable = true)
    private Integer warWinStreak;

    @Column(name = "war_win_streak_changed_at", nullable = true)
    private Instant warWinStreakChangedAt;

    @Column(name = "war_wins", nullable = true)
    private Integer warWins;

    @Column(name = "war_wins_changed_at", nullable = true)
    private Instant warWinsChangedAt;

    @Column(name = "war_ties", nullable = true)
    private Integer warTies;

    @Column(name = "war_ties_changed_at", nullable = true)
    private Instant warTiesChangedAt;

    @Column(name = "war_losses", nullable = true)
    private Integer warLosses;

    @Column(name = "war_losses_changed_at", nullable = true)
    private Instant warLossesChangedAt;

    @Column(name = "is_war_log_public", nullable = true)
    private Boolean isWarLogPublic;

    @Column(name = "is_war_log_public_changed_at", nullable = true)
    private Instant isWarLogPublicChangedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "war_league_id", nullable = true)
    private WarLeague warLeague;

    @Column(name = "war_league_changed_at", nullable = true)
    private Instant warLeagueChangedAt;

    @Column(name = "member_count", nullable = true)
    private Integer memberCount;

    @Column(name = "member_count_changed_at", nullable = true)
    private Instant memberCountChangedAt;

    @SuppressWarnings("FieldMayBeFinal")
    @OneToMany(
            mappedBy = "clan",
            cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<Member> memberList = new ArrayList<>();

    @Column(name = "member_list_changed_at", nullable = true)
    private Instant memberListChangedAt;

    @SuppressWarnings("FieldMayBeFinal")
    @OneToMany(mappedBy = "clan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ClanLabelLink> labelLinks = new ArrayList<>();

    @Column(name = "label_links_changed_at", nullable = true)
    private Instant labelLinksChangedAt;

    @Column(name = "required_builder_base_trophies", nullable = true)
    private Integer requiredBuilderBaseTrophies;

    @Column(name = "required_builder_base_trophies_changed_at", nullable = true)
    private Instant requiredBuilderBaseTrophiesChangedAt;

    @Column(name = "required_townhall_level", nullable = true)
    private Integer requiredTownHallLevel;

    @Column(name = "required_townhall_level_changed_at", nullable = true)
    private Instant requiredTownHallLevelChangedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "required_league_tier_id", nullable = true)
    private LeagueTier requiredLeagueTier;

    @Column(name = "required_league_tier_changed_at", nullable = true)
    private Instant requiredLeagueTierChangedAt;

    @OneToOne(mappedBy = "clan")
    private ClanCapital clanCapital;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "chat_language_id", nullable = true)
    private ClanChatLanguage clanChatLanguage;

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

    public void setType(String type) {
        this.type = type;
    }

    public void setTypeChangedAt(Instant typeChangedAt) {
        this.typeChangedAt = typeChangedAt;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDescriptionChangedAt(Instant descriptionChangedAt) {
        this.descriptionChangedAt = descriptionChangedAt;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public void setLocationChangedAt(Instant locationChangedAt) {
        this.locationChangedAt = locationChangedAt;
    }

    public void setClanBadgeUrls(ClanBadgeUrls clanBadgeUrls) {
        this.clanBadgeUrls = clanBadgeUrls;
    }

    public void setClanBadgeUrlsChangedAt(Instant clanBadgeUrlsChangedAt) {
        this.clanBadgeUrlsChangedAt = clanBadgeUrlsChangedAt;
    }

    public void setClanLevel(Integer clanLevel) {
        this.clanLevel = clanLevel;
    }

    public void setClanLevelChangedAt(Instant clanLevelChangedAt) {
        this.clanLevelChangedAt = clanLevelChangedAt;
    }

    public void setClanPoints(Integer clanPoints) {
        this.clanPoints = clanPoints;
    }

    public void setClanPointsChangedAt(Instant clanPointsChangedAt) {
        this.clanPointsChangedAt = clanPointsChangedAt;
    }

    public void setClanBuilderBasePoints(Integer clanBuilderBasePoints) {
        this.clanBuilderBasePoints = clanBuilderBasePoints;
    }

    public void setClanBuilderBasePointsChangedAt(Instant clanBuilderBasePointsChangedAt) {
        this.clanBuilderBasePointsChangedAt = clanBuilderBasePointsChangedAt;
    }

    public void setClanCapitalPoints(Integer clanCapitalPoints) {
        this.clanCapitalPoints = clanCapitalPoints;
    }

    public void setClanCapitalPointsChangedAt(Instant clanCapitalPointsChangedAt) {
        this.clanCapitalPointsChangedAt = clanCapitalPointsChangedAt;
    }

    public void setCapitalLeague(CapitalLeague capitalLeague) {
        this.capitalLeague = capitalLeague;
    }

    public void setCapitalLeagueChangedAt(Instant capitalLeagueChangedAt) {
        this.capitalLeagueChangedAt = capitalLeagueChangedAt;
    }

    public void setRequiredTrophies(Integer requiredTrophies) {
        this.requiredTrophies = requiredTrophies;
    }

    public void setRequiredTrophiesChangedAt(Instant requiredTrophiesChangedAt) {
        this.requiredTrophiesChangedAt = requiredTrophiesChangedAt;
    }

    public void setWarFrequency(String warFrequency) {
        this.warFrequency = warFrequency;
    }

    public void setWarFrequencyChangedAt(Instant warFrequencyChangedAt) {
        this.warFrequencyChangedAt = warFrequencyChangedAt;
    }

    public void setWarWinStreak(Integer warWinStreak) {
        this.warWinStreak = warWinStreak;
    }

    public void setWarWinStreakChangedAt(Instant warWinStreakChangedAt) {
        this.warWinStreakChangedAt = warWinStreakChangedAt;
    }

    public void setWarWins(Integer warWins) {
        this.warWins = warWins;
    }

    public void setWarWinsChangedAt(Instant warWinsChangedAt) {
        this.warWinsChangedAt = warWinsChangedAt;
    }

    public void setWarTies(Integer warTies) {
        this.warTies = warTies;
    }

    public void setWarTiesChangedAt(Instant warTiesChangedAt) {
        this.warTiesChangedAt = warTiesChangedAt;
    }

    public void setWarLosses(Integer warLosses) {
        this.warLosses = warLosses;
    }

    public void setWarLossesChangedAt(Instant warLossesChangedAt) {
        this.warLossesChangedAt = warLossesChangedAt;
    }

    public void setIsWarLogPublic(Boolean warLogPublic) {
        this.isWarLogPublic = warLogPublic;
    }

    public void setIsWarLogPublicChangedAt(Instant isWarLogPublicChangedAt) {
        this.isWarLogPublicChangedAt = isWarLogPublicChangedAt;
    }

    public void setWarLeague(WarLeague warLeague) {
        this.warLeague = warLeague;
    }

    public void setWarLeagueChangedAt(Instant warLeagueChangedAt) {
        this.warLeagueChangedAt = warLeagueChangedAt;
    }

    public void setMemberCount(Integer memberCount) {
        this.memberCount = memberCount;
    }

    public void setMemberCountChangedAt(Instant memberCountChangedAt) {
        this.memberCountChangedAt = memberCountChangedAt;
    }

    public void setMemberList(List<Member> memberList) {
        this.memberList = memberList;
    }

    public void setMemberListChangedAt(Instant memberListChangedAt) {
        this.memberListChangedAt = memberListChangedAt;
    }

    public void setLabelLinks(List<ClanLabelLink> labelLinks) {
        this.labelLinks = labelLinks;
    }

    public void setLabelLinksChangedAt(Instant labelLinksChangedAt) {
        this.labelLinksChangedAt = labelLinksChangedAt;
    }

    public void setRequiredBuilderBaseTrophies(Integer requiredBuilderBaseTrophies) {
        this.requiredBuilderBaseTrophies = requiredBuilderBaseTrophies;
    }

    public void setRequiredBuilderBaseTrophiesChangedAt(Instant requiredBuilderBaseTrophiesChangedAt) {
        this.requiredBuilderBaseTrophiesChangedAt = requiredBuilderBaseTrophiesChangedAt;
    }

    public void setRequiredTownHallLevel(Integer requiredTownHallLevel) {
        this.requiredTownHallLevel = requiredTownHallLevel;
    }

    public void setRequiredTownHallLevelChangedAt(Instant requiredTownHallLevelChangedAt) {
        this.requiredTownHallLevelChangedAt = requiredTownHallLevelChangedAt;
    }

    public void setRequiredLeagueTier(LeagueTier requiredLeagueTier) {
        this.requiredLeagueTier = requiredLeagueTier;
    }

    public void setRequiredLeagueTierChangedAt(Instant requiredLeagueTierChangedAt) {
        this.requiredLeagueTierChangedAt = requiredLeagueTierChangedAt;
    }

    public void setClanCapital(ClanCapital clanCapital) {
        this.clanCapital = clanCapital;
    }


    public void setClanChatLanguage(ClanChatLanguage clanChatLanguage) {
        this.clanChatLanguage = clanChatLanguage;
    }

    public void setClanChatLanguageChangedAt(Instant clanChatLanguageChangedAt) {
        this.clanChatLanguageChangedAt = clanChatLanguageChangedAt;
    }

}
