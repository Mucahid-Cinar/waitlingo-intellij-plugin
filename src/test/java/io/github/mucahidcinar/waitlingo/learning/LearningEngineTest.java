package io.github.mucahidcinar.waitlingo.learning;

import io.github.mucahidcinar.waitlingo.settings.WaitLingoSettings;
import io.github.mucahidcinar.waitlingo.vocabulary.WordEntry;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class LearningEngineTest {
    private static WordEntry word(String value, String level, String... tags) {
        return new WordEntry(value, value + "-tr", List.of("example"), "noun", level, List.of(tags));
    }

    @Test public void filtersByLevelAndTopic() {
        WaitLingoSettings.Data settings = new WaitLingoSettings.Data();
        settings.levels = List.of("B1");
        settings.topics = List.of("technology");
        List<WordEntry> filtered = LearningEngine.filter(List.of(
                word("compile", "B1", "technology"),
                word("house", "A2", "daily-life")), settings);
        assertEquals(List.of("compile"), filtered.stream().map(WordEntry::getWord).toList());
    }

    @Test public void fallsBackWhenFiltersHaveNoMatch() {
        WaitLingoSettings.Data settings = new WaitLingoSettings.Data();
        settings.levels = List.of("C2");
        List<WordEntry> input = List.of(word("compile", "B1", "technology"));
        assertEquals(input, LearningEngine.filter(input, settings));
    }

    @Test public void filtersTechnicalContentPack() {
        WaitLingoSettings.Data settings = new WaitLingoSettings.Data();
        settings.levels = List.of();
        settings.contentPack = "TESTING";
        List<WordEntry> filtered = LearningEngine.filter(List.of(
                word("mock", "B1", "developer", "testing"),
                word("deploy", "B1", "developer", "devops")), settings);
        assertEquals(List.of("mock"), filtered.stream().map(WordEntry::getWord).toList());
    }

    @Test public void keepsCurrentWordWhenNothingElseIsDue() {
        WaitLingoProgress progress = new WaitLingoProgress();
        WordEntry current = word("fallback", "B1", "developer");
        WordEntry scheduled = word("retry", "B1", "developer");
        progress.recordKnown(scheduled.id());
        assertEquals(current, LearningEngine.next(List.of(current, scheduled), current, progress));
    }

    @Test public void selectsOnlyWordsThatAreDue() {
        WaitLingoProgress progress = new WaitLingoProgress();
        WordEntry due = word("fallback", "B1", "developer");
        WordEntry scheduled = word("retry", "B1", "developer");
        progress.recordKnown(scheduled.id());
        assertEquals(due, LearningEngine.next(List.of(due, scheduled), null, progress));
    }

    @Test public void keepsPersonalWordsVisibleAcrossFilters() {
        WaitLingoSettings.Data settings = new WaitLingoSettings.Data();
        settings.levels = List.of("C2");
        settings.contentPack = "TESTING";
        WordEntry personal = word("fallback", "", "personal", "developer");
        WordEntry other = word("mock", "C2", "testing");
        assertEquals(List.of(personal, other), LearningEngine.filter(List.of(personal, other), settings));
    }
}
