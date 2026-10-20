package com.chapeullah.guccigoblin.rating;

import java.util.Optional;

public record WarAverages(
        double stars,
        double destructionPercentage,
        Optional<Double> durationSeconds,
        double attackScore) {}
