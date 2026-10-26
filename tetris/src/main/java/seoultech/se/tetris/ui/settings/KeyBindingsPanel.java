package seoultech.se.tetris.ui.settings;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.List;
import java.util.ArrayList;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import seoultech.se.tetris.config.KeyBindingEditor;
import seoultech.se.tetris.config.KeyConfigService.GameAction;

/** 조작 키 목록과 키 입력 대기 상태를 관리한다. */
final class KeyBindingsPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private final KeyBindingEditor editor;
    private final Map<GameAction, JButton[]> keyButtons = new EnumMap<>(GameAction.class);
    private final KeyEventDispatcher keyCapture = this::captureKey;
    private GameAction captureAction;
    private boolean captureMain;
    private int capturedCode = KeyEvent.VK_UNDEFINED;

    KeyBindingsPanel(KeyBindingEditor editor) {
        super(new GridLayout(GameAction.values().length, 3, 10, 8));
        this.editor = Objects.requireNonNull(editor, "editor");
        setBackground(Color.BLACK);
        add(SettingsPanel.label("동작"));
        add(SettingsPanel.label("기본 키"));
        add(SettingsPanel.label("보조 키"));
        for (GameAction action : GameAction.values()) {
            if (action == GameAction.PAUSE_MENU) {
                continue;
            }
            add(SettingsPanel.label(actionLabel(action)));
            JButton main = keyButton(action, true);
            JButton sub = keyButton(action, false);
            keyButtons.put(action, new JButton[] {main, sub});
            add(main);
            add(sub);
        }
        refreshKeys();
    }

    private JButton keyButton(GameAction action, boolean main) {
        JButton button = new JButton();
        SettingsPanel.styleButton(button, false);
        button.setToolTipText(actionLabel(action) + (main ? " 기본 키 변경" : " 보조 키 변경"));
        button.setPreferredSize(new Dimension(110, button.getPreferredSize().height));
        button.addActionListener(event -> beginKeyCapture(action, main, button));
        return button;
    }

    private void beginKeyCapture(GameAction action, boolean main, JButton button) {
        if (captureAction == null) {
            KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(keyCapture);
        }
        captureAction = action;
        captureMain = main;
        capturedCode = KeyEvent.VK_UNDEFINED;
        refreshKeys();
        button.setText("입력 대기…");
        button.setForeground(Color.YELLOW);
        button.requestFocusInWindow();
    }

    /** 키 입력 대기 동안 버튼의 기본 키 동작을 막고 편집기에만 전달한다. */
    private boolean captureKey(KeyEvent event) {
        if (captureAction == null) {
            return false;
        }
        if (event.getID() == KeyEvent.KEY_PRESSED && capturedCode == KeyEvent.VK_UNDEFINED) {
            capturedCode = event.getKeyCode();
            if (captureMain) {
                editor.captureMainKey(captureAction, capturedCode);
            } else {
                editor.captureSubKey(captureAction, capturedCode);
            }
            refreshKeys();
        } else if (event.getID() == KeyEvent.KEY_RELEASED
                && capturedCode == event.getKeyCode()) {
            endKeyCapture();
        }
        event.consume();
        return true;
    }

    private void endKeyCapture() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(keyCapture);
        captureAction = null;
        capturedCode = KeyEvent.VK_UNDEFINED;
    }

    void cancelKeyCapture() {
        if (captureAction != null) {
            endKeyCapture();
            refreshKeys();
        }
    }

    boolean isCapturingKey() {
        return captureAction != null;
    }

    /** 화면에 표시된 순서대로 기본 키·보조 키 행을 제공한다. */
    List<List<JComponent>> navigationRows() {
        List<List<JComponent>> rows = new ArrayList<>();
        for (GameAction action : GameAction.values()) {
            JButton[] buttons = keyButtons.get(action);
            if (buttons != null) {
                rows.add(List.of(buttons[0], buttons[1]));
            }
        }
        return rows;
    }

    void refreshKeys() {
        for (GameAction action : keyButtons.keySet()) {
            var binding = editor.getDraftBinding(action);
            JButton[] buttons = keyButtons.get(action);
            buttons[0].setText(keyLabel(binding.mainKeyCode()));
            buttons[1].setText(keyLabel(binding.subKeyCode()));
            buttons[0].setForeground(Color.WHITE);
            buttons[1].setForeground(Color.WHITE);
        }
    }

    void focusMissingMainKey() {
        for (GameAction action : keyButtons.keySet()) {
            if (editor.getDraftBinding(action).mainKeyCode().isEmpty()) {
                keyButtons.get(action)[0].requestFocusInWindow();
                return;
            }
        }
    }

    private static String keyLabel(OptionalInt code) {
        if (code.isEmpty()) {
            return "";
        }
        return switch (code.getAsInt()) {
            case KeyEvent.VK_ESCAPE -> "Esc";
            case KeyEvent.VK_LEFT -> "←";
            case KeyEvent.VK_RIGHT -> "→";
            case KeyEvent.VK_UP -> "↑";
            case KeyEvent.VK_DOWN -> "↓";
            case KeyEvent.VK_SPACE -> "Space";
            default -> KeyEvent.getKeyText(code.getAsInt());
        };
    }

    private static String actionLabel(GameAction action) {
        return switch (action) {
            case MOVE_LEFT -> "왼쪽 이동";
            case MOVE_RIGHT -> "오른쪽 이동";
            case SOFT_DROP -> "아래 이동";
            case HARD_DROP -> "즉시 낙하";
            case ROTATE -> "시계 방향 회전";
            case ROTATE_COUNTERCLOCKWISE -> "반시계 방향 회전";
            case PAUSE_MENU -> "일시정지 메뉴";
        };
    }
}
