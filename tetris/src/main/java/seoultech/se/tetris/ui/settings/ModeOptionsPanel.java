package seoultech.se.tetris.ui.settings;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicComboBoxUI;
import seoultech.se.tetris.config.AppSettings.ScreenSize;
import seoultech.se.tetris.engine.Difficulty;
import seoultech.se.tetris.ui.DifficultyLabels;

/**
 * 1인 모드 또는 대전 모드에서 화면 크기, 난이도, 색맹 모드를 설정하는 공통 컴포넌트.
 */
final class ModeOptionsPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final Color BACKGROUND = Color.BLACK;
    private static final Color SURFACE = new Color(25, 25, 25);
    private static final Color OUTLINE = new Color(75, 75, 75);
    private static final Color MUTED = new Color(170, 170, 170);
    private static final Font BODY_FONT = new Font(Font.DIALOG, Font.PLAIN, 15);
    private static final int INNER_WIDTH = 432;

    private final boolean allowsDifficulty;
    private final JComboBox<ScreenSize> screenSize = new JComboBox<>(ScreenSize.values());
    private final JComboBox<Difficulty> difficulty = new JComboBox<>(Difficulty.values());
    private final JCheckBox colorBlind = new JCheckBox("색맹 모드");

    ModeOptionsPanel(boolean allowsDifficulty) {
        this.allowsDifficulty = allowsDifficulty;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(BACKGROUND);
        setAlignmentX(Component.LEFT_ALIGNMENT);

        Dimension optionsSize = new Dimension(INNER_WIDTH, allowsDifficulty ? 105 : 70);
        JPanel options = new JPanel(new GridLayout(allowsDifficulty ? 3 : 2, 2, 12, 10));
        options.setBackground(BACKGROUND);
        options.setAlignmentX(Component.LEFT_ALIGNMENT);
        options.setPreferredSize(optionsSize);
        options.setMaximumSize(optionsSize);

        options.add(SettingsPanel.label("화면 크기"));
        styleCombo(screenSize);
        options.add(screenSize);

        if (allowsDifficulty) {
            options.add(SettingsPanel.label("난이도"));
            styleCombo(difficulty);
            difficulty.setRenderer(createDifficultyRenderer());
            options.add(difficulty);
        }

        options.add(SettingsPanel.label("블록 표시"));
        styleCheckBox(colorBlind);
        options.add(colorBlind);
        add(options);

        add(Box.createVerticalStrut(6));
        JLabel sizeHint = SettingsPanel.label("화면 크기와 색맹 모드는 게임 화면에 적용됩니다.");
        sizeHint.setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
        sizeHint.setForeground(MUTED);
        sizeHint.setAlignmentX(Component.LEFT_ALIGNMENT);
        sizeHint.setPreferredSize(new Dimension(INNER_WIDTH, 18));
        sizeHint.setMaximumSize(new Dimension(INNER_WIDTH, 18));
        add(sizeHint);
    }

    ScreenSize getScreenSize() {
        return (ScreenSize) screenSize.getSelectedItem();
    }

    void setScreenSize(ScreenSize size) {
        screenSize.setSelectedItem(size);
    }

    Difficulty getDifficulty() {
        return (Difficulty) difficulty.getSelectedItem();
    }

    void setDifficulty(Difficulty diff) {
        difficulty.setSelectedItem(diff);
    }

    boolean isColorBlind() {
        return colorBlind.isSelected();
    }

    void setColorBlind(boolean enabled) {
        colorBlind.setSelected(enabled);
    }

    JComboBox<ScreenSize> getScreenSizeComboBox() {
        return screenSize;
    }

    JComboBox<Difficulty> getDifficultyComboBox() {
        return difficulty;
    }

    JCheckBox getColorBlindCheckBox() {
        return colorBlind;
    }

    List<List<JComponent>> navigationRows() {
        List<List<JComponent>> rows = new ArrayList<>();
        rows.add(List.of(screenSize));
        if (allowsDifficulty) {
            rows.add(List.of(difficulty));
        }
        rows.add(List.of(colorBlind));
        return rows;
    }

    private static DefaultListCellRenderer createDifficultyRenderer() {
        return new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(javax.swing.JList<?> list,
                    Object value, int index, boolean selected, boolean focused) {
                super.getListCellRendererComponent(list, value, index, selected, focused);
                if (value instanceof Difficulty selectedDifficulty) {
                    setText(DifficultyLabels.of(selectedDifficulty));
                }
                setOpaque(true);
                setBackground(index >= 0 && selected ? Color.CYAN : SURFACE);
                setForeground(index >= 0 && selected ? BACKGROUND : Color.WHITE);
                return this;
            }
        };
    }

    private static void styleCheckBox(JCheckBox box) {
        box.setFont(BODY_FONT);
        box.setBackground(BACKGROUND);
        box.setForeground(Color.WHITE);
        box.setFocusPainted(true);
    }

    private static void styleCombo(JComboBox<?> combo) {
        var originalBorder = combo.getBorder();
        combo.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent event) {
                combo.setBorder(BorderFactory.createLineBorder(Color.YELLOW));
            }

            @Override
            public void focusLost(FocusEvent event) {
                combo.setBorder(originalBorder);
            }
        });
        combo.setUI(new BasicComboBoxUI() {
            @Override
            public void paintCurrentValueBackground(Graphics graphics, Rectangle bounds, boolean focused) {
                graphics.setColor(SURFACE);
                graphics.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            }

            @Override
            protected JButton createArrowButton() {
                JButton arrow = new JButton("▼");
                arrow.setUI(new BasicButtonUI());
                arrow.setFont(new Font(Font.DIALOG, Font.PLAIN, 11));
                arrow.setBackground(SURFACE);
                arrow.setForeground(Color.CYAN);
                arrow.setBorder(BorderFactory.createLineBorder(OUTLINE));
                return arrow;
            }
        });
        combo.setFont(BODY_FONT);
        combo.setBackground(SURFACE);
        combo.setForeground(Color.WHITE);
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(javax.swing.JList<?> list,
                    Object value, int index, boolean selected, boolean focused) {
                super.getListCellRendererComponent(list, value, index, selected, focused);
                setOpaque(true);
                setBackground(index < 0 ? SURFACE : selected ? Color.CYAN : SURFACE);
                setForeground(index >= 0 && selected ? BACKGROUND : Color.WHITE);
                return this;
            }
        });
    }
}
