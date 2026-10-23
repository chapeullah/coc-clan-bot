package com.chapeullah.cocclanbot.war.model;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static com.chapeullah.cocclanbot.war.WarFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class WarTest {

    @Test
    void scheduleChangesDoNotChangeIdentityOrHashCollections() {
        War existing = war(ORIGINAL_START, ORIGINAL_END);
        Set<War> wars = new HashSet<>();
        wars.add(existing);

        assertTrue(existing.updateTimes(SHIFTED_START, SHIFTED_END));

        assertTrue(wars.contains(existing));
        assertEquals(existing, war(SHIFTED_START, FINAL_END));
        assertEquals(existing.hashCode(), war(SHIFTED_START, FINAL_END).hashCode());
    }

    @Test
    void confirmedScheduleCannotBeChangedButIdenticalReplayIsAllowed() {
        War existing = war(SHIFTED_START, FINAL_END);
        existing.markWarLogSynced(NOW);

        assertFalse(existing.updateTimes(SHIFTED_START, FINAL_END));
        assertThrows(IllegalStateException.class, () -> existing.updateTimes(ORIGINAL_START, ORIGINAL_END));
        assertEquals(FINAL_END, existing.getEndTime());
    }

    @Test
    void invalidScheduleDoesNotPartiallyUpdateTheWar() {
        War existing = war(ORIGINAL_START, ORIGINAL_END);

        assertThrows(IllegalArgumentException.class,
                () -> existing.updateTimes(PREPARATION.minusSeconds(1), SHIFTED_END));
        assertThrows(IllegalArgumentException.class,
                () -> existing.updateTimes(SHIFTED_START, SHIFTED_START));
        assertEquals(ORIGINAL_START, existing.getStartTime());
        assertEquals(ORIGINAL_END, existing.getEndTime());
    }
}
