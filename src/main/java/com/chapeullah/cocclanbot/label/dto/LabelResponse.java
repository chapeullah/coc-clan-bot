package com.chapeullah.cocclanbot.label.dto;

public record LabelResponse(
        Integer id,
        String name,
        LabelIconUrlsResponse iconUrls) {}
