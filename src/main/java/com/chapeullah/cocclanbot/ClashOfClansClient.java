package com.chapeullah.cocclanbot;

import com.chapeullah.cocclanbot.builderbaseleague.dto.BuilderBaseLeaguesResponse;
import com.chapeullah.cocclanbot.capitalleague.dto.CapitalLeaguesResponse;
import com.chapeullah.cocclanbot.clan.dto.ClanResponse;
import com.chapeullah.cocclanbot.label.dto.LabelsResponse;
import com.chapeullah.cocclanbot.leaguetier.dto.LeagueTiersResponse;
import com.chapeullah.cocclanbot.location.dto.LocationsResponse;
import com.chapeullah.cocclanbot.member.dto.MemberResponses;
import com.chapeullah.cocclanbot.player.dto.PlayerResponse;
import com.chapeullah.cocclanbot.war.dto.WarResponse;
import com.chapeullah.cocclanbot.warleague.dto.WarLeaguesResponse;
import com.chapeullah.cocclanbot.war.dto.WarLogResponse;
import lombok.NonNull;
import com.chapeullah.cocclanbot.diagnostics.HttpRequestDiagnostics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.concurrent.CancellationException;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
@DependsOn("environmentVariablesService")
public class ClashOfClansClient {

    private static final int MAX_RETRIES = 2;
    private static final long DELAY_MS = 1000;

    private final RestClient rest;

    @Value("${coc.api-token}")
    private String apiToken;

    public PlayerResponse getPlayer(@NonNull String playerTag) {
        log.debug("Fetching player: playerTag=\"{}\"", playerTag);
        var response = executeWithRetry("getPlayer", () -> rest.get()
                .uri("/players/{playerTag}", playerTag)
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiToken)
                .retrieve()
                .body(PlayerResponse.class));
        if (response == null) {
            throw new IllegalStateException("Empty response for player: playerTag=" + playerTag);
        }
        log.debug("Received player: playerTag=\"{}\"", response.tag());
        return response;
    }

    public ClanResponse getClan(@NonNull String clanTag) {
        log.debug("Fetching clan: clanTag=\"{}\"", clanTag);
        var response = executeWithRetry("getClan", () -> rest.get()
                .uri("/clans/{clanTag}", clanTag)
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiToken)
                .retrieve()
                .body(ClanResponse.class));
        if (response == null) {
            throw new IllegalStateException("Empty response for clan: clanTag=" + clanTag);
        }
        log.debug("Received clan: clanTag=\"{}\"", response.tag());
        return response;
    }

    public MemberResponses getClanMembers(@NonNull String clanTag) {
        log.debug("Fetching clan members: clanTag=\"{}\"", clanTag);
        var response = executeWithRetry("getClanMembers", () -> rest.get()
                .uri("/clans/{clanTag}/members", clanTag)
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiToken)
                .retrieve()
                .body(MemberResponses.class));
        if (response == null) {
            throw new IllegalStateException("Empty response for clan members: clanTag=" + clanTag);
        }
        log.debug("Received clan members: clanTag=\"{}\"", clanTag);
        return response;
    }

    public WarResponse getCurrentWar(@NonNull String clanTag) {
        log.debug("Fetching current war: clanTag=\"{}\"", clanTag);
        var response = executeWithRetry("getCurrentWar", () -> rest.get()
                .uri("/clans/{clanTag}/currentwar", clanTag)
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiToken)
                .retrieve()
                .body(WarResponse.class));
        if (response == null) {
            throw new IllegalStateException("Empty response for current war: clanTag=" + clanTag);
        }
        log.debug("Received current war: clanTag=\"{}\"", clanTag);
        return response;
    }

    public WarLogResponse getWarLog(@NonNull String clanTag) {
        log.debug("Fetching war log: clanTag=\"{}\"", clanTag);
        var response = executeWithRetry("getWarLog", () -> rest.get()
                .uri("/clans/{clanTag}/warlog", clanTag)
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiToken)
                .retrieve()
                .body(WarLogResponse.class));
        if (response == null) {
            throw new IllegalStateException("Empty response for war log: clanTag=" + clanTag);
        }
        log.debug("Received war log: clanTag=\"{}\"", clanTag);
        return response;
    }

    public String getCurrentRaid(@NonNull String clanTag) {
        log.debug("Fetching current raid: clanTag=\"{}\"", clanTag);
        String rawJson = executeWithRetry("getCurrentRaid", () -> rest.get()
                .uri("/clans/{clanTag}/capitalraidseasons?limit=1", clanTag)
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiToken)
                .retrieve()
                .body(String.class));
        if (rawJson == null || rawJson.isBlank()) {
            throw new IllegalStateException("Empty response for current raid: clanTag=\"" + clanTag + "\"");
        }
        log.debug("Received current raid: clanTag=\"{}\"", clanTag);
        return rawJson;
    }

    public LeagueTiersResponse getLeagueTiers() {
        log.debug("Fetching league tiers");
        var response = executeWithRetry("getLeagueTiers", () -> rest.get()
                .uri("/leaguetiers")
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiToken)
                .retrieve()
                .body(LeagueTiersResponse.class));
        if (response == null) {
            throw new IllegalStateException("Empty response for league tiers");
        }
        log.debug("Received league tiers");
        return response;
    }

    public LocationsResponse getLocations() {
        log.debug("Fetching locations");
        var response = executeWithRetry("getLocations", () -> rest.get()
                .uri("/locations")
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiToken)
                .retrieve()
                .body(LocationsResponse.class));
        if (response == null) {
            throw new IllegalStateException("Empty response for locations");
        }
        log.debug("Received locations");
        return response;
    }

    public CapitalLeaguesResponse getCapitalLeagues() {
        log.debug("Fetching capital leagues");
        var response = executeWithRetry("getCapitalLeagues", () -> rest.get()
                .uri("/capitalleagues")
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiToken)
                .retrieve()
                .body(CapitalLeaguesResponse.class));
        if (response == null) {
            throw new IllegalStateException("Empty response for capital leagues");
        }
        log.debug("Received capital leagues");
        return response;
    }

    public WarLeaguesResponse getWarLeagues() {
        log.debug("Fetching war leagues");
        var response = executeWithRetry("getWarLeagues", () -> rest.get()
                .uri("/warleagues")
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiToken)
                .retrieve()
                .body(WarLeaguesResponse.class));
        if (response == null) {
            throw new IllegalStateException("Empty response for war leagues");
        }
        log.debug("Received war leagues");
        return response;
    }

    public BuilderBaseLeaguesResponse getBuilderBaseLeagues() {
        log.debug("Fetching builder base leagues");
        var response = executeWithRetry("getBuilderBaseLeagues", () -> rest.get()
                .uri("/builderbaseleagues")
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiToken)
                .retrieve()
                .body(BuilderBaseLeaguesResponse.class));
        if (response == null) {
            throw new IllegalStateException("Empty response for builder base leagues");
        }
        log.debug("Received builder base leagues");
        return response;
    }

    public LabelsResponse getPlayerLabelsResponse() {
        log.debug("Fetching player labels");
        var response = executeWithRetry("getPlayerLabels", () -> rest.get()
                .uri("/labels/players")
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiToken)
                .retrieve()
                .body(LabelsResponse.class));
        if (response == null) {
            throw new IllegalStateException("Empty response for player labels");
        }
        log.debug("Received player labels");
        return response;
    }

    public LabelsResponse getClanLabelsResponse() {
        log.debug("Fetching clan labels");
        var response = executeWithRetry("getClanLabels", () -> rest.get()
                .uri("/labels/clans")
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiToken)
                .retrieve()
                .body(LabelsResponse.class));
        if (response == null) {
            throw new IllegalStateException("Empty response for clan labels");
        }
        log.debug("Received clan labels");
        return response;
    }

    private <T> T executeWithRetry(String operation, Supplier<T> request) {
        try (HttpRequestDiagnostics diagnostics = new HttpRequestDiagnostics(operation, MAX_RETRIES + 1)) {
            for (int retry = 0; ; retry++) {
                diagnostics.beginAttempt(retry + 1);
                if (Thread.currentThread().isInterrupted()) {
                    CancellationException exception = new CancellationException("HTTP request interrupted");
                    diagnostics.failed(exception);
                    diagnostics.finish();
                    log.warn("HTTP request cancelled before execution: {}", diagnostics.summary());
                    throw exception;
                }
                RuntimeException attemptFailure;
                try {
                    T response = request.get();
                    if (Thread.currentThread().isInterrupted()) {
                        throw new CancellationException("HTTP request interrupted");
                    }
                    diagnostics.succeeded();
                    if (retry > 0) {
                        log.info("HTTP request recovered after retry: {}", diagnostics.summary());
                    } else {
                        log.debug("HTTP request succeeded: {}", diagnostics.summary());
                    }
                    return response;
                } catch (RuntimeException exception) {
                    attemptFailure = exception;
                    diagnostics.failed(exception);
                    boolean interrupted = Thread.currentThread().isInterrupted()
                            || exception instanceof ResourceAccessException resource
                            && resource.contains(InterruptedException.class);
                    boolean retrying = exception instanceof ResourceAccessException && !interrupted && retry < MAX_RETRIES;
                    if (!retrying) diagnostics.finish();
                    log.warn("HTTP request failed: {}, retrying={}, retryDelayMs={}",
                            diagnostics.summary(), retrying, retrying ? DELAY_MS : 0);
                    if (interrupted) {
                        Thread.currentThread().interrupt();
                    }
                    if (!retrying) {
                        throw exception;
                    }
                }
                diagnostics.retryDelay();
                try {
                    Thread.sleep(DELAY_MS);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    IllegalStateException exception = new IllegalStateException("HTTP request retry interrupted", interrupted);
                    exception.addSuppressed(attemptFailure);
                    diagnostics.failed(exception);
                    diagnostics.finish();
                    log.warn("HTTP request retry interrupted: {}", diagnostics.summary());
                    throw exception;
                }
            }
        }
    }

}
