package com.chapeullah.cocclanbot.config;

import com.chapeullah.cocclanbot.clan.service.ClanService;
import com.chapeullah.cocclanbot.diagnostics.SynchronizationDiagnostics;
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

    public void sendTestExceptionNotification() {
        try {
            throw new IllegalStateException("Тестовое исключение после запуска приложения");
        } catch (Exception e) {
            telegramWarNotificationService.sendError("Тест уведомлений об исключениях", e);
        }
    }

    @Scheduled(cron = "0 */5 * * * *")
    public void synchronizeClanAndRaids() {
        runWithTimeout("Clan and Raid synchronization", startedAt -> {
            synchronizeClan(startedAt);
            checkSynchronization(startedAt);
            synchronizeRaidSeason(startedAt);
        });
    }

    @Scheduled(cron = "0 */5 * * * *")
    public void synchronizeWars() {
        runWithTimeout("Wars synchronization", startedAt -> {
            finishEndedWars(startedAt);
            checkSynchronization(startedAt);
            synchronizeCurrentWar(startedAt);
            checkSynchronization(startedAt);
            synchronizeWarLog(startedAt);
        });
    }

    private void runWithTimeout(String operation, LongConsumer synchronization) {
        try (SynchronizationDiagnostics diagnostics = new SynchronizationDiagnostics(operation, cocApiResponseTimeoutMs)) {
            long startedAt = diagnostics.startedAt();
            FutureTask<Void> task = new FutureTask<>(() -> {
                checkSynchronization(startedAt);
                synchronization.accept(startedAt);
                checkSynchronization(startedAt);
            }, null);
            ScheduledFuture<?> timeout = timeoutExecutor.schedule(() -> diagnostics.cancelAtDeadline(task),
                    cocApiResponseTimeoutMs, TimeUnit.MILLISECONDS);
            try {
                task.run();
                task.get();
                log.debug("Synchronization finished: {}", diagnostics.summary());
            } catch (CancellationException e) {
                reportSynchronizationTimeout(operation, e);
            } catch (ExecutionException e) {
                if (diagnostics.isDeadlineExceeded()) {
                    reportSynchronizationTimeout(operation, e.getCause());
                } else if (Thread.currentThread().isInterrupted()) {
                    reportSynchronizationInterrupted(operation, e.getCause());
                } else {
                    IllegalStateException exception = new IllegalStateException("Synchronization failed", e.getCause());
                    SynchronizationDiagnostics.recordFailure(e.getCause());
                    log.error("Synchronization failed: operation=\"{}\", {}", operation, diagnostics.summary(), exception);
                    notifyError(operation, exception);
                }
            } catch (InterruptedException e) {
                reportSynchronizationInterrupted(operation, e);
            } finally {
                timeout.cancel(false);
            }
        }
    }

    private void reportSynchronizationTimeout(String operation, Throwable cause) {
        Thread.interrupted();
        SynchronizationDiagnostics diagnostics = SynchronizationDiagnostics.current();
        diagnostics.deadlineExceeded();
        Throwable original = diagnostics.lastFailure() == null ? cause : diagnostics.lastFailure();
        IllegalStateException exception = new IllegalStateException(
                "Synchronization exceeded time limit: timeoutMs=" + cocApiResponseTimeoutMs, original);
        String details = diagnostics.summary() + ", atDeadline={" + diagnostics.deadlineSummary() + "}";
        if (diagnostics.hasLoggedFailure()) {
            log.error("Synchronization exceeded time limit: operation=\"{}\", cancellationReason=SYNC_DEADLINE, {}",
                    operation, details);
        } else {
            log.error("Synchronization exceeded time limit: operation=\"{}\", cancellationReason=SYNC_DEADLINE, {}",
                    operation, details, exception);
        }
        notifyError(operation, exception);
    }

    private void reportStepFailure(String message, String operation, Exception exception, long startedAt) {
        if (!(exception instanceof SynchronizationCancelledException)) {
            SynchronizationDiagnostics.recordFailure(exception);
        }
        checkSynchronization(startedAt);
        SynchronizationDiagnostics diagnostics = SynchronizationDiagnostics.current();
        log.error("{}: clanTag=\"{}\", {}", message, clanTag, diagnostics.summary(), exception);
        diagnostics.failureLogged();
        notifyError(operation, exception);
    }

    private void reportSynchronizationInterrupted(String operation, Throwable cause) {
        SynchronizationDiagnostics diagnostics = SynchronizationDiagnostics.current();
        Throwable original = diagnostics.lastFailure() == null ? cause : diagnostics.lastFailure();
        Thread.currentThread().interrupt();
        if (diagnostics.hasLoggedFailure()) {
            log.error("Synchronization interrupted: operation=\"{}\", cancellationReason=THREAD_INTERRUPTED, {}",
                    operation, diagnostics.summary());
        } else {
            log.error("Synchronization interrupted: operation=\"{}\", cancellationReason=THREAD_INTERRUPTED, {}",
                    operation, diagnostics.summary(), original);
        }
    }

    private void notifyError(String operation, Exception exception) {
        SynchronizationDiagnostics diagnostics = SynchronizationDiagnostics.current();
        String previousStage = diagnostics.stage();
        SynchronizationDiagnostics.stage("sendErrorNotification");
        try {
            telegramWarNotificationService.sendError(operation, exception);
        } catch (RuntimeException notificationFailure) {
            SynchronizationDiagnostics.recordFailure(notificationFailure);
            throw notificationFailure;
        } finally {
            SynchronizationDiagnostics.stage(previousStage);
        }
    }

    @PreDestroy
    public void shutdown() {
        timeoutExecutor.shutdownNow();
    }

    private void checkSynchronization(long startedAt) {
        if (System.nanoTime() - startedAt
                >= TimeUnit.MILLISECONDS.toNanos(cocApiResponseTimeoutMs)) {
            SynchronizationDiagnostics.current().deadlineExceeded();
            throw new SynchronizationCancelledException("Synchronization exceeded time limit");
        }
        if (Thread.currentThread().isInterrupted()) {
            throw new SynchronizationCancelledException("Synchronization interrupted");
        }
    }

    private static final class SynchronizationCancelledException extends CancellationException {
        private SynchronizationCancelledException(String message) {
            super(message);
        }
    }

    private void finishEndedWars(long startedAt) {
        SynchronizationDiagnostics.beginStage("finishEndedWars");
        try {
            log.debug("Finalizing ended wars: clanTag=\"{}\"", clanTag);
            List<War> finishedWars = warService.finishEndedWars();
            if (finishedWars.isEmpty()) {
                log.info("No ended wars to finalize: clanTag=\"{}\"", clanTag);
                return;
            }
            log.info("Ended wars finalization succeeded: clanTag=\"{}\", finishedWarsCount={}", clanTag, finishedWars.size());
        } catch (Exception e) {
            reportStepFailure("Ended wars finalization failed", "Завершение войн", e, startedAt);
        }
    }

    private void synchronizeClan(long startedAt) {
        SynchronizationDiagnostics.beginStage("syncClan");
        try {
            log.debug("Synchronizing clan: clanTag=\"{}\"", clanTag);
            Clan clan = clanService.syncClan(clanTag);
            log.info("Clan synchronization succeeded: clanTag=\"{}\", memberCount={}", clanTag, clan.getMemberCount());
        } catch (Exception e) {
            reportStepFailure("Clan synchronization failed", "Синхронизация клана", e, startedAt);
        }
    }

    private void synchronizeCurrentWar(long startedAt) {
        SynchronizationDiagnostics.beginStage("syncCurrentWar");
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
            reportStepFailure("Current war synchronization failed", "Синхронизация текущей войны", e, startedAt);
        }
    }

    private void sendWarNotification(War war, long startedAt) {
        SynchronizationDiagnostics.beginStage("sendWarNotification");
        checkSynchronization(startedAt);
        try {
            telegramWarNotificationService.sendWarNotification(war);
        } catch (Exception e) {
            reportStepFailure("War notification sending failed: warId=" + war.getId(), "Отправка уведомления о КВ", e, startedAt);
        }
    }

    private void synchronizeWarLog(long startedAt) {
        SynchronizationDiagnostics.beginStage("syncWarLog");
        try {
            log.debug("Synchronizing war log: clanTag=\"{}\"", clanTag);
            warLogService.syncWarLog(clanTag);
            checkSynchronization(startedAt);
            SynchronizationDiagnostics.beginStage("syncEndedWarsFromWarLog");
            warService.syncEndedWarsFromWarLog(clanTag);
            log.info("War log synchronization succeeded: clanTag=\"{}\"", clanTag);
        } catch (Exception e) {
            reportStepFailure("War log synchronization failed", "Синхронизация истории войн", e, startedAt);
        }
    }

    private void synchronizeRaidSeason(long startedAt) {
        SynchronizationDiagnostics.beginStage("syncRaidSeason");
        try {
            log.debug("Synchronizing raid season: clanTag=\"{}\"", clanTag);
            RaidSeason raidSeason = raidSeasonService.syncRaidSeason(clanTag).orElse(null);
            if (raidSeason == null) {
                log.debug("No raid season found: clanTag=\"{}\"", clanTag);
                return;
            }
            log.info("Raid season synchronization succeeded: clanTag=\"{}\", state=\"{}\"", clanTag, raidSeason.getState());
        } catch (Exception e) {
            reportStepFailure("Raid season synchronization failed", "Синхронизация сезона рейдов", e, startedAt);
        }
    }

}
