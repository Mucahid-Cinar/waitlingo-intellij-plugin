package io.github.mucahidcinar.waitlingo;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.wm.CustomStatusBarWidget;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.ui.JBColor;
import io.github.mucahidcinar.waitlingo.ui.WordDetailsPopup;
import io.github.mucahidcinar.waitlingo.vocabulary.VocabularyService;
import io.github.mucahidcinar.waitlingo.vocabulary.WordEntry;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.BasicStroke;
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

public class WaitLingoWidget implements CustomStatusBarWidget, Disposable {

    private static final int WORD_ROTATION_DELAY_MS = 12000;

    private final WaitLingoPanel component;
    private final Timer timer;

    private int wordIndex = 0;

    private final List<WordEntry> words = VocabularyService.loadDefaultWords();

    public WaitLingoWidget() {
        this.component = new WaitLingoPanel(this::markCurrentWordAsKnown, this::showNextWord);
        this.component.setWord(words.get(wordIndex));

        this.timer = new Timer(WORD_ROTATION_DELAY_MS, event -> showNextWord());
        this.timer.start();
    }

    private void showNextWord() {
        wordIndex = (wordIndex + 1) % words.size();
        component.setWord(words.get(wordIndex));
    }

    private void markCurrentWordAsKnown() {
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

        private final Runnable knowWordAction;
        private final Runnable nextWordAction;

        private WordEntry wordEntry;
        private boolean hovered;

        private WaitLingoPanel(Runnable knowWordAction, Runnable nextWordAction) {
            this.knowWordAction = knowWordAction;
            this.nextWordAction = nextWordAction;

            setOpaque(false);
            setPreferredSize(new Dimension(WIDTH, HEIGHT));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText("Click to learn this word.");

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent event) {
                    if (wordEntry != null) {
                        WordDetailsPopup.show(
                                wordEntry,
                                WaitLingoPanel.this,
                                knowWordAction,
                                nextWordAction
                        );
                    }
                }

                @Override
                public void mouseEntered(MouseEvent event) {
                    hovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent event) {
                    hovered = false;
                    repaint();
                }
            });
        }

        private void setWord(WordEntry wordEntry) {
            this.wordEntry = wordEntry;
            setToolTipText("Click to learn: " + wordEntry.displayText());
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);

            Graphics2D g2 = (Graphics2D) graphics.create();

            try {
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

                Font brandFont = getFont().deriveFont(Font.BOLD, 11f);
                Font wordFont = getFont().deriveFont(Font.BOLD, 12f);
                Font translationFont = getFont().deriveFont(Font.PLAIN, 12f);

                String brand = "WaitLingo";
                String word = wordEntry == null ? "" : wordEntry.getWord();
                String translation = wordEntry == null ? "" : wordEntry.getTranslation();

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