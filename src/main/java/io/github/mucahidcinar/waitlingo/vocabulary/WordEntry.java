package io.github.mucahidcinar.waitlingo.vocabulary;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WordEntry {

    private String word;
    private String translation;
    private Map<String, String> translations;
    private List<String> examples;
    private String category;
    private String level;
    private List<String> tags;

    public WordEntry() {
    }

    public WordEntry(
            String word,
            String translation,
            List<String> examples,
            String category,
            String level,
            List<String> tags
    ) {
        this.word = word;
        this.translation = translation;
        this.examples = examples;
        this.category = category;
        this.level = level;
        this.tags = tags;
    }

    public String getWord() {
        return word;
    }

    public String getTranslation() {
        return translation;
    }

    public Map<String, String> getTranslations() {
        Map<String, String> values = translations == null ? new LinkedHashMap<>() : new LinkedHashMap<>(translations);
        if (translation != null && !translation.isBlank()) values.putIfAbsent("tr", translation);
        return values;
    }

    public void setTranslations(Map<String, String> translations) { this.translations = translations; }

    public String getTranslation(String languageCode) {
        return getTranslations().getOrDefault(languageCode, translation == null ? "" : translation);
    }

    public List<String> getExamples() {
        return examples == null ? List.of() : examples;
    }

    public String getPrimaryExample() {
        if (examples == null || examples.isEmpty()) {
            return "";
        }
        return examples.get(0);
    }

    public String getCategory() {
        return category == null ? "" : category;
    }

    public String getLevel() {
        return level == null ? "" : level;
    }

    public List<String> getTags() {
        return tags == null ? List.of() : tags;
    }

    public String displayText() {
        return word + " — " + translation;
    }

    public String id() { return word == null ? "" : word.trim().toLowerCase(); }
}
