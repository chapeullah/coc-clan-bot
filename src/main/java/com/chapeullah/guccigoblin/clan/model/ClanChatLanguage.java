package com.chapeullah.guccigoblin.clan.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity @Table(name = "clan_chat_languages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClanChatLanguage {

    @Id
    @Column(name = "id",
            nullable = false)
    private Integer id;

    @SuppressWarnings("FieldMayBeFinal")
    @OneToMany(mappedBy = "clanChatLanguage")
    private List<Clan> clans = new ArrayList<>();

    @Column(name = "name",
            nullable = false)
    private String name;

    @Column(name = "language_code",
            nullable = true)
    private String languageCode;

    public ClanChatLanguage(
            @NonNull Integer id,
            @NonNull String name,
            String languageCode) {
        this.id = id;
        this.name = name;
        this.languageCode = languageCode;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof ClanChatLanguage other)) {
            return false;
        }
        return Objects.equals(this.getId(), other.getId()) &&
                Objects.equals(this.getName(), other.getName()) &&
                Objects.equals(this.getLanguageCode(), other.getLanguageCode());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getId(), this.getName(), this.getLanguageCode());
    }

}
