package seoultech.se.tetris.ui.settings;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;
import java.util.Objects;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.KeyStroke;
import javax.swing.BoxLayout;
import javax.swing.Box;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicComboBoxUI;
import seoultech.se.tetris.config.AppSettings;
import seoultech.se.tetris.config.AppSettings.ScreenSize;
import seoultech.se.tetris.config.KeyBindingEditor;
import seoultech.se.tetris.config.PropertiesAppSettingsService;
import seoultech.se.tetris.config.PropertiesKeyConfigService;
import seoultech.se.tetris.engine.Difficulty;
import seoultech.se.tetris.score.ScoreRepository;
import seoultech.se.tetris.ui.DifficultyLabels;

/** 시작 메뉴와 일시정지 화면에서 재사용할 수 있는 설정 화면. */
public final class SettingsPanel extends JPanel {
    /**
     * 설정 화면을 연 위치.
     *
     * <p>난이도와 스코어보드 기록 초기화는 시작 메뉴에서만 바꿀 수 있다.
     * 게임 중에는 진행 중인 판의 규칙과 기록이 어긋나지 않도록 두 항목을 보여주지 않는다.
     */
    public enum Context {
        /** 시작 메뉴에서 연 설정 */
        START_MENU(true),
        /** 게임 중 일시정지 메뉴에서 연 설정 */
        IN_GAME(false);

        private final boolean fullAccess;

        Context(boolean fullAccess) {
            this.fullAccess = fullAccess;
        }

        boolean allowsDifficulty() {
            return fullAccess;
        }

        boolean allowsScoreReset() {
            return fullAccess;
        }
    }

    private static final long serialVersionUID = 1L;
    private static final Color BACKGROUND = Color.BLACK;
    private static final Color SURFACE = new Color(25, 25, 25);
    private static final Color OUTLINE = new Color(75, 75, 75);
    private static final Color MUTED = new Color(170, 170, 170);
    private static final Font BODY_FONT = new Font(Font.DIALOG, Font.PLAIN, 15);

    private final PropertiesAppSettingsService settings;
    private final KeyBindingEditor keyEditor;
    private final KeyBindingsPanel keyBindings;
    private final ScoreRepository scores;
    private final Context context;
    private final Runnable onClose;
    private final transient KeyEventDispatcher keyboardDispatcher = this::dispatchKeyboard;
    private final List<List<JComponent>> navigationRows = new ArrayList<>();
    private final JComboBox<ScreenSize> screenSize = new JComboBox<>(ScreenSize.values());
    private final JComboBox<Difficulty> difficulty = new JComboBox<>(Difficulty.values());
    private final JCheckBox colorBlind = new JCheckBox("색맹 모드");

    public SettingsPanel(PropertiesAppSettingsService settings,
            PropertiesKeyConfigService keys, ScoreRepository scores,
            Context context, Runnable onKeysSaved, Runnable onClose) {
        super(new BorderLayout());
        this.settings = Objects.requireNonNull(settings, "settings");
        this.keyEditor = new KeyBindingEditor(Objects.requireNonNull(keys, "keys"),
                Objects.requireNonNull(onKeysSaved, "onKeysSaved"));
        this.keyBindings = new KeyBindingsPanel(keyEditor);
        this.scores = Objects.requireNonNull(scores, "scores");
        this.context = Objects.requireNonNull(context, "context");
        this.onClose = Objects.requireNonNull(onClose, "onClose");
        buildContent();
        loadCurrentValues();
        for (List<JComponent> row : navigationRows) {
            for (JComponent control : row) {
                control.addFocusListener(new FocusAdapter() {
                    @Override
                    public void focusGained(FocusEvent event) {
                        control.scrollRectToVisible(new Rectangle(control.getSize()));
                    }
                });
            }
        }
    }

    @Override
    public void addNotify() {
        super.addNotify();
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(keyboardDispatcher);
        SwingUtilities.invokeLater(() -> {
            if (isShowing()) {
                screenSize.requestFocusInWindow();
            }
        });
    }

    @Override
    public void removeNotify() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(keyboardDispatcher);
        super.removeNotify();
    }

    private boolean dispatchKeyboard(KeyEvent event) {
        if (event.getID() != KeyEvent.KEY_PRESSED
                || keyBindings.isCapturingKey() || !isShowing()
                || !SwingUtilities.isDescendingFrom(event.getComponent(), this)) {
            return false;
        }
        Component focused = event.getComponent();
        // 펼친 선택 목록에서는 Swing의 방향키 선택과 Esc 닫기를 사용한다.
        if (focused instanceof JComboBox<?> combo && combo.isPopupVisible()) {
            return false;
        }
        Component target = navigationTarget(focused, event.getKeyCode());
        if (target != null) {
            target.requestFocusInWindow();
            event.consume();
            return true;
        }
        if (event.getKeyCode() == KeyEvent.VK_ENTER) {
            if (focused instanceof JComboBox<?> combo) {
                combo.showPopup();
            } else if (focused instanceof AbstractButton button) {
                button.doClick();
            } else {
                return false;
            }
            event.consume();
            return true;
        }
        if (event.getKeyCode() != KeyEvent.VK_ESCAPE) {
            return false;
        }
        event.consume();
        String[] options = {"예", "아니오", "취소"};
        JOptionPane prompt = new JOptionPane("설정을 저장하시겠습니까?", JOptionPane.QUESTION_MESSAGE,
                JOptionPane.YES_NO_CANCEL_OPTION, null, options, options[0]);
        // 확인창의 버튼도 방향키로 이동하고 Enter로 현재 버튼을 선택한다.
        for (int key : new int[] {KeyEvent.VK_LEFT, KeyEvent.VK_UP, KeyEvent.VK_RIGHT,
                KeyEvent.VK_DOWN, KeyEvent.VK_ENTER}) {
            String action = "dialog-navigation-" + key;
            prompt.getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(key, 0), action);
            prompt.getActionMap().put(action, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
                    if (key == KeyEvent.VK_ENTER) {
                        if (manager.getFocusOwner() instanceof AbstractButton button) {
                            button.doClick();
                        }
                    } else if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_UP) {
                        manager.focusPreviousComponent();
                    } else {
                        manager.focusNextComponent();
                    }
                }
            });
        }
        JDialog dialog = prompt.createDialog(this, "설정 나가기");
        try {
            dialog.setVisible(true);
        } finally {
            dialog.dispose();
        }
        for (int choice = 0; choice < options.length; choice++) {
            if (options[choice].equals(prompt.getValue())) {
                handleEscapeChoice(choice);
                break;
            }
        }
        return true;
    }

    /** 화면의 행과 열을 기준으로 방향키 이동 대상을 찾는다. */
    Component navigationTarget(Component current, int keyCode) {
        for (int row = 0; row < navigationRows.size(); row++) {
            int column = navigationRows.get(row).indexOf(current);
            if (column < 0) {
                continue;
            }
            int nextRow = row;
            int nextColumn = column;
            switch (keyCode) {
                case KeyEvent.VK_UP -> nextRow = Math.max(0, row - 1);
                case KeyEvent.VK_DOWN -> nextRow = Math.min(navigationRows.size() - 1, row + 1);
                case KeyEvent.VK_LEFT -> nextColumn = Math.max(0, column - 1);
                case KeyEvent.VK_RIGHT -> nextColumn = Math.min(navigationRows.get(row).size() - 1, column + 1);
                default -> { return null; }
            }
            List<JComponent> destination = navigationRows.get(nextRow);
            return destination.get(Math.min(nextColumn, destination.size() - 1));
        }
        return null;
    }

    void handleEscapeChoice(int choice) {
        if (choice == JOptionPane.YES_OPTION) {
            saveSettings();
        } else if (choice == JOptionPane.NO_OPTION) {
            close();
        }
    }

    private void buildContent() {
        setBackground(BACKGROUND);
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(BACKGROUND);
        content.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        JLabel title = new JLabel("설정");
        title.setFont(new Font(Font.DIALOG, Font.BOLD, 26));
        title.setForeground(Color.CYAN);
        content.add(title);
        content.add(Box.createVerticalStrut(18));

        JPanel options = new JPanel(new GridLayout(context.allowsDifficulty() ? 3 : 2, 2, 12, 12));
        options.setBackground(BACKGROUND);
        options.add(label("화면 크기"));
        styleCombo(screenSize);
        options.add(screenSize);
        if (context.allowsDifficulty()) {
            options.add(label("난이도"));
            styleCombo(difficulty);
            difficulty.setRenderer(new DefaultListCellRenderer() {
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
            });
            options.add(difficulty);
        }
        options.add(label("블록 표시"));
        colorBlind.setFont(BODY_FONT);
        colorBlind.setBackground(BACKGROUND);
        colorBlind.setForeground(Color.WHITE);
        colorBlind.setFocusPainted(true);
        options.add(colorBlind);
        content.add(options);
        navigationRows.add(List.of(screenSize));
        if (context.allowsDifficulty()) {
            navigationRows.add(List.of(difficulty));
        }
        navigationRows.add(List.of(colorBlind));
        content.add(Box.createVerticalStrut(8));
        JLabel sizeHint = label("화면 크기와 색맹 모드는 게임 화면에 적용됩니다.");
        sizeHint.setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
        sizeHint.setForeground(MUTED);
        content.add(sizeHint);
        content.add(Box.createVerticalStrut(24));
        JLabel section = label("조작 키");
        section.setForeground(Color.CYAN);
        section.setFont(new Font(Font.DIALOG, Font.BOLD, 18));
        content.add(section);
        content.add(Box.createVerticalStrut(4));
        JLabel hint = label("키 칸 선택 후 입력  ·  Esc로 해제");
        hint.setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
        hint.setForeground(MUTED);
        content.add(hint);
        content.add(Box.createVerticalStrut(12));
        content.add(keyBindings);
        navigationRows.addAll(keyBindings.navigationRows());
        JLabel navigationHint = label("↑ ↓ 항목 이동 · ← → 키 칸/버튼 이동 · Enter 선택");
        navigationHint.setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
        content.add(navigationHint);

        JPanel actions = new JPanel();
        actions.setBackground(BACKGROUND);
        actions.setBorder(BorderFactory.createEmptyBorder(12, 12, 16, 12));
        JButton reset = new JButton("모든 설정 기본값");
        styleButton(reset, false);
        reset.addActionListener(event -> restoreDefaults());
        actions.add(reset);
        if (context.allowsScoreReset()) {
            JButton clearScores = new JButton("스코어보드 기록 초기화");
            styleButton(clearScores, false);
            clearScores.addActionListener(event -> clearScores());
            actions.add(clearScores);
        }
        JButton save = new JButton("저장");
        styleButton(save, true);
        save.addActionListener(event -> saveSettings());
        actions.add(save);
        JButton cancel = new JButton("취소");
        styleButton(cancel, false);
        cancel.addActionListener(event -> close());
        actions.add(cancel);
        List<JComponent> actionRow = new ArrayList<>();
        for (Component component : actions.getComponents()) {
            actionRow.add((JComponent) component);
        }
        navigationRows.add(List.copyOf(actionRow));

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BACKGROUND);
        add(scroll, BorderLayout.CENTER);
        add(actions, BorderLayout.SOUTH);
    }

    static JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setFont(BODY_FONT);
        label.setForeground(Color.WHITE);
        return label;
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

    static void styleButton(JButton button, boolean accent) {
        button.setUI(new BasicButtonUI());
        button.setFont(BODY_FONT);
        button.setBackground(accent ? Color.CYAN : SURFACE);
        button.setForeground(accent ? BACKGROUND : Color.WHITE);
        button.setFocusPainted(false);
        setButtonBorder(button, accent ? Color.CYAN : OUTLINE);
        button.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent event) {
                setButtonBorder(button, Color.YELLOW);
            }

            @Override
            public void focusLost(FocusEvent event) {
                setButtonBorder(button, accent ? Color.CYAN : OUTLINE);
            }
        });
    }

    private static void setButtonBorder(JButton button, Color color) {
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color),
                BorderFactory.createEmptyBorder(7, 12, 7, 12)));
    }

    private void close() {
        keyBindings.cancelKeyCapture();
        onClose.run();
    }

    private void loadCurrentValues() {
        AppSettings current = settings.current();
        screenSize.setSelectedItem(current.screenSize());
        difficulty.setSelectedItem(current.difficulty());
        colorBlind.setSelected(current.colorBlind());
        keyBindings.refreshKeys();
    }

    private void restoreDefaults() {
        keyBindings.cancelKeyCapture();
        screenSize.setSelectedItem(AppSettings.DEFAULT.screenSize());
        if (context.allowsDifficulty()) {
            difficulty.setSelectedItem(AppSettings.DEFAULT.difficulty());
        }
        colorBlind.setSelected(AppSettings.DEFAULT.colorBlind());
        keyEditor.restoreDefaults();
        keyBindings.refreshKeys();
    }

    private void saveSettings() {
        keyBindings.cancelKeyCapture();
        if (!keyEditor.canSave()) {
            JOptionPane.showMessageDialog(this, "모든 동작에 메인 키가 필요합니다.", "설정", JOptionPane.WARNING_MESSAGE);
            keyBindings.focusMissingMainKey();
            return;
        }
        try {
            keyEditor.save();
            // 게임 중에는 난이도를 보여주지 않으므로 저장된 값을 그대로 유지한다.
            Difficulty savedDifficulty = context.allowsDifficulty()
                    ? (Difficulty) difficulty.getSelectedItem()
                    : settings.current().difficulty();
            settings.save(new AppSettings((ScreenSize) screenSize.getSelectedItem(),
                    colorBlind.isSelected(), savedDifficulty));
            close();
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this,
                    e.getMessage() + "\n키 설정과 다른 설정은 별도 파일이므로 일부가 저장되었을 수 있습니다.",
                    "설정 저장 실패", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearScores() {
        keyBindings.cancelKeyCapture();
        if (!context.allowsScoreReset()) {
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this, "모든 모드의 점수 기록을 삭제하시겠습니까?",
                "스코어보드 초기화", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            try {
                scores.clearScores();
                JOptionPane.showMessageDialog(this, "점수 기록을 초기화했습니다.");
            } catch (RuntimeException e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "초기화 실패", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
