package io.github.mucahidcinar.waitlingo.vocabulary;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.List;

public final class VocabularyService {

    private static final String DEFAULT_RESOURCE = "/vocabulary/en-tr-words.json";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private VocabularyService() {
    }

    public static List<WordEntry> loadDefaultWords() {
        InputStream inputStream = VocabularyService.class.getResourceAsStream(DEFAULT_RESOURCE);

        if (inputStream == null) {
            return fallbackWords();
        }

        try (InputStream stream = inputStream) {
            List<WordEntry> words = OBJECT_MAPPER.readValue(
                    stream,
                    new TypeReference<List<WordEntry>>() {
                    }
            );

            if (words == null || words.isEmpty()) {
                return fallbackWords();
            }

            return List.copyOf(words);
        } catch (Exception exception) {
            return fallbackWords();
        }
    }

    private static List<WordEntry> fallbackWords() {
        return List.of(
                new WordEntry(
                        "resilient",
                        "dayanıklı",
                        List.of("A resilient service recovers quickly after failure."),
                        "developer",
                        "B2",
                        List.of("architecture", "reliability")
                )
        );
    }
}