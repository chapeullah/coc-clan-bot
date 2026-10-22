package com.chapeullah.cocclanbot.config;

import com.chapeullah.cocclanbot.clan.service.ClanService;
import com.chapeullah.cocclanbot.clan.model.Clan;
import com.chapeullah.cocclanbot.raidseason.model.RaidSeason;
import com.chapeullah.cocclanbot.raidseason.service.RaidSeasonService;
import com.chapeullah.cocclanbot.telegram.notification.TelegramWarNotificationService;
import com.chapeullah.cocclanbot.war.model.War;
import com.chapeullah.cocclanbot.war.service.WarService;
import com.chapeullah.cocclanbot.war.service.WarLogService;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.LongConsumer;

@Slf4j
@Component
@RequiredArgsConstructor
@DependsOn("environmentVariablesService")
public class Scheduler {

    @Value("${COC_API_RESPONSE_TIMEOUT_MS}")
    private long cocApiResponseTimeoutMs;

    private final ScheduledExecutorService timeoutExecutor = Executors.newSingleThreadScheduledExecutor(
            Thread.ofPlatform().name("synchronization-timeout").daemon(true).factory());

    private final TelegramWarNotificationService telegramWarNotificationService;

    private final WarLogService warLogService;
    private final WarService warService;
    private final ClanService clanService;
    private final RaidSeasonService raidSeasonService;

    @Value("${coc.clan-tag}")
    private String clanTag;

    @Scheduled(cron = "0 * * * * *")
    public void synchronizeClanAndRaids() {
        runWithTimeout("Синхронизация клана и рейдов", startedAt -> {
            synchronizeClan(startedAt);
            checkSynchronization(startedAt);
            synchronizeRaidSeason(startedAt);
        });
    }

    @Scheduled(cron = "0 * * * * *")
    public void synchronizeWars() {
        runWithTimeout("Синхронизация войн", startedAt -> {
            finishEndedWars(startedAt);
            checkSynchronization(startedAt);
            synchronizeCurrentWar(startedAt);
            checkSynchronization(startedAt);
            synchronizeWarLog(startedAt);
        });
    }

    private void runWithTimeout(String operation, LongConsumer synchronization) {
        long startedAt = System.nanoTime();
        FutureTask<Void> task = new FutureTask<>(() -> {
            checkSynchronization(startedAt);
            synchronization.accept(startedAt);
            checkSynchronization(startedAt);
        }, null);
        ScheduledFuture<?> timeout = timeoutExecutor.schedule(() ->
                task.cancel(true), cocApiResponseTimeoutMs, TimeUnit.MILLISECONDS);

        try {
            task.run();
            task.get();
        } catch (CancellationException e) {
            throw reportSynchronizationTimeout(operation, e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof CancellationException
                    && System.nanoTime() - startedAt >= TimeUnit.MILLISECONDS.toNanos(cocApiResponseTimeoutMs)) {
                throw reportSynchronizationTimeout(operation, e.getCause());
            }
            throw new IllegalStateException("Synchronization failed", e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Synchronization interrupted", e);
        } finally {
            timeout.cancel(false);
        }
    }

    private IllegalStateException reportSynchronizationTimeout(String operation, Throwable cause) {
        Thread.interrupted();
        IllegalStateException exception = new IllegalStateException(
                "Synchronization exceeded time limit: timeoutMs=" + cocApiResponseTimeoutMs, cause);
        log.error("Synchronization exceeded time limit: operation=\"{}\", timeoutMs={}",
                operation, cocApiResponseTimeoutMs, exception);
        telegramWarNotificationService.sendError(operation, exception);
        return exception;
    }

    @PreDestroy
    public void shutdown() {
        timeoutExecutor.shutdownNow();
    }

    private void checkSynchronization(long startedAt) {
        if (System.nanoTime() - startedAt
                >= TimeUnit.MILLISECONDS.toNanos(cocApiResponseTimeoutMs)) {
            throw new CancellationException("Synchronization exceeded time limit");
        }
        if (Thread.currentThread().isInterrupted()) {
            throw new CancellationException("Synchronization interrupted");
        }
    }

    private void finishEndedWars(long startedAt) {
        try {
            log.debug("Finalizing ended wars: clanTag=\"{}\"", clanTag);
            List<War> finishedWars = warService.finishEndedWars();
            if (finishedWars.isEmpty()) {
                log.info("No ended wars to finalize: clanTag=\"{}\"", clanTag);
                return;
            }
            log.info("Ended wars finalization succeeded: clanTag=\"{}\", finishedWarsCount={}", clanTag, finishedWars.size());
        } catch (Exception e) {
            checkSynchronization(startedAt);
            log.error("Ended wars finalization failed: clanTag=\"{}\"", clanTag, e);
            telegramWarNotificationService.sendError("Завершение войн", e);
        }
    }

    private void synchronizeClan(long startedAt) {
        try {
            log.debug("Synchronizing clan: clanTag=\"{}\"", clanTag);
            Clan clan = clanService.syncClan(clanTag);
            log.info("Clan synchronization succeeded: clanTag=\"{}\", memberCount={}", clanTag, clan.getMemberCount());
        } catch (Exception e) {
            checkSynchronization(startedAt);
            log.error("Clan synchronization failed: clanTag=\"{}\"", clanTag, e);
            telegramWarNotificationService.sendError("Синхронизация клана", e);
        }
    }

    private void synchronizeCurrentWar(long startedAt) {
        try {
            log.debug("Synchronizing current war: clanTag=\"{}\"", clanTag);
            War war = warService.syncCurrentWar(clanTag).orElse(null);
            if (war == null) {
                log.debug("No current war found: clanTag=\"{}\"", clanTag);
                return;
            }
            sendWarNotification(war, startedAt);
            log.info("Current war synchronization succeeded: clanTag=\"{}\", state=\"{}\"", clanTag, war.getState());
        } catch (Exception e) {
            checkSynchronization(startedAt);
            log.error("Current war synchronization failed: clanTag=\"{}\"", clanTag, e);
            telegramWarNotificationService.sendError("Синхронизация текущей войны", e);
        }
    }

    private void sendWarNotification(War war, long startedAt) {
        checkSynchronization(startedAt);
        try {
            telegramWarNotificationService.sendWarNotification(war);
        } catch (Exception e) {
            checkSynchronization(startedAt);
            log.error("War notification sending failed: warId={}", war.getId(), e);
            telegramWarNotificationService.sendError("Отправка уведомления о КВ", e);
        }
    }

    private void synchronizeWarLog(long startedAt) {
        try {
            log.debug("Synchronizing war log: clanTag=\"{}\"", clanTag);
            warLogService.syncWarLog(clanTag);
            checkSynchronization(startedAt);
            warService.syncEndedWarsFromWarLog(clanTag);
            log.info("War log synchronization succeeded: clanTag=\"{}\"", clanTag);
        } catch (Exception e) {
            checkSynchronization(startedAt);
            log.error("War log synchronization failed: clanTag=\"{}\"", clanTag, e);
            telegramWarNotificationService.sendError("Синхронизация истории войн", e);
        }
    }

    private void synchronizeRaidSeason(long startedAt) {
        try {
            log.debug("Synchronizing raid season: clanTag=\"{}\"", clanTag);
            RaidSeason raidSeason = raidSeasonService.syncRaidSeason(clanTag).orElse(null);
            if (raidSeason == null) {
                log.debug("No raid season found: clanTag=\"{}\"", clanTag);
                return;
            }
            log.info("Raid season synchronization succeeded: clanTag=\"{}\", state=\"{}\"", clanTag, raidSeason.getState());
        } catch (Exception e) {
            checkSynchronization(startedAt);
            log.error("Raid season synchronization failed: clanTag=\"{}\"", clanTag, e);
            telegramWarNotificationService.sendError("Синхронизация сезона рейдов", e);
        }
    }

}
