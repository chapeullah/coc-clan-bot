package com.chapeullah.guccigoblin.label;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IconUrls {

    @Column(name = "icon_url_small")
    private String small;

    @Column(name = "icon_url_medium")
    private String medium;

    public IconUrls(String small, String medium) {
        this.small = small;
        this.medium = medium;
    }
    
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof IconUrls other)) {
            return false;
        }
        return Objects.equals(this.getSmall(), other.getSmall())
                && Objects.equals(this.getMedium(), other.getMedium());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getSmall(), this.getMedium());
    }

}
