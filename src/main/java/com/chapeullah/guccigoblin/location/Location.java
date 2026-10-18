package com.chapeullah.guccigoblin.location;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.Objects;

@Entity @Table(name = "locations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Location {

    @Id
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "is_country", nullable = false)
    private Boolean isCountry;

    @Column(name = "country_code", nullable = true)
    private String countryCode;

    public Location(
            @NonNull Integer id,
            @NonNull String name,
            @NonNull Boolean isCountry,
            String countryCode) {
        this.id = id;
        this.name = name;
        this.isCountry = isCountry;
        this.countryCode = countryCode;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof Location other)) {
            return false;
        }
        return Objects.equals(this.getId(), other.getId()) &&
                Objects.equals(this.getName(), other.getName()) &&
                Objects.equals(this.getIsCountry(), other.getIsCountry()) &&
                Objects.equals(this.getCountryCode(), other.getCountryCode());
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                this.getId(),
                this.getName(),
                this.getIsCountry(),
                this.getCountryCode());
    }

}
