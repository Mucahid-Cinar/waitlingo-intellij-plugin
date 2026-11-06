package io.github.mucahidcinar.waitlingo.actions;

import io.github.mucahidcinar.waitlingo.vocabulary.WordEntry;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class LearnSelectedWordActionTest {
    private static WordEntry word(String value) {
        return new WordEntry(value, "meaning", List.of(), "developer", "B1", List.of("developer"));
    }

    @Test public void findsExactPhraseAndIdentifierPart() {
        List<WordEntry> words = List.of(word("graceful degradation"), word("retry"));
        assertEquals("graceful degradation",
                LearnSelectedWordAction.findEntry(words, "Graceful Degradation").getWord());
        assertEquals("retry", LearnSelectedWordAction.findEntry(words, "retryCount").getWord());
        assertNull(LearnSelectedWordAction.findEntry(words, "unknownTerm"));
    }
}
