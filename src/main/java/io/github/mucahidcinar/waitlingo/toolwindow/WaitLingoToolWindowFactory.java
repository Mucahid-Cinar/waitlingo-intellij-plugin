package io.github.mucahidcinar.waitlingo.toolwindow;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.util.ui.JBUI;
import io.github.mucahidcinar.waitlingo.learning.WaitLingoProgress;
import io.github.mucahidcinar.waitlingo.vocabulary.VocabularyService;
import io.github.mucahidcinar.waitlingo.vocabulary.WordEntry;
import org.jetbrains.annotations.NotNull;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public final class WaitLingoToolWindowFactory implements ToolWindowFactory {
    @Override public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        AtomicReference<List<WordEntry>> words = new AtomicReference<>(VocabularyService.loadDefaultWords());
        AtomicInteger personalWordCount = new AtomicInteger(WaitLingoProgress.getInstance().getState().personalTranslations.size());
        WaitLingoProgress.Data progress = WaitLingoProgress.getInstance().getState();
        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBorder(JBUI.Borders.empty(12));

        JPanel summary = new JPanel(new BorderLayout(5, 5));
        JBLabel summaryLabel = new JBLabel();
        summary.add(summaryLabel, BorderLayout.NORTH);
        JProgressBar bar = new JProgressBar(0, words.get().size());
        bar.setStringPainted(true);
        summary.add(bar, BorderLayout.CENTER);

        DefaultListModel<String> model = new DefaultListModel<>();
        JBList<String> list = new JBList<>(model);
        JBTextField search = new JBTextField();
        search.getEmptyText().setText("Search word, translation or tag");
        Runnable refresh = () -> {
            int currentCount = progress.personalTranslations.size();
            if (currentCount != personalWordCount.get()) {
                words.set(VocabularyService.loadDefaultWords());
                personalWordCount.set(currentCount);
                bar.setMaximum(words.get().size());
            }
            summaryLabel.setText("🔥 " + progress.streak + " day streak · Today: "
                    + progress.todayKnown + "/" + progress.todaySeen + " known"
                    + " · This week: " + progress.weekKnown + "/" + progress.weekSeen
                    + (progress.pinnedWord == null || progress.pinnedWord.isBlank() ? "" : " · Pinned: " + progress.pinnedWord)
                    + (progress.snoozedUntil > System.currentTimeMillis() ? " · Snoozed" : ""));
            bar.setValue((int) progress.words.values().stream().filter(item -> item.correctCount > 0).count());
            bar.setString(bar.getValue() + " / " + words.get().size() + " words practiced");
            String query = search.getText().trim().toLowerCase();
            model.clear();
            words.get().stream().filter(word -> query.isBlank()
                            || word.getWord().toLowerCase().contains(query)
                            || word.getTranslation("tr").toLowerCase().contains(query)
                            || word.getTags().stream().anyMatch(tag -> tag.contains(query)))
                    .limit(250).forEach(word -> {
                        WaitLingoProgress.WordProgress item = progress.words.get(word.id());
                        model.addElement((item != null && item.favorite ? "★ " : "")
                                + word.getWord() + " — " + word.getTranslation("tr") + "  [" + word.getLevel() + "]");
                    });
        };
        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { refresh.run(); }
            public void removeUpdate(DocumentEvent e) { refresh.run(); }
            public void changedUpdate(DocumentEvent e) { refresh.run(); }
        });
        refresh.run();

        JButton settings = new JButton("Open WaitLingo Settings");
        settings.addActionListener(event -> com.intellij.openapi.options.ShowSettingsUtil.getInstance()
                .showSettingsDialog(project, "WaitLingo"));
        JPanel center = new JPanel(new BorderLayout(0, 6));
        center.add(search, BorderLayout.NORTH);
        center.add(new com.intellij.ui.components.JBScrollPane(list), BorderLayout.CENTER);
        center.add(settings, BorderLayout.SOUTH);
        root.add(summary, BorderLayout.NORTH);
        root.add(center, BorderLayout.CENTER);

        Content content = ContentFactory.getInstance().createContent(root, "Vocabulary", false);
        toolWindow.getContentManager().addContent(content);
        Timer refreshTimer = new Timer(2000, event -> refresh.run());
        refreshTimer.start();
        com.intellij.openapi.util.Disposer.register(content, refreshTimer::stop);
    }
}
