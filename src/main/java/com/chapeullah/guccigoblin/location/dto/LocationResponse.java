package com.chapeullah.guccigoblin.location.dto;

public record LocationResponse(
        Integer id,
        String name,
        Boolean isCountry,
        String countryCode) {}
