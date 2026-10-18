package com.chapeullah.guccigoblin.clan.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClanBadgeUrls {

    @Column(name = "badge_small_url", length = 512)
    private String small;

    @Column(name = "badge_large_url", length = 512)
    private String large;

    @Column(name = "badge_medium_url", length = 512)
    private String medium;

    public ClanBadgeUrls(String small, String large, String medium) {
        this.small = small;
        this.large = large;
        this.medium = medium;
    }

    public static ClanBadgeUrls of(String small, String large, String medium) {
        if (small == null && large == null && medium == null) {
            return null;
        }
        return new ClanBadgeUrls(small, large, medium);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof ClanBadgeUrls other)) {
            return false;
        }
        return Objects.equals(this.small, other.small)
                && Objects.equals(this.large, other.large)
                && Objects.equals(this.medium, other.medium);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.small, this.large, this.medium);
    }

}
