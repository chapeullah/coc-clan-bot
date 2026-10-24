package com.chapeullah.cocclanbot.telegram.groupchatthreads;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.Objects;

@Entity @Table(name = "telegram_chat_threads")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TelegramGroupChatThreads {

    public static final Long SETTING_ID = 1L;

    public static final int GENERAL_THREAD_ID = 0;

    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "war_thread_id", nullable = true)
    private Integer warThreadId;

    @Column(name = "war_thread_id_changed_at", nullable = true)
    private Instant warThreadIdChangedAt;

    @Column(name = "raid_thread_id", nullable = true)
    private Integer raidThreadId;

    @Column(name = "raid_thread_id_changed_at", nullable = true)
    private Instant raidThreadIdChangedAt;

    @Column(name = "main_thread_id", nullable = true)
    private Integer mainThreadId;

    @Column(name = "main_thread_id_changed_at", nullable = true)
    private Instant mainThreadIdChangedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public TelegramGroupChatThreads(Integer warThreadId, Integer raidThreadId) {
        this(warThreadId, raidThreadId, null);
    }

    public TelegramGroupChatThreads(Integer warThreadId, Integer raidThreadId, Integer mainThreadId) {
        this.id = SETTING_ID;
        this.warThreadId = warThreadId;
        this.raidThreadId = raidThreadId;
        this.mainThreadId = mainThreadId;
    }

    public void update(Integer warThreadId, Integer raidThreadId) {
        Instant now = Instant.now();
        if (!Objects.equals(this.warThreadId, warThreadId)) {
            this.warThreadId = warThreadId;
            this.warThreadIdChangedAt = now;
        }

        if (!Objects.equals(this.raidThreadId, raidThreadId)) {
            this.raidThreadId = raidThreadId;
            this.raidThreadIdChangedAt = now;
        }
    }

    public void setWarThreadId(Integer warThreadId) {
        if (!Objects.equals(this.warThreadId, warThreadId)) {
            this.warThreadId = warThreadId;
            this.warThreadIdChangedAt = Instant.now();
        }
    }

    public void setRaidThreadId(Integer raidThreadId) {
        if (!Objects.equals(this.raidThreadId, raidThreadId)) {
            this.raidThreadId = raidThreadId;
            this.raidThreadIdChangedAt = Instant.now();
        }
    }

    public void setMainThreadId(Integer mainThreadId) {
        if (!Objects.equals(this.mainThreadId, mainThreadId)) {
            this.mainThreadId = mainThreadId;
            this.mainThreadIdChangedAt = Instant.now();
        }
    }

}
