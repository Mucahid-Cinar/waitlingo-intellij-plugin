package io.github.mucahidcinar.waitlingo.ui;

import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import io.github.mucahidcinar.waitlingo.vocabulary.WordEntry;
import io.github.mucahidcinar.waitlingo.settings.WaitLingoSettings;
import io.github.mucahidcinar.waitlingo.audio.PronunciationService;
import org.jetbrains.annotations.NotNull;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public final class WordDetailsPopup {

    private static final int POPUP_WIDTH = 400;
    private static final int CONTENT_WIDTH = 360;

    private WordDetailsPopup() {
    }

    public static void show(
            WordEntry entry,
            Component owner,
            Consumer<WordEntry> knowWordAction,
            Runnable nextWordAction,
            Consumer<WordEntry> againAction,
            Consumer<WordEntry> favoriteAction,
            WaitLingoSettings.Data settings,
            List<WordEntry> wordPool,
            boolean recallMode,
            Runnable popupClosedAction
    ) {
        AtomicReference<JBPopup> popupRef = new AtomicReference<>();

        JPanel root = new RoundedPanel(
                bg(),
                border(),
                18
        );

        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(JBUI.Borders.empty(18, 20, 18, 20));

        boolean reverse = WaitLingoSettings.TR_TO_EN.equals(settings.direction);
        root.add(buildHeader(entry, favoriteAction, reverse));
        root.add(Box.createVerticalStrut(6));
        JLabel translation = buildTranslation(entry, reverse);
        if (recallMode) {
            translation.setText("Recall the meaning before revealing it.");
        }
        root.add(translation);
        JButton revealButton = new PillButton("Show meaning", true);
        if (recallMode) {
            revealButton.setAlignmentX(Component.LEFT_ALIGNMENT);
            root.add(Box.createVerticalStrut(10));
            root.add(revealButton);
        }
        root.add(Box.createVerticalStrut(16));
        if (!entry.getPrimaryExample().isBlank()) {
            root.add(buildExampleCard(entry));
            root.add(Box.createVerticalStrut(12));
        }

        JPanel tags = buildTags(entry);

        if (tags != null) {
            root.add(tags);
            root.add(Box.createVerticalStrut(16));
        }

        JPanel footer = buildFooter(popupRef, entry, knowWordAction, nextWordAction, againAction);
        footer.setVisible(!recallMode);
        root.add(footer);
        revealButton.addActionListener(event -> {
            translation.setText(reverse ? entry.getWord() : entry.getTranslation("tr"));
            revealButton.setVisible(false);
            footer.setVisible(true);
            root.revalidate();
            root.repaint();
        });

        int preferredHeight = Math.max(340, root.getPreferredSize().height);
        root.setPreferredSize(new Dimension(POPUP_WIDTH, preferredHeight));

        JBPopup popup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(root, root)
                .setRequestFocus(false)
                .setResizable(false)
                .setMovable(false)
                .setCancelOnClickOutside(true)
                .setCancelOnWindowDeactivation(true)
                .createPopup();

        popupRef.set(popup);
        popup.addListener(new JBPopupListener() {
            @Override public void onClosed(@NotNull LightweightWindowEvent event) { popupClosedAction.run(); }
        });
        popup.showUnderneathOf(owner);
    }

    private static JPanel buildHeader(WordEntry entry, Consumer<WordEntry> favoriteAction, boolean reverse) {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.setMaximumSize(new Dimension(CONTENT_WIDTH, 42));

        JLabel word = new JLabel(reverse ? entry.getTranslation("tr") : entry.getWord());
        word.setFont(word.getFont().deriveFont(Font.BOLD, 25f));
        word.setForeground(fgStrong());

        JLabel level = new PillLabel(entry.getLevel().isBlank() ? "WORD" : entry.getLevel(), true);

        header.add(word, BorderLayout.WEST);
        JButton favorite = new JButton("★");
        favorite.setToolTipText("Add/remove favorite");
        favorite.setFocusable(true);
        favorite.addActionListener(event -> favoriteAction.accept(entry));
        JButton speak = new JButton("🔊");
        speak.setToolTipText("Pronounce using offline system voice");
        speak.setFocusable(true);
        speak.addActionListener(event -> PronunciationService.speak(entry.getWord()));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        actions.setOpaque(false);
        actions.add(speak);
        actions.add(favorite);
        actions.add(level);
        header.add(actions, BorderLayout.EAST);

        return header;
    }

    private static JLabel buildTranslation(WordEntry entry, boolean reverse) {
        String value = reverse ? entry.getWord() : entry.getTranslation("tr");
        JLabel translation = new JLabel(value);
        translation.setAlignmentX(Component.LEFT_ALIGNMENT);
        translation.setFont(translation.getFont().deriveFont(Font.PLAIN, 15f));
        translation.setForeground(fgMuted());
        translation.setMaximumSize(new Dimension(CONTENT_WIDTH, 24));
        return translation;
    }

    private static JPanel buildQuiz(WordEntry entry, List<WordEntry> pool, boolean reverse,
                                    Consumer<WordEntry> correctAction, Consumer<WordEntry> wrongAction,
                                    AtomicReference<JBPopup> popupRef) {
        JPanel quiz = new RoundedPanel(cardBg(), border(), 14);
        quiz.setLayout(new BoxLayout(quiz, BoxLayout.Y_AXIS));
        quiz.setBorder(JBUI.Borders.empty(10, 12));
        quiz.setAlignmentX(Component.LEFT_ALIGNMENT);
        quiz.setMaximumSize(new Dimension(CONTENT_WIDTH, Integer.MAX_VALUE));
        quiz.add(sectionTitle("QUICK QUIZ — choose the meaning"));
        quiz.add(Box.createVerticalStrut(6));
        List<WordEntry> choices = new ArrayList<>();
        choices.add(entry);
        for (WordEntry candidate : pool) {
            if (choices.size() >= 4) break;
            if (!candidate.id().equals(entry.id())) choices.add(candidate);
        }
        Collections.shuffle(choices);
        for (WordEntry choice : choices) {
            String answer = reverse ? choice.getWord() : choice.getTranslation("tr");
            JButton button = new JButton(answer);
            button.setFocusable(true);
            button.setAlignmentX(Component.LEFT_ALIGNMENT);
            button.addActionListener(event -> {
                quiz.setEnabled(false);
                if (choice.id().equals(entry.id())) {
                    button.setText("✓ " + answer);
                    correctAction.accept(entry);
                } else {
                    button.setText("✕ " + answer);
                    wrongAction.accept(entry);
                }
                JBPopup popup = popupRef.get();
                if (popup != null) popup.cancel();
            });
            quiz.add(button);
            quiz.add(Box.createVerticalStrut(4));
        }
        return quiz;
    }

    private static JPanel buildExampleCard(WordEntry entry) {
        JPanel card = new RoundedPanel(
                cardBg(),
                border(),
                16
        );

        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(JBUI.Borders.empty(14, 16));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(CONTENT_WIDTH, Integer.MAX_VALUE));

        JLabel title = sectionTitle(entry.getTags().contains("personal") ? "From your code" : "Example sentence");

        JTextArea primary = textArea(
                "“" + entry.getPrimaryExample() + "”",
                14f,
                fgStrong()
        );

        card.add(title);
        card.add(Box.createVerticalStrut(8));
        card.add(primary);

        List<String> examples = entry.getExamples();

        if (examples != null && examples.size() > 1) {
            card.add(Box.createVerticalStrut(14));
            card.add(sectionTitle("Also useful"));
            card.add(Box.createVerticalStrut(7));
            card.add(textArea("• " + examples.get(1), 13f, fgMuted()));
        }

        return card;
    }

    private static JPanel buildTags(WordEntry entry) {
        JPanel tags = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        tags.setOpaque(false);
        tags.setAlignmentX(Component.LEFT_ALIGNMENT);
        tags.setMaximumSize(new Dimension(CONTENT_WIDTH, 30));

        if (!entry.getCategory().isBlank()) {
            tags.add(new PillLabel(entry.getCategory(), false));
        }

        for (String tag : entry.getTags()) {
            tags.add(new PillLabel(tag, false));
        }

        return tags.getComponentCount() == 0 ? null : tags;
    }

    private static JPanel buildFooter(
            AtomicReference<JBPopup> popupRef,
            WordEntry entry,
            Consumer<WordEntry> knowWordAction,
            Runnable nextWordAction,
            Consumer<WordEntry> againAction
    ) {
        JPanel footer = new JPanel();
        footer.setOpaque(false);
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
        footer.setAlignmentX(Component.LEFT_ALIGNMENT);
        footer.setMaximumSize(new Dimension(CONTENT_WIDTH, 74));

        JLabel hint = new JLabel("Keep learning without leaving your flow.");
        hint.setFont(hint.getFont().deriveFont(Font.PLAIN, 11f));
        hint.setForeground(fgSubtle());
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel buttonsRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonsRow.setOpaque(false);
        buttonsRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttonsRow.setMaximumSize(new Dimension(CONTENT_WIDTH, 38));

        JButton againButton = new PillButton("Again soon", false);
        JButton nextButton = new PillButton("Next", false);
        JButton knowButton = new PillButton("Got it", true);

        againButton.addActionListener(event -> {
            JBPopup popup = popupRef.get();
            if (popup != null) popup.cancel();
            againAction.accept(entry);
        });

        knowButton.addActionListener(event -> {
            JBPopup popup = popupRef.get();
            if (popup != null) {
                popup.cancel();
            }
            knowWordAction.accept(entry);
        });

        nextButton.addActionListener(event -> {
            JBPopup popup = popupRef.get();
            if (popup != null) {
                popup.cancel();
            }
            nextWordAction.run();
        });

        buttonsRow.add(againButton);
        buttonsRow.add(nextButton);
        buttonsRow.add(knowButton);

        footer.add(hint);
        footer.add(Box.createVerticalStrut(10));
        footer.add(buttonsRow);

        return footer;
    }

    private static JLabel sectionTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 11f));
        label.setForeground(fgSubtle());
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private static JTextArea textArea(String text, float size, Color color) {
        JTextArea area = new JTextArea(text);
        area.setEditable(false);
        area.setFocusable(false);
        area.setOpaque(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(new JLabel().getFont().deriveFont(Font.PLAIN, size));
        area.setForeground(color);
        area.setBorder(BorderFactory.createEmptyBorder());
        area.setColumns(38);
        area.setAlignmentX(Component.LEFT_ALIGNMENT);
        area.setMaximumSize(new Dimension(CONTENT_WIDTH - 32, Integer.MAX_VALUE));
        return area;
    }

    private static Color bg() {
        return JBColor.namedColor(
                "WaitLingo.Popup.Background",
                new JBColor(new Color(250, 251, 253), new Color(38, 41, 48))
        );
    }

    private static Color cardBg() {
        return JBColor.namedColor(
                "WaitLingo.Popup.Card.Background",
                new JBColor(new Color(244, 247, 252), new Color(49, 53, 62))
        );
    }

    private static Color border() {
        return JBColor.namedColor(
                "WaitLingo.Popup.Border",
                new JBColor(new Color(223, 228, 238), new Color(72, 78, 92))
        );
    }

    private static Color fgStrong() {
        return JBColor.namedColor(
                "WaitLingo.Popup.Text.Strong",
                new JBColor(new Color(22, 27, 38), new Color(244, 246, 250))
        );
    }

    private static Color fgMuted() {
        return JBColor.namedColor(
                "WaitLingo.Popup.Text.Muted",
                new JBColor(new Color(78, 87, 105), new Color(190, 198, 212))
        );
    }

    private static Color fgSubtle() {
        return JBColor.namedColor(
                "WaitLingo.Popup.Text.Subtle",
                new JBColor(new Color(112, 122, 142), new Color(145, 154, 172))
        );
    }

    private static Color chipBg() {
        return JBColor.namedColor(
                "WaitLingo.Popup.Chip.Background",
                new JBColor(new Color(236, 240, 248), new Color(58, 64, 76))
        );
    }

    private static Color chipFg() {
        return JBColor.namedColor(
                "WaitLingo.Popup.Chip.Text",
                new JBColor(new Color(78, 92, 126), new Color(180, 190, 212))
        );
    }

    private static Color chipPrimaryBg() {
        return JBColor.namedColor(
                "WaitLingo.Popup.Chip.Primary.Background",
                new JBColor(new Color(229, 236, 255), new Color(55, 65, 98))
        );
    }

    private static Color chipPrimaryFg() {
        return JBColor.namedColor(
                "WaitLingo.Popup.Chip.Primary.Text",
                new JBColor(new Color(52, 82, 178), new Color(196, 212, 255))
        );
    }

    private static Color buttonPrimaryBg() {
        return JBColor.namedColor(
                "WaitLingo.Popup.Button.Primary.Background",
                new JBColor(new Color(58, 102, 230), new Color(90, 126, 235))
        );
    }

    private static Color buttonPrimaryHoverBg() {
        return JBColor.namedColor(
                "WaitLingo.Popup.Button.Primary.HoverBackground",
                new JBColor(new Color(45, 88, 212), new Color(105, 140, 248))
        );
    }

    private static Color buttonSecondaryBg() {
        return JBColor.namedColor(
                "WaitLingo.Popup.Button.Secondary.Background",
                new JBColor(new Color(239, 242, 248), new Color(58, 62, 70))
        );
    }

    private static Color buttonSecondaryHoverBg() {
        return JBColor.namedColor(
                "WaitLingo.Popup.Button.Secondary.HoverBackground",
                new JBColor(new Color(230, 235, 245), new Color(70, 75, 84))
        );
    }

    private static Color buttonSecondaryBorder() {
        return JBColor.namedColor(
                "WaitLingo.Popup.Button.Secondary.Border",
                new JBColor(new Color(212, 218, 230), new Color(84, 90, 102))
        );
    }

    private static Color buttonPrimaryText() {
        return JBColor.namedColor(
                "WaitLingo.Popup.Button.Primary.Text",
                new JBColor(Color.WHITE, Color.WHITE)
        );
    }

    private static Color buttonSecondaryText() {
        return JBColor.namedColor(
                "WaitLingo.Popup.Button.Secondary.Text",
                new JBColor(new Color(70, 78, 96), new Color(215, 220, 230))
        );
    }

    private static final class PillLabel extends JLabel {

        private final boolean primary;

        private PillLabel(String text, boolean primary) {
            super(text);
            this.primary = primary;
            setOpaque(false);
            setBorder(JBUI.Borders.empty(4, 9));
            setFont(getFont().deriveFont(Font.BOLD, primary ? 11f : 10.5f));
            setForeground(primary ? chipPrimaryFg() : chipFg());
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();

            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(primary ? chipPrimaryBg() : chipBg());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
            } finally {
                g2.dispose();
            }

            super.paintComponent(graphics);
        }
    }

    private static final class PillButton extends JButton {

        private final boolean primary;

        private PillButton(String text, boolean primary) {
            super(text);
            this.primary = primary;

            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setFocusable(true);
            setRolloverEnabled(true);
            setFont(getFont().deriveFont(primary ? Font.BOLD : Font.PLAIN, 12f));
            setForeground(primary ? buttonPrimaryText() : buttonSecondaryText());
            setBorder(JBUI.Borders.empty(7, 16));
            setPreferredSize(new Dimension(primary ? 96 : 104, 34));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();

            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                boolean hover = getModel().isRollover();
                boolean pressed = getModel().isPressed();

                Color background;

                if (primary) {
                    background = hover || pressed ? buttonPrimaryHoverBg() : buttonPrimaryBg();
                } else {
                    background = hover || pressed ? buttonSecondaryHoverBg() : buttonSecondaryBg();
                }

                g2.setColor(background);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);

                if (!primary) {
                    g2.setColor(buttonSecondaryBorder());
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
                }
            } finally {
                g2.dispose();
            }

            super.paintComponent(graphics);
        }
    }

    private static final class RoundedPanel extends JPanel {

        private final Color background;
        private final Color border;
        private final int arc;

        private RoundedPanel(Color background, Color border, int arc) {
            this.background = background;
            this.border = border;
            this.arc = arc;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();

            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int width = getWidth() - 1;
                int height = getHeight() - 1;

                g2.setColor(background);
                g2.fillRoundRect(0, 0, width, height, arc, arc);

                g2.setColor(border);
                g2.drawRoundRect(0, 0, width, height, arc, arc);
            } finally {
                g2.dispose();
            }

            super.paintComponent(graphics);
        }
    }
}
