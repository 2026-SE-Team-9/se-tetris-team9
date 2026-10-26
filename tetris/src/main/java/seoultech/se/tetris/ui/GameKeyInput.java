package seoultech.se.tetris.ui;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.swing.JComponent;
import seoultech.se.tetris.component.controller.GameController;
import seoultech.se.tetris.component.model.GameState;
import seoultech.se.tetris.config.KeyBindingEditor;
import seoultech.se.tetris.config.KeyConfigService;
import seoultech.se.tetris.config.KeyConfigService.GameAction;

/**
 * 게임 화면의 키 입력을 현재 키 설정에 맞는 게임 조작으로 전달한다.
 * 화면은 표시된 뒤 키보드 포커스를 받아야 하며, 저장된 설정은 생성 전에 로드해야 한다.
 */
public final class GameKeyInput extends KeyAdapter {
    private final KeyConfigService keyConfig;
    private final GameController controller;
    private final GameState state;
    private final Runnable showPauseMenu;
    private Map<Integer, GameAction> actionsByKey = Map.of();
    private JComponent attachedComponent;

    public GameKeyInput(KeyConfigService keyConfig, GameController controller, GameState state,
            Runnable showPauseMenu) {
        this.keyConfig = Objects.requireNonNull(keyConfig, "keyConfig");
        this.controller = Objects.requireNonNull(controller, "controller");
        this.state = Objects.requireNonNull(state, "state");
        this.showPauseMenu = Objects.requireNonNull(showPauseMenu, "showPauseMenu");
        applyKeyBindings();
    }

    /** 저장된 설정으로 맵 전체를 교체하여 제거·변경된 이전 키 매핑도 없앤다. */
    public void applyKeyBindings() {
        Map<Integer, GameAction> updated = new HashMap<>();
        for (GameAction action : GameAction.values()) {
            if (action == GameAction.PAUSE_MENU) {
                continue;
            }
            for (int keyCode : keyConfig.getKeyCodes(action)) {
                if (keyCode != KeyEvent.VK_ESCAPE) {
                    updated.put(keyCode, action);
                }
            }
        }
        updated.put(KeyEvent.VK_ESCAPE, GameAction.PAUSE_MENU);
        actionsByKey = Map.copyOf(updated);
    }

    /** 편집 중에는 기존 입력을 유지하고 저장 성공 시 새 입력 맵을 적용하는 편집기를 만든다. */
    public KeyBindingEditor createKeyBindingEditor() {
        return new KeyBindingEditor(keyConfig, this::applyKeyBindings);
    }

    /** 게임 화면에 연결한다. 같은 화면에 다시 연결해도 리스너는 한 번만 등록된다. */
    public void attachTo(JComponent component) {
        Objects.requireNonNull(component, "component");
        if (component == attachedComponent) {
            component.requestFocusInWindow();
            return;
        }
        detach();
        component.setFocusable(true);
        component.addKeyListener(this);
        attachedComponent = component;
        component.requestFocusInWindow();
    }

    /** 화면을 벗어날 때 입력 연결을 해제한다. */
    public void detach() {
        if (attachedComponent != null) {
            attachedComponent.removeKeyListener(this);
            attachedComponent = null;
        }
    }

    /** 연결된 화면의 키를 액션으로 찾아 실행한다. 일시정지 중 조작과 게임 종료 후 입력은 무시한다.
     * 줄 삭제 애니메이션 중에는 입력을 무시한다.
     */
    @Override
    public void keyPressed(KeyEvent event) {
        if (attachedComponent == null || event.getComponent() != attachedComponent) {
            return;
        }
        GameAction action = actionsByKey.get(event.getKeyCode());
        if (action == null || state.isGameOver() || state.isClearing()) {
            return;
        }
        if (action == GameAction.PAUSE_MENU) {
            if (!state.isPaused()) {
                controller.pauseGame();
                showPauseMenu.run();
            }
            return;
        }
        if (state.isPaused()) {
            return;
        }
        switch (action) {
            case MOVE_LEFT -> controller.moveLeft();
            case MOVE_RIGHT -> controller.moveRight();
            case SOFT_DROP -> controller.moveDown();
            case HARD_DROP -> controller.dropToBottom();
            case ROTATE -> controller.rotate();
            case ROTATE_COUNTERCLOCKWISE -> controller.rotateCounterclockwise();
            case PAUSE_MENU -> throw new IllegalStateException("이미 처리한 액션");
        }
    }
}
