package seoultech.se.tetris.ui.game;

import java.util.Objects;
import javax.swing.Timer;
import seoultech.se.tetris.component.controller.GameController;
import seoultech.se.tetris.component.listener.GameEventListener;
import seoultech.se.tetris.component.model.GameState;
import seoultech.se.tetris.engine.LevelPolicy;

/** 게임 상태와 레벨에 맞춰 자동 낙하를 실행하는 Swing 타이머. */
public final class GameProgressTimer implements GameEventListener {
    private final GameState state;
    private final GameController controller;
    private final Timer timer;
    private boolean started;

    public GameProgressTimer(GameState state, GameController controller) {
        this.state = Objects.requireNonNull(state, "state");
        this.controller = Objects.requireNonNull(controller, "controller");
        timer = new Timer(LevelPolicy.INITIAL_INTERVAL_MS, event -> {
            if (!state.isPaused() && !state.isGameOver() && !state.isClearing()) {
                controller.moveDown();
            }
        });
    }

    /** 화면이 표시된 뒤 자동 낙하를 시작한다. */
    public void start() {
        started = true;
        syncWithState();
    }

    /** 화면을 닫을 때 타이머를 중지한다. */
    public void stop() {
        started = false;
        timer.stop();
    }

    @Override
    public void onBoardUpdated() {
        syncWithState();
    }

    @Override
    public void onLinesCleared(int clearedCount, int gainedScore) {
    }

    @Override
    public void onSpeedIncreased(int currentInterval) {
        timer.setDelay(currentInterval);
        timer.setInitialDelay(currentInterval);
    }

    @Override
    public void onGameOver(int finalScore) {
        timer.stop();
    }

    private void syncWithState() {
        if (!started || state.isPaused() || state.isGameOver() || state.isClearing()) {
            timer.stop();
        } else if (!timer.isRunning()) {
            timer.start();
        }
    }

    // 실제 시간을 기다리지 않고 낙하 간격과 실행 상태를 검증한다.
    boolean isRunning() {
        return timer.isRunning();
    }

    int intervalMs() {
        return timer.getDelay();
    }

    void tick() {
        timer.getActionListeners()[0].actionPerformed(null);
    }
}
