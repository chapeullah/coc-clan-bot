package com.chapeullah.cocclanbot.label.player;

import com.chapeullah.cocclanbot.label.IconUrls;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.Objects;

@Entity @Table(name = "player_labels")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlayerLabel {

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof PlayerLabel other)) {
            return false;
        }
        return Objects.equals(this.getId(), other.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getId());
    }

    @Id
    @Column(name = "id",
            nullable = false)
    private Integer id;

    @Column(name = "name",
            nullable = false)
    private String name;

    @Embedded
    private IconUrls iconUrls;

    public PlayerLabel(
            @NonNull Integer id,
            String name,
            IconUrls iconUrls) {
        this.id = id;
        this.name = name;
        this.iconUrls = iconUrls;
    }

}
