package com.chapeullah.cocclanbot.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.chapeullah.cocclanbot.clan.service.ClanService;
import com.chapeullah.cocclanbot.diagnostics.SynchronizationDiagnostics;
import com.chapeullah.cocclanbot.raidseason.service.RaidSeasonService;
import com.chapeullah.cocclanbot.telegram.notification.TelegramWarNotificationService;
import com.chapeullah.cocclanbot.war.service.WarLogService;
import com.chapeullah.cocclanbot.war.service.WarService;
import com.chapeullah.cocclanbot.war.model.War;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import java.net.http.HttpTimeoutException;
import java.util.Optional;
import java.util.concurrent.FutureTask;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SchedulerDiagnosticsTest {
    private final ClanService clans = mock(ClanService.class);
    private final RaidSeasonService raids = mock(RaidSeasonService.class);
    private final TelegramWarNotificationService notifications = mock(TelegramWarNotificationService.class);
    private final WarService wars = mock(WarService.class);
    private final Logger logger = (Logger) LoggerFactory.getLogger(Scheduler.class);
    private final Scheduler scheduler = new Scheduler(notifications, mock(WarLogService.class), wars, clans, raids);
    private ListAppender<ILoggingEvent> events;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(scheduler, "clanTag", "#TEST");
        ReflectionTestUtils.setField(scheduler, "cocApiResponseTimeoutMs", 1000L);
        when(raids.syncRaidSeason("#TEST")).thenReturn(Optional.empty());
        events = new ListAppender<>();
        events.start();
        logger.addAppender(events);
    }

    @AfterEach
    void tearDown() {
        scheduler.shutdown();
        logger.detachAppender(events);
        Thread.interrupted();
        MDC.clear();
    }

    @Test
    void deadlineIsHandledOnceAndPreservesTheUnderlyingFailure() {
        ReflectionTestUtils.setField(scheduler, "cocApiResponseTimeoutMs", 100L);
        when(clans.syncClan("#TEST")).thenAnswer(invocation -> {
            try { Thread.sleep(10000); } catch (InterruptedException ignored) {
                throw new ResourceAccessException("request interrupted", new HttpTimeoutException(null));
            }
            return null;
        });
        assertDoesNotThrow(scheduler::synchronizeClanAndRaids);
        var errors = events.list.stream().filter(e -> e.getLevel() == Level.ERROR).toList();
        assertEquals(1, errors.size());
        assertTrue(errors.getFirst().getFormattedMessage().contains("cancellationReason=SYNC_DEADLINE"));
        assertTrue(errors.getFirst().getFormattedMessage().contains("stage=\"syncClan\""));
        assertTrue(errors.getFirst().getFormattedMessage().contains("ResourceAccessException -> HttpTimeoutException"));
        assertNotNull(errors.getFirst().getThrowableProxy());
        verify(notifications).sendError(eq("Clan and Raid synchronization"), any(IllegalStateException.class));
        verify(raids, never()).syncRaidSeason(anyString());
        assertFalse(Thread.currentThread().isInterrupted());
        assertNull(SynchronizationDiagnostics.current());
        assertNull(MDC.get("syncId"));
    }

    @Test
    void ordinaryStepFailureIsLoggedOnceAndNextStepStillRuns() {
        when(clans.syncClan("#TEST")).thenThrow(new ResourceAccessException("request failed", new HttpTimeoutException(null)));
        scheduler.synchronizeClanAndRaids();
        assertEquals(1, events.list.stream().filter(e -> e.getLevel() == Level.ERROR).count());
        verify(raids).syncRaidSeason("#TEST");
        verify(notifications).sendError(eq("Синхронизация клана"), any(ResourceAccessException.class));
    }

    @Test
    void timeoutDuringErrorNotificationKeepsItsStageAndDoesNotRepeatTheOriginalStack() {
        ReflectionTestUtils.setField(scheduler, "cocApiResponseTimeoutMs", 150L);
        when(clans.syncClan("#TEST")).thenThrow(new ResourceAccessException("request failed", new HttpTimeoutException(null)));
        doAnswer(invocation -> {
            try { Thread.sleep(10000); } catch (InterruptedException ignored) { }
            return null;
        }).when(notifications).sendError(eq("Синхронизация клана"), any());
        assertDoesNotThrow(scheduler::synchronizeClanAndRaids);
        var errors = events.list.stream().filter(e -> e.getLevel() == Level.ERROR).toList();
        assertEquals(2, errors.size());
        assertNotNull(errors.getFirst().getThrowableProxy());
        assertNull(errors.getLast().getThrowableProxy());
        assertTrue(errors.getLast().getFormattedMessage().contains("stage=\"sendErrorNotification\""));
        assertTrue(errors.getLast().getFormattedMessage().contains("ResourceAccessException -> HttpTimeoutException"));
    }

    @Test
    void completedRequestCannotBeMarkedAsCancelledByTheDeadlineRace() {
        try (SynchronizationDiagnostics diagnostics = new SynchronizationDiagnostics("sync", 1000)) {
            FutureTask<Void> task = new FutureTask<>(() -> null) {
                @Override
                public boolean cancel(boolean mayInterruptIfRunning) {
                    run();
                    return super.cancel(mayInterruptIfRunning);
                }
            };
            assertFalse(diagnostics.cancelAtDeadline(task));
            assertTrue(task.isDone());
            assertFalse(task.isCancelled());
            assertFalse(diagnostics.isDeadlineExceeded());
            assertFalse(Thread.currentThread().isInterrupted());
        }
    }

    @Test
    void handledFailureFromPreviousStepDoesNotExplainLaterDeadline() {
        ReflectionTestUtils.setField(scheduler, "cocApiResponseTimeoutMs", 200L);
        var previousFailure = new ResourceAccessException("earlier clan failure", new HttpTimeoutException(null));
        when(clans.syncClan("#TEST")).thenThrow(previousFailure);
        when(raids.syncRaidSeason("#TEST")).thenAnswer(invocation -> {
            assertNull(SynchronizationDiagnostics.current().lastFailure());
            try { Thread.sleep(10000); } catch (InterruptedException ignored) { }
            return Optional.empty();
        });
        scheduler.synchronizeClanAndRaids();
        var errors = events.list.stream().filter(e -> e.getLevel() == Level.ERROR).toList();
        assertEquals(2, errors.size());
        var timeout = errors.getLast();
        assertTrue(timeout.getFormattedMessage().contains("stage=\"syncRaidSeason\""));
        assertFalse(timeout.getFormattedMessage().contains("HttpTimeoutException"));
        assertNotNull(timeout.getThrowableProxy());
        assertNotEquals(ResourceAccessException.class.getName(), timeout.getThrowableProxy().getCause().getClassName());
        verify(notifications).sendError(eq("Синхронизация клана"), same(previousFailure));
        verify(notifications).sendError(eq("Clan and Raid synchronization"), any(IllegalStateException.class));
    }

    @Test
    void externalInterruptionIsPreservedAndDoesNotBecomeADeadline() {
        MDC.put("syncId", "outer-sync");
        Thread.currentThread().interrupt();
        scheduler.synchronizeClanAndRaids();
        assertTrue(Thread.currentThread().isInterrupted());
        assertNull(SynchronizationDiagnostics.current());
        assertEquals("outer-sync", MDC.get("syncId"));
        var errors = events.list.stream().filter(e -> e.getLevel() == Level.ERROR).toList();
        assertEquals(1, errors.size());
        assertTrue(errors.getFirst().getFormattedMessage().contains("cancellationReason=THREAD_INTERRUPTED"));
        assertFalse(errors.getFirst().getFormattedMessage().contains("SYNC_DEADLINE"));
        verifyNoInteractions(clans, raids, notifications);
    }

    @Test
    void nestedStepCancellationRetainsTheActualNotificationFailure() {
        ReflectionTestUtils.setField(scheduler, "cocApiResponseTimeoutMs", 200L);
        War war = mock(War.class);
        when(wars.syncCurrentWar("#TEST")).thenReturn(Optional.of(war));
        var underlying = new ResourceAccessException("notification interrupted", new HttpTimeoutException(null));
        doAnswer(invocation -> {
            try { Thread.sleep(10000); } catch (InterruptedException ignored) { throw underlying; }
            return null;
        }).when(notifications).sendWarNotification(war);
        scheduler.synchronizeWars();
        var errors = events.list.stream().filter(e -> e.getLevel() == Level.ERROR).toList();
        assertEquals(1, errors.size());
        assertTrue(errors.getFirst().getFormattedMessage().contains("ResourceAccessException -> HttpTimeoutException"));
        assertEquals(underlying.getClass().getName(), errors.getFirst().getThrowableProxy().getCause().getClassName());
        verify(notifications).sendError(eq("Wars synchronization"), argThat(error -> error.getCause() == underlying));
    }

    @Test
    void aDifferentFailureWhileReportingAnErrorKeepsItsOwnStackAndNotification() {
        var original = new ResourceAccessException("first error", new HttpTimeoutException(null));
        var reportingFailure = new IllegalStateException("notification failed independently");
        when(clans.syncClan("#TEST")).thenThrow(original);
        doThrow(reportingFailure).when(notifications).sendError(eq("Синхронизация клана"), same(original));
        scheduler.synchronizeClanAndRaids();
        var errors = events.list.stream().filter(e -> e.getLevel() == Level.ERROR).toList();
        assertEquals(2, errors.size());
        assertNotNull(errors.getFirst().getThrowableProxy());
        assertEquals(reportingFailure.getMessage(), errors.getLast().getThrowableProxy().getCause().getMessage());
        verify(notifications).sendError(eq("Clan and Raid synchronization"), argThat(error -> error.getCause() == reportingFailure));
    }

    @Test
    void deadlineWhileReportingAnErrorDoesNotHideANewFailureBehindTheLoggedOne() {
        ReflectionTestUtils.setField(scheduler, "cocApiResponseTimeoutMs", 200L);
        var original = new ResourceAccessException("first error", new HttpTimeoutException(null));
        var reportingFailure = new IllegalStateException("error notification interrupted");
        when(clans.syncClan("#TEST")).thenThrow(original);
        doAnswer(invocation -> {
            try { Thread.sleep(10000); } catch (InterruptedException ignored) { throw reportingFailure; }
            return null;
        }).when(notifications).sendError(eq("Синхронизация клана"), same(original));
        scheduler.synchronizeClanAndRaids();
        var errors = events.list.stream().filter(e -> e.getLevel() == Level.ERROR).toList();
        assertEquals(2, errors.size());
        assertNotNull(errors.getLast().getThrowableProxy());
        assertEquals(reportingFailure.getMessage(), errors.getLast().getThrowableProxy().getCause().getMessage());
        verify(notifications).sendError(eq("Clan and Raid synchronization"), argThat(error -> error.getCause() == reportingFailure));
    }
}
