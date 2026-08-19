package io.github.mucahidcinar.waitlingo.vocabulary;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.List;
import java.util.ArrayList;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import io.github.mucahidcinar.waitlingo.settings.WaitLingoSettings;

public final class VocabularyService {

    private static final String DEFAULT_RESOURCE = "/vocabulary/en-tr-words.json";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private VocabularyService() {
    }

    public static List<WordEntry> loadDefaultWords() {
        List<WordEntry> combined = new ArrayList<>(loadResource(DEFAULT_RESOURCE));
        combined.addAll(loadResource("/vocabulary/developer-en-tr.json"));
        combined.addAll(loadResource("/vocabulary/developer-phrases-en-tr.json"));
        String customPath = WaitLingoSettings.getInstance().getState().customVocabularyPath;
        if (customPath != null && !customPath.isBlank()) combined.addAll(loadCustom(Path.of(customPath)));
        return combined.isEmpty() ? fallbackWords() : List.copyOf(combined);
    }

    private static List<WordEntry> loadResource(String resource) {
        InputStream inputStream = VocabularyService.class.getResourceAsStream(resource);

        if (inputStream == null) {
            return List.of();
        }

        try (InputStream stream = inputStream) {
            List<WordEntry> words = OBJECT_MAPPER.readValue(
                    stream,
                    new TypeReference<List<WordEntry>>() {
                    }
            );

            if (words == null || words.isEmpty()) {
                return List.of();
            }

            return List.copyOf(words);
        } catch (Exception exception) {
            return List.of();
        }
    }

    private static List<WordEntry> loadCustom(Path path) {
        try {
            if (!Files.isRegularFile(path)) return List.of();
            if (path.getFileName().toString().toLowerCase().endsWith(".json")) {
                return OBJECT_MAPPER.readValue(path.toFile(), new TypeReference<List<WordEntry>>() {});
            }
            List<WordEntry> result = new ArrayList<>();
            for (String line : Files.readAllLines(path)) {
                if (line.isBlank() || line.toLowerCase().startsWith("word,")) continue;
                String[] values = line.split(",", -1);
                if (values.length < 2) continue;
                result.add(new WordEntry(values[0].trim(), values[1].trim(),
                        values.length > 2 ? List.of(values[2].trim()) : List.of(),
                        values.length > 3 ? values[3].trim() : "custom",
                        values.length > 4 ? values[4].trim() : "",
                        values.length > 5 ? Arrays.stream(values[5].split("\\|")).map(String::trim).toList() : List.of("custom")));
            }
            return result;
        } catch (Exception ignored) {
            return List.of();
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
