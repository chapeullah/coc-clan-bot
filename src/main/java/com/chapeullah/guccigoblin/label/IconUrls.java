package com.chapeullah.guccigoblin.label;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

}
