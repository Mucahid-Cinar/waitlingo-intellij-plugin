package io.github.mucahidcinar.waitlingo.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public final class WaitLingoConfigurable implements Configurable {
    private JPanel panel;
    private JComboBox<String> direction;
    private JBTextField rotation;
    private JBTextField topics;
    private JBTextField fontPercent;
    private final Map<String, JBCheckBox> levels = new LinkedHashMap<>();
    private JBCheckBox notifications;
    private JBCheckBox developerWords;
    private JBTextField customVocabularyPath;
    private JBCheckBox compactMode;
    private JBCheckBox adaptiveRotation;
    private JComboBox<String> contentPack;

    @Override public @Nls String getDisplayName() { return "WaitLingo"; }

    @Override public @Nullable JComponent createComponent() {
        direction = new JComboBox<>(new String[]{"English → Turkish", "Turkish → English"});
        rotation = new JBTextField();
        topics = new JBTextField();
        fontPercent = new JBTextField();
        for (String level : new String[]{"A1", "A2", "B1", "B2", "C1", "C2"}) levels.put(level, new JBCheckBox(level));
        notifications = new JBCheckBox("Show daily progress notifications");
        developerWords = new JBCheckBox("Include developer vocabulary pack");
        customVocabularyPath = new JBTextField();
        compactMode = new JBCheckBox("Compact status bar (show translation on hover)");
        adaptiveRotation = new JBCheckBox("Adapt rotation to coding and indexing activity");
        contentPack = new JComboBox<>(new String[]{"All content", "Git & Code Review", "Backend & API", "Testing", "DevOps", "Database", "Frontend"});

        JPanel levelPanel = new JPanel();
        levels.values().forEach(levelPanel::add);
        panel = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("Practice direction:"), direction)
                .addLabeledComponent(new JBLabel("Rotation interval (seconds):"), rotation)
                .addLabeledComponent(new JBLabel("CEFR levels:"), levelPanel)
                .addLabeledComponent(new JBLabel("Technical content pack:"), contentPack)
                .addLabeledComponent(new JBLabel("Topics/tags (comma-separated):"), topics)
                .addLabeledComponent(new JBLabel("Status bar font size (%):"), fontPercent)
                .addLabeledComponent(new JBLabel("Custom CSV/JSON vocabulary file:"), customVocabularyPath)
                .addComponent(compactMode).addComponent(adaptiveRotation)
                .addComponent(notifications).addComponent(developerWords)
                .addComponentFillVertically(new JPanel(), 0).getPanel();
        reset();
        return panel;
    }

    @Override public boolean isModified() {
        WaitLingoSettings.Data state = WaitLingoSettings.getInstance().getState();
        return direction.getSelectedIndex() != (WaitLingoSettings.TR_TO_EN.equals(state.direction) ? 1 : 0)
                || parseInt(rotation.getText(), 12) != state.rotationSeconds
                || parseInt(fontPercent.getText(), 100) != state.fontPercent
                || !selectedLevels().equals(state.levels)
                || !selectedTopics().equals(state.topics)
                || notifications.isSelected() != state.notificationsEnabled
                || developerWords.isSelected() != state.developerWordsEnabled
                || compactMode.isSelected() != state.compactMode
                || adaptiveRotation.isSelected() != state.adaptiveRotation
                || contentPack.getSelectedIndex() != packIndex(state.contentPack)
                || !customVocabularyPath.getText().trim().equals(state.customVocabularyPath == null ? "" : state.customVocabularyPath);
    }

    @Override public void apply() {
        WaitLingoSettings.Data state = new WaitLingoSettings.Data();
        state.direction = direction.getSelectedIndex() == 1 ? WaitLingoSettings.TR_TO_EN : WaitLingoSettings.EN_TO_TR;
        state.rotationSeconds = parseInt(rotation.getText(), 12);
        state.fontPercent = parseInt(fontPercent.getText(), 100);
        state.levels = selectedLevels();
        state.topics = selectedTopics();
        state.quizEnabled = false;
        state.notificationsEnabled = notifications.isSelected();
        state.developerWordsEnabled = developerWords.isSelected();
        state.customVocabularyPath = customVocabularyPath.getText().trim();
        state.compactMode = compactMode.isSelected();
        state.adaptiveRotation = adaptiveRotation.isSelected();
        state.contentPack = packCode(contentPack.getSelectedIndex());
        WaitLingoSettings.getInstance().update(state);
        reset();
    }

    @Override public void reset() {
        if (panel == null) return;
        WaitLingoSettings.Data state = WaitLingoSettings.getInstance().getState();
        direction.setSelectedIndex(WaitLingoSettings.TR_TO_EN.equals(state.direction) ? 1 : 0);
        rotation.setText(Integer.toString(state.rotationSeconds));
        fontPercent.setText(Integer.toString(state.fontPercent));
        topics.setText(String.join(", ", state.topics));
        levels.forEach((name, box) -> box.setSelected(state.levels.contains(name)));
        notifications.setSelected(state.notificationsEnabled);
        developerWords.setSelected(state.developerWordsEnabled);
        customVocabularyPath.setText(state.customVocabularyPath == null ? "" : state.customVocabularyPath);
        compactMode.setSelected(state.compactMode);
        adaptiveRotation.setSelected(state.adaptiveRotation);
        contentPack.setSelectedIndex(packIndex(state.contentPack));
    }

    @Override public void disposeUIResources() { panel = null; }

    private ArrayList<String> selectedLevels() {
        return levels.entrySet().stream().filter(entry -> entry.getValue().isSelected())
                .map(Map.Entry::getKey).collect(Collectors.toCollection(ArrayList::new));
    }

    private ArrayList<String> selectedTopics() {
        return Arrays.stream(topics.getText().split(",")).map(String::trim).map(String::toLowerCase)
                .filter(value -> !value.isBlank()).distinct().collect(Collectors.toCollection(ArrayList::new));
    }

    private static int parseInt(String value, int fallback) {
        try { return Integer.parseInt(value.trim()); } catch (NumberFormatException ignored) { return fallback; }
    }

    private static String packCode(int index) {
        return new String[]{"ALL", "GIT", "BACKEND", "TESTING", "DEVOPS", "DATABASE", "FRONTEND"}[Math.max(0, Math.min(6, index))];
    }

    private static int packIndex(String code) {
        return switch (code == null ? "ALL" : code) {
            case "GIT" -> 1; case "BACKEND" -> 2; case "TESTING" -> 3;
            case "DEVOPS" -> 4; case "DATABASE" -> 5; case "FRONTEND" -> 6; default -> 0;
        };
    }
}
