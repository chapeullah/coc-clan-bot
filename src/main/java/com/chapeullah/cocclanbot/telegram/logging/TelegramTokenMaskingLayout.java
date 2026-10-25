package com.chapeullah.cocclanbot.telegram.logging;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.LoggerContextVO;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import com.chapeullah.cocclanbot.diagnostics.DiagnosticSanitizer;
import org.slf4j.Marker;
import org.slf4j.event.KeyValuePair;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class TelegramTokenMaskingLayout extends PatternLayout {

    @Override
    public String doLayout(ILoggingEvent event) {
        return DiagnosticSanitizer.mask(super.doLayout(new SanitizedEvent(event)));
    }

    private static String sanitize(String message) {
        return message == null ? null : DiagnosticSanitizer.escapeControls(DiagnosticSanitizer.mask(message));
    }

    private static IThrowableProxy sanitize(IThrowableProxy throwable) {
        return throwable == null ? null : new SanitizedThrowable(throwable);
    }

    private record SanitizedEvent(ILoggingEvent event) implements ILoggingEvent {
        @Override public String getThreadName() { return event.getThreadName(); }
        @Override public Level getLevel() { return event.getLevel(); }
        @Override public String getMessage() { return getFormattedMessage(); }
        @Override public Object[] getArgumentArray() { return null; }
        @Override public String getFormattedMessage() { return sanitize(event.getFormattedMessage()); }
        @Override public String getLoggerName() { return event.getLoggerName(); }
        @Override public LoggerContextVO getLoggerContextVO() { return event.getLoggerContextVO(); }
        @Override public IThrowableProxy getThrowableProxy() { return sanitize(event.getThrowableProxy()); }
        @Override public StackTraceElement[] getCallerData() { return event.getCallerData(); }
        @Override public boolean hasCallerData() { return event.hasCallerData(); }
        @Override public List<Marker> getMarkerList() { return event.getMarkerList(); }
        @Override public Map<String, String> getMDCPropertyMap() { return event.getMDCPropertyMap(); }
        @Override public Map<String, String> getMdc() { return event.getMdc(); }
        @Override public long getTimeStamp() { return event.getTimeStamp(); }
        @Override public int getNanoseconds() { return event.getNanoseconds(); }
        @Override public long getSequenceNumber() { return event.getSequenceNumber(); }
        @Override public List<KeyValuePair> getKeyValuePairs() { return event.getKeyValuePairs(); }
        @Override public void prepareForDeferredProcessing() { event.prepareForDeferredProcessing(); }
    }

    private record SanitizedThrowable(IThrowableProxy throwable) implements IThrowableProxy {
        @Override public String getMessage() { return sanitize(throwable.getMessage()); }
        @Override public String getOverridingMessage() { return sanitize(throwable.getOverridingMessage()); }
        @Override public String getClassName() { return throwable.getClassName(); }
        @Override public StackTraceElementProxy[] getStackTraceElementProxyArray() { return throwable.getStackTraceElementProxyArray(); }
        @Override public int getCommonFrames() { return throwable.getCommonFrames(); }
        @Override public IThrowableProxy getCause() { return sanitize(throwable.getCause()); }
        @Override public IThrowableProxy[] getSuppressed() {
            return throwable.getSuppressed() == null ? null : Arrays.stream(throwable.getSuppressed())
                    .map(TelegramTokenMaskingLayout::sanitize).toArray(IThrowableProxy[]::new);
        }
        @Override public boolean isCyclic() { return throwable.isCyclic(); }
    }
}
