package com.chapeullah.guccigoblin;

import com.chapeullah.guccigoblin.builderbaseleague.dto.BuilderBaseLeaguesResponse;
import com.chapeullah.guccigoblin.capitalleague.dto.CapitalLeaguesResponse;
import com.chapeullah.guccigoblin.clan.dto.ClanResponse;
import com.chapeullah.guccigoblin.label.dto.LabelsResponse;
import com.chapeullah.guccigoblin.leaguetier.dto.LeagueTiersResponse;
import com.chapeullah.guccigoblin.location.dto.LocationsResponse;
import com.chapeullah.guccigoblin.member.dto.MemberResponses;
import com.chapeullah.guccigoblin.player.dto.PlayerResponse;
import com.chapeullah.guccigoblin.war.dto.WarResponse;
import com.chapeullah.guccigoblin.warleague.dto.WarLeaguesResponse;
import com.chapeullah.guccigoblin.war.dto.WarLogResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
@DependsOn("environmentVariablesValidator")
public class ClashOfClansClient {

    private final RestClient rest;

    @Value("${coc.api-token}")
    private String apiToken;

    public PlayerResponse getPlayer(@NonNull String playerTag) {
        log.debug("Fetching player: playerTag=\"{}\"", playerTag);
        var response = rest.get()
                .uri("/players/{playerTag}", playerTag)
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiToken)
                .retrieve()
                .body(PlayerResponse.class);
        if (response == null) {
            throw new IllegalStateException("Empty response for player: playerTag=" + playerTag);
        }
        log.debug("Received player: playerTag=\"{}\"", response.tag());
        return response;
    }

    public ClanResponse getClan(@NonNull String clanTag) {
        log.debug("Fetching clan: clanTag=\"{}\"", clanTag);
        var response = rest.get()
                .uri("/clans/{clanTag}", clanTag)
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiToken)
                .retrieve()
                .body(ClanResponse.class);
        if (response == null) {
            throw new IllegalStateException("Empty response for clan: clanTag=" + clanTag);
        }
        log.debug("Received clan: clanTag=\"{}\"", response.tag());
        return response;
    }

    public MemberResponses getClanMembers(@NonNull String clanTag) {
        log.debug("Fetching clan members: clanTag=\"{}\"", clanTag);
        var response = rest.get()
                .uri("/clans/{clanTag}/members", clanTag)
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiToken)
                .retrieve()
                .body(MemberResponses.class);
        if (response == null) {
            throw new IllegalStateException("Empty response for clan members: clanTag=" + clanTag);
        }
        log.debug("Received clan members: clanTag=\"{}\"", clanTag);
        return response;
    }

    public WarResponse getCurrentWar(@NonNull String clanTag) {
        log.debug("Fetching current war: clanTag=\"{}\"", clanTag);
        var response = rest.get()
                .uri("/clans/{clanTag}/currentwar", clanTag)
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiToken)
                .retrieve()
                .body(WarResponse.class);
        if (response == null) {
            throw new IllegalStateException("Empty response for current war: clanTag=" + clanTag);
        }
        log.debug("Received current war: clanTag=\"{}\"", clanTag);
        return response;
    }

    public WarLogResponse getWarLog(@NonNull String clanTag) {
        log.debug("Fetching war log: clanTag=\"{}\"", clanTag);
        var response = rest.get()
                .uri("/clans/{clanTag}/warlog", clanTag)
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiToken)
                .retrieve()
                .body(WarLogResponse.class);
        if (response == null) {
            throw new IllegalStateException("Empty response for war log: clanTag=" + clanTag);
        }
        log.debug("Received war log: clanTag=\"{}\"", clanTag);
        return response;
    }

    public String getCurrentRaid(@NonNull String clanTag) {
        log.debug("Fetching current raid: clanTag=\"{}\"", clanTag);
        String rawJson = rest.get()
                .uri("/clans/{clanTag}/capitalraidseasons?limit=1", clanTag)
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiToken)
                .retrieve()
                .body(String.class);
        if (rawJson == null || rawJson.isBlank()) {
            throw new IllegalStateException("Empty response for current raid: clanTag=\"" + clanTag + "\"");
        }
        log.debug("Received current raid: clanTag=\"{}\"", clanTag);
        return rawJson;
    }

    public LeagueTiersResponse getLeagueTiers() {
        log.debug("Fetching league tiers");
        var response = rest.get()
                .uri("/leaguetiers")
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiToken)
                .retrieve()
                .body(LeagueTiersResponse.class);
        if (response == null) {
            throw new IllegalStateException("Empty response for league tiers");
        }
        log.debug("Received league tiers");
        return response;
    }

    public LocationsResponse getLocations() {
        log.debug("Fetching locations");
        var response = rest.get()
                .uri("/locations")
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiToken)
                .retrieve()
                .body(LocationsResponse.class);
        if (response == null) {
            throw new IllegalStateException("Empty response for locations");
        }
        log.debug("Received locations");
        return response;
    }

    public CapitalLeaguesResponse getCapitalLeagues() {
        log.debug("Fetching capital leagues");
        var response = rest.get()
                .uri("/capitalleagues")
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiToken)
                .retrieve()
                .body(CapitalLeaguesResponse.class);
        if (response == null) {
            throw new IllegalStateException("Empty response for capital leagues");
        }
        log.debug("Received capital leagues");
        return response;
    }

    public WarLeaguesResponse getWarLeagues() {
        log.debug("Fetching war leagues");
        var response = rest.get()
                .uri("/warleagues")
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiToken)
                .retrieve()
                .body(WarLeaguesResponse.class);
        if (response == null) {
            throw new IllegalStateException("Empty response for war leagues");
        }
        log.debug("Received war leagues");
        return response;
    }

    public BuilderBaseLeaguesResponse getBuilderBaseLeagues() {
        log.debug("Fetching builder base leagues");
        var response = rest.get()
                .uri("/builderbaseleagues")
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiToken)
                .retrieve()
                .body(BuilderBaseLeaguesResponse.class);
        if (response == null) {
            throw new IllegalStateException("Empty response for builder base leagues");
        }
        log.debug("Received builder base leagues");
        return response;
    }

    public LabelsResponse getPlayerLabelsResponse() {
        log.debug("Fetching player labels");
        var response = rest.get()
                .uri("/labels/players")
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiToken)
                .retrieve()
                .body(LabelsResponse.class);
        if (response == null) {
            throw new IllegalStateException("Empty response for player labels");
        }
        log.debug("Received player labels");
        return response;
    }

    public LabelsResponse getClanLabelsResponse() {
        log.debug("Fetching clan labels");
        var response = rest.get()
                .uri("/labels/clans")
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiToken)
                .retrieve()
                .body(LabelsResponse.class);
        if (response == null) {
            throw new IllegalStateException("Empty response for clan labels");
        }
        log.debug("Received clan labels");
        return response;
    }

}
