package io.github.mucahidcinar.waitlingo;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.ide.IdeEventQueue;
import com.intellij.ide.ui.UISettings;
import com.intellij.execution.ExecutionManager;
import com.intellij.task.ProjectTaskContext;
import com.intellij.task.ProjectTaskListener;
import com.intellij.task.ProjectTaskManager;
import com.intellij.openapi.wm.CustomStatusBarWidget;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.ui.JBColor;
import io.github.mucahidcinar.waitlingo.ui.WordDetailsPopup;
import io.github.mucahidcinar.waitlingo.learning.LearningEngine;
import io.github.mucahidcinar.waitlingo.learning.WaitLingoProgress;
import io.github.mucahidcinar.waitlingo.settings.WaitLingoSettings;
import io.github.mucahidcinar.waitlingo.settings.WaitLingoSettingsListener;
import io.github.mucahidcinar.waitlingo.vocabulary.VocabularyService;
import io.github.mucahidcinar.waitlingo.vocabulary.WordEntry;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;

public class WaitLingoWidget implements CustomStatusBarWidget, Disposable {

    private final Project project;
    private final WaitLingoPanel component;
    private final Timer timer;
    private List<WordEntry> allWords = VocabularyService.loadDefaultWords();
    private List<WordEntry> words;
    private WordEntry current;
    private boolean buildActive;
    private int rotationCount;

    public WaitLingoWidget(Project project) {
        this.project = project;
        this.component = new WaitLingoPanel(this::markWordAsKnown, this::showNextWord, this::markWordAgain,
                this::toggleFavorite, this::markWordSeen, this::togglePinned, this::snooze, this::hideToday);
        reloadSettings();
        this.timer = new Timer(delay(), event -> showNextWord());
        this.component.setTimerActions(this.timer::stop, this.timer::restart);
        this.timer.start();
        project.getMessageBus().connect(this); // ties connection to widget lifecycle
        com.intellij.openapi.application.ApplicationManager.getApplication().getMessageBus().connect(this)
                .subscribe(WaitLingoSettingsListener.TOPIC, this::reloadSettings);
        project.getMessageBus().connect(this).subscribe(WaitLingoCommands.TOPIC, this::showNextWord);
        project.getMessageBus().connect(this).subscribe(ProjectTaskListener.TOPIC, new ProjectTaskListener() {
            @Override public void started(@NotNull ProjectTaskContext context) {
                buildActive = true;
                showNextWord();
            }

            @Override public void finished(@NotNull ProjectTaskManager.Result result) {
                buildActive = false;
                component.setBuildActive(false);
                updateAdaptiveDelay();
            }
        });
    }

    private void showNextWord() {
        WaitLingoProgress progress = WaitLingoProgress.getInstance();
        if (progress.isSnoozed()) {
            component.setVisible(false);
            updateAdaptiveDelay();
            return;
        }
        component.setVisible(true);
        List<WordEntry> eligible = words.stream().filter(word -> !progress.isHiddenToday(word.id())).toList();
        if (eligible.isEmpty()) eligible = words;
        String pinnedId = progress.getState().pinnedWord;
        if (pinnedId != null && !pinnedId.isBlank()) {
            List<WordEntry> finalEligible = eligible;
            current = eligible.stream().filter(word -> word.id().equals(pinnedId)).findFirst()
                    .orElseGet(() -> LearningEngine.next(finalEligible, current, progress, editorContext()));
        } else if (++rotationCount % 5 == 0) {
            String dailyId = progress.dailyWord(eligible);
            List<WordEntry> finalEligible = eligible;
            current = eligible.stream().filter(word -> word.id().equals(dailyId)).findFirst()
                    .orElseGet(() -> LearningEngine.next(finalEligible, current, progress, editorContext()));
        } else {
            current = LearningEngine.next(eligible, current, progress, editorContext());
        }
        component.setSmartCompact(isAttentionMode());
        component.setBuildActive(buildActive);
        component.setWord(current, WaitLingoSettings.getInstance().getState(), words);
        updateAdaptiveDelay();
    }

    private void markWordAsKnown(WordEntry entry) {
        WaitLingoProgress.getInstance().recordKnown(entry.id());
        showNextWord();
    }

    private void markWordAgain(WordEntry entry) {
        WaitLingoProgress.getInstance().recordAgain(entry.id());
        showNextWord();
    }

    private void toggleFavorite(WordEntry entry) { WaitLingoProgress.getInstance().toggleFavorite(entry.id()); }

    private void markWordSeen(WordEntry entry) { WaitLingoProgress.getInstance().recordSeen(entry.id()); }

    private void togglePinned(WordEntry entry) { WaitLingoProgress.getInstance().togglePinned(entry.id()); }

    private void snooze() {
        WaitLingoProgress.getInstance().snoozeMinutes(30);
        component.setVisible(false);
    }

    private void hideToday(WordEntry entry) {
        WaitLingoProgress.getInstance().hideToday(entry.id());
        showNextWord();
    }

    private int delay() { return WaitLingoSettings.getInstance().getState().rotationSeconds * 1000; }

    private String editorContext() {
        var files = FileEditorManager.getInstance(project).getSelectedFiles();
        return (files.length == 0 ? "" : files[0].getName()) + " " + project.getName();
    }

    private boolean isAttentionMode() {
        return UISettings.getInstance().getPresentationMode()
                || ExecutionManager.getInstance(project).getRunningProcesses().length > 0;
    }

    private void updateAdaptiveDelay() {
        if (timer == null) return;
        int nextDelay = delay();
        if (WaitLingoSettings.getInstance().getState().adaptiveRotation) {
            if (buildActive || DumbService.isDumb(project)) nextDelay = Math.max(5_000, nextDelay / 2);
            else if (IdeEventQueue.getInstance().getIdleTime() < 4_000) nextDelay = Math.min(3_600_000, nextDelay * 3);
        }
        timer.setDelay(nextDelay);
        timer.setInitialDelay(nextDelay);
    }

    private void reloadSettings() {
        allWords = VocabularyService.loadDefaultWords();
        words = LearningEngine.filter(allWords, WaitLingoSettings.getInstance().getState());
        if (timer != null) { timer.setDelay(delay()); timer.restart(); }
        showNextWord();
    }

    @Override
    public @NotNull JComponent getComponent() {
        return component;
    }

    @Override
    public @NotNull String ID() {
        return WaitLingoWidgetFactory.ID;
    }

    @Override
    public void install(@NotNull StatusBar statusBar) {
        // No status bar reference needed for now.
    }

    @Override
    public void dispose() {
        timer.stop();
    }

    private static final class WaitLingoPanel extends JPanel {

        private static final int WIDTH = 360;
        private static final int HEIGHT = 26;

        private final Consumer<WordEntry> knowWordAction;
        private final Runnable nextWordAction;
        private final Consumer<WordEntry> againAction;
        private final Consumer<WordEntry> favoriteAction;
        private final Consumer<WordEntry> seenAction;
        private final Consumer<WordEntry> pinAction;
        private final Runnable snoozeAction;
        private final Consumer<WordEntry> hideTodayAction;
        private List<WordEntry> wordPool = List.of();

        private WordEntry wordEntry;
        private WaitLingoSettings.Data settings;
        private boolean hovered;
        private boolean hoverRevealed;
        private final Timer hoverTimer;
        private float transitionAlpha = 1f;
        private Timer transitionTimer;

        private WaitLingoPanel(Consumer<WordEntry> knowWordAction, Runnable nextWordAction,
                               Consumer<WordEntry> againAction, Consumer<WordEntry> favoriteAction,
                               Consumer<WordEntry> seenAction, Consumer<WordEntry> pinAction,
                               Runnable snoozeAction, Consumer<WordEntry> hideTodayAction) {
            this.knowWordAction = knowWordAction;
            this.nextWordAction = nextWordAction;
            this.againAction = againAction;
            this.favoriteAction = favoriteAction;
            this.seenAction = seenAction;
            this.pinAction = pinAction;
            this.snoozeAction = snoozeAction;
            this.hideTodayAction = hideTodayAction;

            setOpaque(false);
            setPreferredSize(new Dimension(WIDTH, HEIGHT));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText("Click to learn this word.");
            hoverTimer = new Timer(450, event -> {
                hoverRevealed = true;
                repaint();
            });
            hoverTimer.setRepeats(false);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent event) {
                    if (wordEntry != null) {
                        if (SwingUtilities.isRightMouseButton(event)) {
                            showQuickMenu(event);
                            return;
                        }
                        seenAction.accept(wordEntry);
                        timerPause.run();
                        WordDetailsPopup.show(
                                wordEntry,
                                WaitLingoPanel.this,
                                knowWordAction,
                                nextWordAction,
                                againAction,
                                favoriteAction,
                                settings,
                                wordPool,
                                timerResume
                        );
                    }
                }

                @Override
                public void mouseEntered(MouseEvent event) {
                    hovered = true;
                    hoverRevealed = false;
                    hoverTimer.restart();
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent event) {
                    hovered = false;
                    hoverRevealed = false;
                    hoverTimer.stop();
                    repaint();
                }
            });
        }

        private void showQuickMenu(MouseEvent event) {
            JPopupMenu menu = new JPopupMenu();
            JMenuItem favorite = new JMenuItem("Toggle favorite");
            favorite.addActionListener(ignored -> favoriteAction.accept(wordEntry));
            JMenuItem pin = new JMenuItem("Pin / unpin this word");
            pin.addActionListener(ignored -> pinAction.accept(wordEntry));
            JMenuItem snooze = new JMenuItem("Snooze WaitLingo for 30 minutes");
            snooze.addActionListener(ignored -> snoozeAction.run());
            JMenuItem next = new JMenuItem("Next word");
            next.addActionListener(ignored -> nextWordAction.run());
            JMenuItem hideToday = new JMenuItem("Do not show this word today");
            hideToday.addActionListener(ignored -> hideTodayAction.accept(wordEntry));
            menu.add(favorite);
            menu.add(pin);
            menu.add(hideToday);
            menu.addSeparator();
            menu.add(snooze);
            menu.add(next);
            menu.show(this, event.getX(), event.getY());
        }

        private Runnable timerPause = () -> {};
        private Runnable timerResume = () -> {};
        private boolean smartCompact;
        private boolean buildActive;

        private void setSmartCompact(boolean value) { smartCompact = value; }
        private void setBuildActive(boolean value) { buildActive = value; repaint(); }

        private void setTimerActions(Runnable pause, Runnable resume) {
            timerPause = pause;
            timerResume = resume;
        }

        private void setWord(WordEntry wordEntry, WaitLingoSettings.Data settings, List<WordEntry> wordPool) {
            this.wordEntry = wordEntry;
            this.settings = settings;
            this.wordPool = wordPool;
            transitionAlpha = 0f;
            if (transitionTimer != null) transitionTimer.stop();
            transitionTimer = new Timer(25, event -> {
                transitionAlpha = Math.min(1f, transitionAlpha + 0.14f);
                repaint();
                if (transitionAlpha >= 1f) ((Timer) event.getSource()).stop();
            });
            transitionTimer.start();
            float scale = settings.fontPercent / 100f;
            int textWidth = getFontMetrics(getFont().deriveFont(Font.BOLD, 12f * scale))
                    .stringWidth(wordEntry.displayText()) + 150;
            int preferredWidth = settings.compactMode || smartCompact ? 230 : Math.max(250, Math.min(420, textWidth));
            setPreferredSize(new Dimension(preferredWidth, Math.max(HEIGHT, Math.round(HEIGHT * scale))));
            revalidate();
            String example = wordEntry.getPrimaryExample();
            String safeExample = example == null ? "" : example.replace("<", "&lt;").replace(">", "&gt;");
            setToolTipText("<html><b>" + wordEntry.displayText() + "</b>"
                    + (safeExample.isBlank() ? "" : "<br>" + safeExample) + "</html>");
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);

            Graphics2D g2 = (Graphics2D) graphics.create();

            try {
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, transitionAlpha));
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                int x = 2;
                int y = 3;
                int width = getWidth() - 4;
                int height = getHeight() - 6;
                int arc = 16;

                Color backgroundTop = JBColor.namedColor(
                        "WaitLingo.Badge.Background.Top",
                        new JBColor(new Color(252, 253, 255), new Color(55, 58, 64))
                );

                Color backgroundBottom = JBColor.namedColor(
                        "WaitLingo.Badge.Background.Bottom",
                        new JBColor(new Color(239, 242, 247), new Color(43, 46, 52))
                );

                Color hoverOverlay = JBColor.namedColor(
                        "WaitLingo.Badge.Hover",
                        new JBColor(new Color(246, 248, 252), new Color(64, 68, 76))
                );

                Color border = JBColor.namedColor(
                        "WaitLingo.Badge.Border",
                        new JBColor(new Color(207, 212, 222), new Color(88, 94, 106))
                );

                Color accent = JBColor.namedColor(
                        "WaitLingo.Badge.Accent",
                        new JBColor(new Color(86, 130, 245), new Color(128, 168, 255))
                );

                Color brandText = JBColor.namedColor(
                        "WaitLingo.Badge.BrandText",
                        new JBColor(new Color(82, 88, 104), new Color(184, 190, 202))
                );

                Color wordText = JBColor.namedColor(
                        "WaitLingo.Badge.WordText",
                        new JBColor(new Color(25, 30, 40), new Color(235, 238, 244))
                );

                Color translationText = JBColor.namedColor(
                        "WaitLingo.Badge.TranslationText",
                        new JBColor(new Color(102, 110, 126), new Color(178, 184, 194))
                );

                Color separator = JBColor.namedColor(
                        "WaitLingo.Badge.Separator",
                        new JBColor(new Color(214, 218, 226), new Color(92, 98, 110))
                );

                LinearGradientPaint gradient = new LinearGradientPaint(
                        x,
                        y,
                        x,
                        y + height,
                        new float[]{0f, 1f},
                        new Color[]{hovered ? hoverOverlay : backgroundTop, backgroundBottom}
                );

                g2.setPaint(gradient);
                g2.fillRoundRect(x, y, width, height, arc, arc);

                g2.setStroke(new BasicStroke(1f));
                g2.setColor(border);
                g2.drawRoundRect(x, y, width, height, arc, arc);

                int dotSize = 7;
                int dotX = x + 10;
                int dotY = y + (height - dotSize) / 2;

                g2.setColor(accent);
                g2.fillOval(dotX, dotY, dotSize, dotSize);

                float scale = settings == null ? 1f : settings.fontPercent / 100f;
                Font brandFont = getFont().deriveFont(Font.BOLD, 11f * scale);
                Font wordFont = getFont().deriveFont(Font.BOLD, 12f * scale);
                Font translationFont = getFont().deriveFont(Font.PLAIN, 12f * scale);

                String brand = buildActive ? "WaitLingo • BUILD" : "WaitLingo";
                boolean reverse = settings != null && WaitLingoSettings.TR_TO_EN.equals(settings.direction);
                String word = wordEntry == null ? "" : (reverse ? wordEntry.getTranslation("tr") : wordEntry.getWord());
                String translation = wordEntry == null ? "" : (reverse ? "?" : wordEntry.getTranslation("tr"));
                if (settings != null && (settings.compactMode || smartCompact) && !hoverRevealed) translation = "";

                int currentX = dotX + dotSize + 8;

                g2.setFont(brandFont);
                FontMetrics brandMetrics = g2.getFontMetrics();
                int baseline = y + ((height - brandMetrics.getHeight()) / 2) + brandMetrics.getAscent();

                g2.setColor(brandText);
                g2.drawString(brand, currentX, baseline);

                currentX += brandMetrics.stringWidth(brand) + 12;

                g2.setColor(separator);
                g2.drawLine(currentX, y + 5, currentX, y + height - 5);

                currentX += 12;

                g2.setFont(wordFont);
                FontMetrics wordMetrics = g2.getFontMetrics();

                String clippedWord = clipText(word, wordMetrics, 90);
                g2.setColor(wordText);
                g2.drawString(clippedWord, currentX, baseline);

                currentX += wordMetrics.stringWidth(clippedWord) + 8;

                g2.setFont(translationFont);
                FontMetrics translationMetrics = g2.getFontMetrics();

                int maxTranslationWidth = x + width - currentX - 10;
                String clippedTranslation = clipText(translation, translationMetrics, maxTranslationWidth);

                g2.setColor(translationText);
                g2.drawString(clippedTranslation, currentX, baseline);
            } finally {
                g2.dispose();
            }
        }

        private static String clipText(String text, FontMetrics metrics, int maxWidth) {
            if (text == null || text.isBlank() || maxWidth <= 0) {
                return "";
            }

            if (metrics.stringWidth(text) <= maxWidth) {
                return text;
            }

            String ellipsis = "...";
            int ellipsisWidth = metrics.stringWidth(ellipsis);

            StringBuilder builder = new StringBuilder();

            for (int i = 0; i < text.length(); i++) {
                char next = text.charAt(i);
                int candidateWidth = metrics.stringWidth(builder.toString() + next) + ellipsisWidth;

                if (candidateWidth > maxWidth) {
                    break;
                }

                builder.append(next);
            }

            return builder + ellipsis;
        }
    }
}
