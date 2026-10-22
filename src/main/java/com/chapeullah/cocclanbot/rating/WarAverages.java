package com.chapeullah.cocclanbot.rating;

import java.util.Optional;

public record WarAverages(
        double stars,
        double destructionPercentage,
        Optional<Double> durationSeconds,
        double attackScore) {}
