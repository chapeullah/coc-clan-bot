package com.chapeullah.guccigoblin.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
@ConditionalOnProperty(name = "app.readiness-file")
public class ApplicationReadiness {

    private final Path readinessFile;

    public ApplicationReadiness(@Value("${app.readiness-file}") String readinessFile) {
        this.readinessFile = Path.of(readinessFile);
    }

    @PostConstruct
    @PreDestroy
    public void clearReadiness() throws IOException {
        Files.deleteIfExists(readinessFile);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void markReady() throws IOException {
        Files.writeString(readinessFile, "ready");
    }
}
