package io.github.mucahidcinar.waitlingo.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.TextRange;
import io.github.mucahidcinar.waitlingo.WaitLingoCommands;
import io.github.mucahidcinar.waitlingo.learning.WaitLingoProgress;
import io.github.mucahidcinar.waitlingo.vocabulary.VocabularyService;
import io.github.mucahidcinar.waitlingo.vocabulary.WordEntry;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

public final class LearnSelectedWordAction extends AnAction {
    @Override public void update(@NotNull AnActionEvent event) {
        Editor editor = event.getData(CommonDataKeys.EDITOR);
        event.getPresentation().setEnabledAndVisible(event.getProject() != null && editor != null
                && validSelection(editor.getSelectionModel().getSelectedText()));
    }

    @Override public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        Editor editor = event.getData(CommonDataKeys.EDITOR);
        if (project == null || editor == null) return;
        String selected = editor.getSelectionModel().getSelectedText();
        if (!validSelection(selected)) return;
        selected = selected.trim();

        List<WordEntry> vocabulary = VocabularyService.loadDefaultWords();
        WordEntry entry = findEntry(vocabulary, selected);
        if (entry == null) {
            String translation = Messages.showInputDialog(project,
                    "Enter the Turkish meaning of “" + selected + "”:",
                    "Add to WaitLingo", Messages.getQuestionIcon());
            if (translation == null || translation.isBlank()) return;
            WaitLingoProgress.getInstance().savePersonalWord(selected, translation, selectedLine(editor));
            entry = findEntry(VocabularyService.loadDefaultWords(), selected);
        }
        if (entry == null) return;
        WaitLingoProgress.getInstance().markFavorite(entry.id());
        project.getMessageBus().syncPublisher(WaitLingoCommands.TOPIC).showWord(entry.id());
    }

    static WordEntry findEntry(List<WordEntry> vocabulary, String selected) {
        String normalized = selected.trim().toLowerCase(Locale.ROOT);
        for (WordEntry entry : vocabulary) {
            if (entry.id().equals(normalized)) return entry;
        }
        String[] parts = selected.replaceAll("([a-z])([A-Z])", "$1 $2").split("[^\\p{L}]+");
        for (String part : parts) {
            if (part.length() < 4) continue;
            String token = part.toLowerCase(Locale.ROOT);
            for (WordEntry entry : vocabulary) {
                if (entry.id().equals(token)) return entry;
            }
        }
        return null;
    }

    private static boolean validSelection(String selected) {
        return selected != null && !selected.isBlank() && selected.length() <= 80
                && !selected.contains("\n") && !selected.contains("\r");
    }

    private static String selectedLine(Editor editor) {
        int line = editor.getDocument().getLineNumber(editor.getSelectionModel().getSelectionStart());
        int start = editor.getDocument().getLineStartOffset(line);
        int end = editor.getDocument().getLineEndOffset(line);
        String text = editor.getDocument().getText(new TextRange(start, end)).trim();
        return text.length() <= 160 ? text : "";
    }
}
