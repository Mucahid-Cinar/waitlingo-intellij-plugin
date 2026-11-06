package io.github.mucahidcinar.waitlingo.learning;

import io.github.mucahidcinar.waitlingo.settings.WaitLingoSettings;
import io.github.mucahidcinar.waitlingo.vocabulary.WordEntry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class LearningEngine {
    private LearningEngine() {}

    public static List<WordEntry> filter(List<WordEntry> words, WaitLingoSettings.Data settings) {
        List<WordEntry> result = words.stream()
                .filter(word -> word.getTags().contains("personal") ||
                        ((settings.levels.isEmpty() || settings.levels.contains(word.getLevel()))
                                && (settings.topics.isEmpty() || word.getTags().stream().anyMatch(settings.topics::contains))
                                && (settings.developerWordsEnabled || !word.getTags().contains("developer"))
                                && matchesPack(word, settings.contentPack)))
                .toList();
        return result.isEmpty() ? words : result;
    }

    private static boolean matchesPack(WordEntry word, String pack) {
        if (pack == null || "ALL".equals(pack)) return true;
        List<String> tags = switch (pack) {
            case "GIT" -> List.of("git", "collaboration", "code-review");
            case "BACKEND" -> List.of("backend", "api", "architecture");
            case "TESTING" -> List.of("testing");
            case "DEVOPS" -> List.of("devops", "deployment");
            case "DATABASE" -> List.of("database");
            case "FRONTEND" -> List.of("frontend");
            default -> List.of();
        };
        return word.getTags().stream().anyMatch(tags::contains);
    }

    public static WordEntry next(List<WordEntry> words, WordEntry current, WaitLingoProgress progress) {
        return next(words, current, progress, "");
    }

    public static WordEntry next(List<WordEntry> words, WordEntry current, WaitLingoProgress progress, String context) {
        long now = System.currentTimeMillis();
        List<WordEntry> due = new ArrayList<>(words.stream()
                .filter(word -> current == null || !word.id().equals(current.id()))
                .filter(word -> progress.progress(word.id()).dueAt <= now)
                .toList());
        if (due.isEmpty()) {
            return current != null ? current : words.stream()
                    .min(Comparator.comparingLong(word -> progress.progress(word.id()).dueAt))
                    .orElse(null);
        }
        String normalizedContext = context == null ? "" : context.toLowerCase();
        due.sort(Comparator
                .comparingInt((WordEntry word) -> contextScore(word, normalizedContext) * 2
                        + progress.progress(word.id()).wrongCount * 4
                        + (progress.progress(word.id()).correctCount > 0 ? 2 : 0)).reversed()
                .thenComparingLong(word -> progress.progress(word.id()).dueAt));
        int candidateWindow = Math.min(3, due.size());
        return due.get(ThreadLocalRandom.current().nextInt(candidateWindow));
    }

    private static int contextScore(WordEntry word, String context) {
        if (context.isBlank()) return 0;
        int score = word.getTags().contains("developer") ? 1 : 0;
        String contextToken = context.replaceAll("[^a-z0-9]", "");
        String wordToken = word.id().replaceAll("[^a-z0-9]", "");
        if (wordToken.length() >= 4 && contextToken.contains(wordToken)) score += 8;
        if ((context.endsWith(".java") || context.endsWith(".kt") || context.endsWith(".cs"))
                && word.getTags().stream().anyMatch(tag -> List.of("developer", "architecture", "design", "concurrency").contains(tag))) score += 4;
        if ((context.contains("test") || context.contains("spec")) && word.getTags().contains("testing")) score += 6;
        if ((context.contains("gradle") || context.contains("pom.xml") || context.contains("docker"))
                && word.getTags().stream().anyMatch(tag -> List.of("devops", "architecture").contains(tag))) score += 5;
        return score;
    }
}
