package com.chapeullah.guccigoblin.war.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WarLogResponse(List<WarLogEntryResponse> items) {}