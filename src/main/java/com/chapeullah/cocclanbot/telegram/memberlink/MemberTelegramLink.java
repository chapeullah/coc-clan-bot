package com.chapeullah.cocclanbot.telegram.memberlink;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity @Table(name = "member_telegram_links")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberTelegramLink {

    @Id
    @Column(name = "member_tag", nullable = false, updatable = false)
    private String memberTag;

    @Column(name = "telegram_user_id", nullable = false, updatable = false)
    private Long telegramUserId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public MemberTelegramLink(
            @NonNull String memberTag,
            @NonNull Long telegramUserId) {
        this.memberTag = memberTag;
        this.telegramUserId = telegramUserId;
    }

}
