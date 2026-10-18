package com.chapeullah.guccigoblin.capitalleague;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.Objects;

@Entity @Table(name = "capital_leagues")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CapitalLeague {

    @Id
    @Column(name = "id",
            nullable = false)
    private Integer id;

    @Column(name = "name",
            nullable = false)
    private String name;

    public CapitalLeague(@NonNull Integer id, @NonNull String name) {
        if (name.isBlank()) {
            throw new IllegalArgumentException("Clan capital league name must not be blank");
        }
        this.id = id;
        this.name = name;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof CapitalLeague other)) {
            return false;
        }
        return Objects.equals(this.getId(), other.getId()) &&
                Objects.equals(this.getName(), other.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getId(), this.getName());
    }

}
