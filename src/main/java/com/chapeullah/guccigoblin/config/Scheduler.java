package com.chapeullah.guccigoblin.config;

import com.chapeullah.guccigoblin.clan.service.ClanService;
import com.chapeullah.guccigoblin.clan.model.Clan;
import com.chapeullah.guccigoblin.raidseason.model.RaidSeason;
import com.chapeullah.guccigoblin.raidseason.service.RaidSeasonService;
import com.chapeullah.guccigoblin.telegram.TelegramNotificationService;
import com.chapeullah.guccigoblin.war.model.War;
import com.chapeullah.guccigoblin.war.service.WarService;
import com.chapeullah.guccigoblin.war.service.WarLogService;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class Scheduler {

    private static final long SYNCHRONIZATION_TIMEOUT_MS = 55000;

    private final ScheduledExecutorService timeoutExecutor = Executors.newSingleThreadScheduledExecutor(
            Thread.ofPlatform().name("synchronization-timeout").daemon(true).factory());

    private long synchronizationStartedAt;

    private final TelegramNotificationService telegramNotificationService;

    private final WarLogService warLogService;
    private final WarService warService;
    private final ClanService clanService;
    private final RaidSeasonService raidSeasonService;

    @Value("${coc.clan-tag}")
    private String clanTag;

    @Scheduled(cron = "0 * * * * *")
    public void synchronize() {
        synchronizationStartedAt = System.nanoTime();
        FutureTask<Void> task = new FutureTask<>(this::synchronizeAll, null);
        ScheduledFuture<?> timeout = timeoutExecutor.schedule(
                () -> task.cancel(true), SYNCHRONIZATION_TIMEOUT_MS, TimeUnit.MILLISECONDS);

        try {
            task.run();
            task.get();
        } catch (CancellationException e) {
            Thread.interrupted();
            throw new IllegalStateException(
                    "Synchronization exceeded time limit: timeoutMs=" + SYNCHRONIZATION_TIMEOUT_MS, e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Synchronization failed", e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Synchronization interrupted", e);
        } finally {
            timeout.cancel(false);
        }
    }

    @PreDestroy
    public void shutdown() {
        timeoutExecutor.shutdownNow();
    }

    private void synchronizeAll() {
        checkSynchronization();
        synchronizeClan();
        checkSynchronization();
        finishEndedWars();
        checkSynchronization();
        synchronizeCurrentWar();
        checkSynchronization();
        synchronizeWarLog();
        checkSynchronization();
        synchronizeRaidSeason();
        checkSynchronization();
    }

    private void checkSynchronization() {
        if (System.nanoTime() - synchronizationStartedAt
                >= TimeUnit.MILLISECONDS.toNanos(SYNCHRONIZATION_TIMEOUT_MS)) {
            throw new CancellationException("Synchronization exceeded time limit");
        }
        if (Thread.currentThread().isInterrupted()) {
            throw new CancellationException("Synchronization interrupted");
        }
    }

    private void finishEndedWars() {
        try {
            log.debug("Finishing ended wars: clanTag=\"{}\"", clanTag);
            List<War> finishedWars = warService.finishEndedWars();
            if (finishedWars.isEmpty()) {
                log.debug("Ended wars finishing success: clanTag=\"{}\", finishedWarsCount={}", clanTag, finishedWars.size());
            } else {
                log.info("Ended wars finishing success: clanTag=\"{}\", finishedWarsCount={}", clanTag, finishedWars.size());
            }
        } catch (Exception e) {
            checkSynchronization();
            log.error("Ended wars finishing failed: clanTag=\"{}\"", clanTag, e);
            telegramNotificationService.sendError("Завершение войн", e);
        }
    }

    private void synchronizeClan() {
        try {
            log.debug("Synchronizing clan: clanTag=\"{}\"", clanTag);
            Clan clan = clanService.syncClan(clanTag);
            log.info("Clan synchronization success: clanTag=\"{}\", memberCount={}", clanTag, clan.getMemberCount());
        } catch (Exception e) {
            checkSynchronization();
            log.error("Clan synchronization failed: clanTag=\"{}\"", clanTag, e);
            telegramNotificationService.sendError("Синхронизация клана", e);
        }
    }

    private void synchronizeCurrentWar() {
        try {
            log.debug("Synchronizing current war: clanTag=\"{}\"", clanTag);
            War war = warService.syncCurrentWar(clanTag).orElse(null);
            if (war == null) {
                log.debug("Current war synchronization success: clanTag=\"{}\" - war not found", clanTag);
                return;
            }
            log.info("Current war synchronization success: clanTag=\"{}\", state=\"{}\"", clanTag, war.getState());
        } catch (Exception e) {
            checkSynchronization();
            log.error("Current war synchronization failed: clanTag=\"{}\"", clanTag, e);
            telegramNotificationService.sendError("Синхронизация текущей войны", e);
        }
    }

    private void synchronizeWarLog() {
        try {
            log.debug("Synchronizing war log: clanTag=\"{}\"", clanTag);
            warLogService.syncWarLog(clanTag);
            checkSynchronization();
            warService.syncEndedWarsFromWarLog(clanTag);
            log.info("War log synchronization success: clanTag=\"{}\"", clanTag);
        } catch (Exception e) {
            checkSynchronization();
            log.error("War log synchronization failed: clanTag=\"{}\"", clanTag, e);
            telegramNotificationService.sendError("Синхронизация истории войн", e);
        }
    }

    private void synchronizeRaidSeason() {
        try {
            log.debug("Synchronizing raid season: clanTag=\"{}\"", clanTag);
            RaidSeason raidSeason = raidSeasonService.syncRaidSeason(clanTag).orElse(null);
            if (raidSeason == null) {
                log.info("Raid season synchronization success: clanTag=\"{}\" - raid season not found", clanTag);
                return;
            }
            log.info("Raid season synchronization success: clanTag=\"{}\", state=\"{}\"", clanTag, raidSeason.getState());
        } catch (Exception e) {
            checkSynchronization();
            log.error("Raid season synchronization failed: clanTag=\"{}\"", clanTag, e);
            telegramNotificationService.sendError("Синхронизация сезона рейдов", e);
        }
    }

}
