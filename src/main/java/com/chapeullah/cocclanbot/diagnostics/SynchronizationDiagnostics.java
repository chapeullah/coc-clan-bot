package com.chapeullah.cocclanbot.diagnostics;

import org.slf4j.MDC;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public final class SynchronizationDiagnostics implements AutoCloseable {
    private static final ThreadLocal<SynchronizationDiagnostics> CURRENT = new ThreadLocal<>();
    private final SynchronizationDiagnostics previous;
    private final String previousSyncId;
    private final String syncId = UUID.randomUUID().toString();
    private final long startedAt = System.nanoTime();
    private final long timeoutMs;
    private volatile String stage;
    private volatile HttpRequestDiagnostics lastRequest;
    private volatile Throwable lastFailure;
    private volatile Throwable loggedFailure;
    private volatile boolean deadlineExceeded;
    private volatile String deadlineSnapshot;

    public SynchronizationDiagnostics(String operation, long timeoutMs) {
        this.timeoutMs = timeoutMs;
        stage = operation;
        previous = CURRENT.get();
        previousSyncId = MDC.get("syncId");
        CURRENT.set(this);
        MDC.put("syncId", syncId);
    }

    public static SynchronizationDiagnostics current() {
        return CURRENT.get();
    }

    public static void stage(String stage) {
        if (current() != null) {
            current().stage = stage;
        }
    }

    public static void beginStage(String stage) {
        SynchronizationDiagnostics diagnostics = current();
        if (diagnostics != null) {
            diagnostics.lastFailure = null;
            diagnostics.loggedFailure = null;
            diagnostics.stage = stage;
        }
    }

    public static void recordFailure(Throwable failure) {
        if (current() != null) {
            current().lastFailure = failure;
        }
    }

    public long startedAt() {
        return startedAt;
    }

    public String stage() {
        return stage;
    }

    public long remainingMs() {
        return Math.max(0, timeoutMs - TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt));
    }

    public void request(HttpRequestDiagnostics request) {
        lastRequest = request;
    }

    public void failureLogged() {
        loggedFailure = lastFailure;
    }

    public boolean hasLoggedFailure() {
        return lastFailure != null && lastFailure == loggedFailure;
    }

    public Throwable lastFailure() {
        return lastFailure;
    }

    public void deadlineExceeded() {
        String snapshot = summary();
        synchronized (this) {
            if (!deadlineExceeded) {
                deadlineSnapshot = snapshot;
                deadlineExceeded = true;
            }
        }
    }

    public boolean cancelAtDeadline(Future<?> task) {
        if (task.isDone()) {
            return false;
        }
        String snapshot = summary();
        synchronized (this) {
            if (!task.cancel(true)) {
                return false;
            }
            if (!deadlineExceeded) {
                deadlineSnapshot = snapshot;
                deadlineExceeded = true;
            }
            return true;
        }
    }

    public synchronized boolean isDeadlineExceeded() {
        return deadlineExceeded;
    }

    public String summary() {
        HttpRequestDiagnostics request = lastRequest;
        return "syncId=" + syncId + ", stage=\"" + stage + "\", elapsedMs="
                + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt)
                + ", syncTimeoutMs=" + timeoutMs + ", syncRemainingMs=" + remainingMs()
                + ", lastRequest={" + (request == null ? "none" : request.summary()) + "}"
                + ", lastCauseChain=\"" + HttpRequestDiagnostics.causeChain(lastFailure) + "\"";
    }

    public String deadlineSummary() {
        return deadlineSnapshot == null ? summary() : deadlineSnapshot;
    }

    @Override
    public void close() {
        if (previous == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(previous);
        }
        if (previousSyncId == null) {
            MDC.remove("syncId");
        } else {
            MDC.put("syncId", previousSyncId);
        }
    }
}
