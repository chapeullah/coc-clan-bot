package com.chapeullah.guccigoblin.telegram.logging;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.chapeullah.guccigoblin.telegram.TelegramUtils;

public class TelegramTokenMaskingLayout extends PatternLayout {

    @Override
    public String doLayout(ILoggingEvent event) {
        return TelegramUtils.maskBotToken(super.doLayout(event));
    }
}
