package seoultech.se.tetris.ui.settings;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

import seoultech.se.tetris.config.BattleKeyBindingEditor;
import seoultech.se.tetris.config.BattleKeyConfigService.BattleAction;
import seoultech.se.tetris.config.BattleKeyConfigService.BattleKeySlot;
import seoultech.se.tetris.config.BattleKeyConfigService.BattlePlayer;

/**
 * 대전 모드 2인 조작 키 목록과 키 입력 대기 상태를 관리한다.
 */
final class BattleKeyBindingsPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private final BattleKeyBindingEditor editor;
    private final Map<BattleKeySlot, JButton> slotButtons = new HashMap<>();
    private final KeyEventDispatcher keyCapture = this::captureKey;
    private BattleKeySlot captureSlot;
    private int capturedCode = KeyEvent.VK_UNDEFINED;
    private boolean interactive = true;

    BattleKeyBindingsPanel(BattleKeyBindingEditor editor) {
        // 1인 모드 키 설정 패널(헤더 1행 + 6개 동작 행 = 7행)과 동일한 7행 3열 그리드로 구성
        super(new GridLayout(7, 3, 10, 8));
        this.editor = Objects.requireNonNull(editor, "editor");
        setBackground(Color.BLACK);

        add(SettingsPanel.label("동작"));
        add(SettingsPanel.label("1P"));
        add(SettingsPanel.label("2P"));

        for (BattleAction action : BattleAction.values()) {
            add(SettingsPanel.label(actionLabel(action)));

            JButton p1Button = createKeyButton(BattlePlayer.P1, action);
            JButton p2Button = createKeyButton(BattlePlayer.P2, action);

            slotButtons.put(new BattleKeySlot(BattlePlayer.P1, action), p1Button);
            slotButtons.put(new BattleKeySlot(BattlePlayer.P2, action), p2Button);

            add(p1Button);
            add(p2Button);
        }
        // 1인 모드 패널과 행 수를 완벽히 일치시키기 위한 빈 7번째 행
        add(new javax.swing.JLabel(""));
        add(new javax.swing.JLabel(""));
        add(new javax.swing.JLabel(""));
        refreshKeys();
    }

    private JButton createKeyButton(BattlePlayer player, BattleAction action) {
        JButton button = new JButton();
        SettingsPanel.styleButton(button, false);
        button.setToolTipText(player.name() + " " + actionLabel(action) + " 키 변경");
        button.setPreferredSize(new Dimension(110, button.getPreferredSize().height));
        button.addActionListener(event -> beginKeyCapture(new BattleKeySlot(player, action), button));
        return button;
    }

    private void beginKeyCapture(BattleKeySlot slot, JButton button) {
        if (!interactive) {
            return;
        }
        if (captureSlot == null) {
            KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(keyCapture);
        }
        captureSlot = slot;
        capturedCode = KeyEvent.VK_UNDEFINED;
        refreshKeys();
        button.setText("입력 대기…");
        button.setForeground(Color.YELLOW);
        button.requestFocusInWindow();
    }

    private boolean captureKey(KeyEvent event) {
        if (captureSlot == null) {
            return false;
        }
        if (event.getID() == KeyEvent.KEY_PRESSED && capturedCode == KeyEvent.VK_UNDEFINED) {
            capturedCode = event.getKeyCode();
            editor.captureKey(captureSlot, capturedCode);
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
        captureSlot = null;
        capturedCode = KeyEvent.VK_UNDEFINED;
    }

    void cancelKeyCapture() {
        if (captureSlot != null) {
            endKeyCapture();
            refreshKeys();
        }
    }

    boolean isCapturingKey() {
        return captureSlot != null;
    }

    /** 인게임 일시정지 등에서 버튼 활성화 여부를 설정한다. */
    void setInteractive(boolean enabled) {
        this.interactive = enabled;
        if (!enabled) {
            cancelKeyCapture();
        }
        for (JButton button : slotButtons.values()) {
            button.setEnabled(enabled);
        }
    }

    boolean isInteractive() {
        return interactive;
    }

    /** 동작 순환 순서대로 1P·2P 버튼 행을 제공한다. */
    List<List<JComponent>> navigationRows() {
        List<List<JComponent>> rows = new ArrayList<>();
        if (!interactive) {
            return rows;
        }
        for (BattleAction action : BattleAction.values()) {
            JButton p1 = slotButtons.get(new BattleKeySlot(BattlePlayer.P1, action));
            JButton p2 = slotButtons.get(new BattleKeySlot(BattlePlayer.P2, action));
            if (p1 != null && p2 != null) {
                rows.add(List.of(p1, p2));
            }
        }
        return rows;
    }

    void refreshKeys() {
        for (Map.Entry<BattleKeySlot, JButton> entry : slotButtons.entrySet()) {
            BattleKeySlot slot = entry.getKey();
            JButton button = entry.getValue();
            OptionalInt code = editor.getDraftBinding(slot);
            button.setText(keyLabel(code));
            button.setForeground(Color.WHITE);
        }
    }

    boolean focusMissingKey() {
        if (!interactive) {
            return false;
        }
        for (BattleAction action : BattleAction.values()) {
            for (BattlePlayer player : BattlePlayer.values()) {
                BattleKeySlot slot = new BattleKeySlot(player, action);
                if (editor.getDraftBinding(slot).isEmpty()) {
                    JButton button = slotButtons.get(slot);
                    if (button != null) {
                        button.requestFocusInWindow();
                        return true;
                    }
                }
            }
        }
        return false;
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
            case KeyEvent.VK_SHIFT -> "Shift";
            case KeyEvent.VK_CONTROL -> "Ctrl";
            default -> KeyEvent.getKeyText(code.getAsInt());
        };
    }

    private static String actionLabel(BattleAction action) {
        return switch (action) {
            case MOVE_LEFT -> "왼쪽 이동";
            case MOVE_RIGHT -> "오른쪽 이동";
            case SOFT_DROP -> "아래 이동";
            case HARD_DROP -> "즉시 낙하";
            case ROTATE -> "시계 방향 회전";
        };
    }
}
