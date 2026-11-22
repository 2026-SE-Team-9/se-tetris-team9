package seoultech.se.tetris.ui.versus;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import seoultech.se.tetris.component.controller.GameController;
import seoultech.se.tetris.versus.Side;

/**
 * 로컬 대전 화면의 키 입력. 한 창에서 두 플레이어의 키를 함께 받는다.
 *
 * <ul>
 *   <li>대전 중: 2인 키 설정의 키는 해당 플레이어에게 전달하고, Esc는 두 보드를 멈추고 일시정지 메뉴를 연다</li>
 *   <li>대전이 끝난 뒤: Enter로 기다리지 않고 바로 대전 종료 화면으로 넘어간다</li>
 * </ul>
 */
public final class VersusKeyInput extends KeyAdapter {

    private VersusKeyMap keyMap;
    private final Function<Side, Optional<GameController>> controllers;
    private final VersusScreenModel model;
    private final Runnable requestPause;
    private final Runnable showResult;

    /**
     * @param controllers  플레이어 자리별 조작기
     * @param requestPause 대전 중 Esc를 눌렀을 때 두 보드를 멈추고 일시정지 메뉴를 여는 동작
     * @param showResult   대전이 끝난 뒤 대전 종료 화면으로 넘어가는 동작
     */
    public VersusKeyInput(VersusKeyMap keyMap, Function<Side, Optional<GameController>> controllers,
            VersusScreenModel model, Runnable requestPause, Runnable showResult) {
        this.keyMap = Objects.requireNonNull(keyMap, "keyMap");
        this.controllers = Objects.requireNonNull(controllers, "controllers");
        this.model = Objects.requireNonNull(model, "model");
        this.requestPause = Objects.requireNonNull(requestPause, "requestPause");
        this.showResult = Objects.requireNonNull(showResult, "showResult");
    }

    /** 일시정지 중 설정에서 키를 바꿨을 때 새 키 설정으로 바꾼다. */
    public void setKeyMap(VersusKeyMap keyMap) {
        this.keyMap = Objects.requireNonNull(keyMap, "keyMap");
    }

    @Override
    public void keyPressed(KeyEvent event) {
        handle(event.getKeyCode());
    }

    /** 키 하나를 처리한다. */
    void handle(int keyCode) {
        if (model.isEnded()) {
            if (keyCode == KeyEvent.VK_ENTER) {
                showResult.run();
            }
            return;
        }
        if (keyCode == KeyEvent.VK_ESCAPE) {
            requestPause.run();
            return;
        }
        keyMap.dispatch(keyCode, controllers);
    }
}
