package io.github.mucahidcinar.waitlingo.learning;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class WaitLingoProgressTest {
    @Test public void knownAnswerSchedulesForLater() {
        WaitLingoProgress progress = new WaitLingoProgress();
        progress.recordKnown("resilient");
        WaitLingoProgress.WordProgress item = progress.progress("resilient");
        assertEquals(1, item.correctCount);
        assertTrue(item.dueAt > System.currentTimeMillis());
        assertTrue(item.intervalDays >= 1);
    }

    @Test public void wrongAnswerReducesEaseAndSchedulesSoon() {
        WaitLingoProgress progress = new WaitLingoProgress();
        progress.recordAgain("resilient");
        WaitLingoProgress.WordProgress item = progress.progress("resilient");
        assertEquals(1, item.wrongCount);
        assertTrue(item.easeFactor < 2.5);
        assertTrue(item.dueAt > System.currentTimeMillis());
    }

    @Test public void canHideAWordForToday() {
        WaitLingoProgress progress = new WaitLingoProgress();
        progress.hideToday("rollback");
        assertTrue(progress.isHiddenToday("rollback"));
    }

    @Test public void dailyWordIsStableDuringTheDay() {
        WaitLingoProgress progress = new WaitLingoProgress();
        java.util.List<io.github.mucahidcinar.waitlingo.vocabulary.WordEntry> words = java.util.List.of(
                new io.github.mucahidcinar.waitlingo.vocabulary.WordEntry("mock", "sahte", java.util.List.of(), "developer", "B1", java.util.List.of()),
                new io.github.mucahidcinar.waitlingo.vocabulary.WordEntry("deploy", "dağıtmak", java.util.List.of(), "developer", "B1", java.util.List.of()));
        assertEquals(progress.dailyWord(words), progress.dailyWord(words));
    }
}
