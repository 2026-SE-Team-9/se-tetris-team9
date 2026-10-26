package seoultech.se.tetris.ui.game;

import java.util.Objects;
import javax.swing.Timer;
import seoultech.se.tetris.component.controller.GameController;
import seoultech.se.tetris.component.listener.GameEventListener;

/**
 * 줄 삭제 애니메이션의 길이를 재는 Swing 타이머.
 *
 * <p>엔진이 애니메이션을 시작했다고 알리면 정해진 시간 뒤에 한 번
 * {@link GameController#finishClearing()}을 호출해 게임을 이어가게 한다.
 *
 * <p>낙하 타이머와 따로 둔다. 낙하 간격은 레벨마다 달라지지만 애니메이션 길이는 항상 같아야 하기 때문이다.
 */
public final class ClearAnimationTimer implements GameEventListener {

    /**
     * 애니메이션 길이(ms). 난이도·레벨과 관계없이 같다.
     *
     * <p>지워질 칸이 하얗게 보이는 시간이다. 연속으로 줄을 지울 때 진행이 답답하지 않고
     * 반짝임을 알아볼 수 있도록 300ms로 정했다.
     */
    public static final int DURATION_MS = 300;

    private final Timer timer;

    public ClearAnimationTimer(GameController controller) {
        Objects.requireNonNull(controller, "controller");
        timer = new Timer(DURATION_MS, event -> controller.finishClearing());
        timer.setRepeats(false);
    }

    /** 화면을 닫을 때 타이머를 중지한다. */
    public void stop() {
        timer.stop();
    }

    @Override
    public void onClearingStarted() {
        timer.restart();
    }

    @Override
    public void onGameOver(int finalScore) {
        timer.stop();
    }

    @Override
    public void onBoardUpdated() {
    }

    @Override
    public void onLinesCleared(int clearedCount, int gainedScore) {
    }

    @Override
    public void onSpeedIncreased(int currentInterval) {
    }

    // 실제 시간을 기다리지 않고 실행 상태를 검증한다.
    boolean isRunning() {
        return timer.isRunning();
    }

    boolean repeats() {
        return timer.isRepeats();
    }

    void fire() {
        timer.stop();
        timer.getActionListeners()[0].actionPerformed(null);
    }
}
