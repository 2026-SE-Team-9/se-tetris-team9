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
 *   <li>대전 중: 2인 키 설정의 키는 해당 플레이어에게 전달하고, Esc는 나가기 확인을 요청한다</li>
 *   <li>대전이 끝난 뒤: Enter로 기다리지 않고 바로 대전 종료 화면으로 넘어간다</li>
 * </ul>
 */
public final class VersusKeyInput extends KeyAdapter {

    private final VersusKeyMap keyMap;
    private final Function<Side, Optional<GameController>> controllers;
    private final VersusScreenModel model;
    private final Runnable requestQuit;
    private final Runnable showResult;

    /**
     * @param controllers  플레이어 자리별 조작기
     * @param requestQuit  대전 중 Esc를 눌렀을 때 실행할 나가기 확인
     * @param showResult   대전이 끝난 뒤 대전 종료 화면으로 넘어가는 동작
     */
    public VersusKeyInput(VersusKeyMap keyMap, Function<Side, Optional<GameController>> controllers,
            VersusScreenModel model, Runnable requestQuit, Runnable showResult) {
        this.keyMap = Objects.requireNonNull(keyMap, "keyMap");
        this.controllers = Objects.requireNonNull(controllers, "controllers");
        this.model = Objects.requireNonNull(model, "model");
        this.requestQuit = Objects.requireNonNull(requestQuit, "requestQuit");
        this.showResult = Objects.requireNonNull(showResult, "showResult");
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
            requestQuit.run();
            return;
        }
        keyMap.dispatch(keyCode, controllers);
    }
}
