package seoultech.se.tetris.ui.versus;

import java.util.Objects;

import seoultech.se.tetris.versus.MatchListener;
import seoultech.se.tetris.versus.MatchPhase;
import seoultech.se.tetris.versus.MatchResult;
import seoultech.se.tetris.versus.VersusMatch;

/**
 * 시간제한 모드에서 대전과 남은 시간을 연결한다. Swing/AWT에 의존하지 않는다.
 *
 * <p>대전이 진행 중일 때만 시계가 줄어들고, 일시정지하면 시계도 멈춘다. (3차 팀 결정)
 * 대전이 끝나면 시계를 고정한다. 화면은 주기적으로 {@link #tick()}을 호출하며,
 * 제한 시간이 모두 지났으면 대전에 시간 종료를 알린다. 승패 판정은 대전 모델이 한다.
 *
 * <p>대전의 {@link VersusMatch#begin()}보다 먼저 리스너로 등록해야 시작 시점부터 잰다.
 */
public final class TimeLimitWatcher implements MatchListener {

    private final VersusMatch match;
    private final CountdownClock clock;

    public TimeLimitWatcher(VersusMatch match, CountdownClock clock) {
        this.match = Objects.requireNonNull(match, "match");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public CountdownClock clock() {
        return clock;
    }

    @Override
    public void onPhaseChanged(MatchPhase phase) {
        switch (phase) {
            case PLAYING -> clock.start();
            case PAUSED -> clock.pause();
            case ENDED -> clock.stop();
            default -> {
                // 시작 전(READY)에는 재지 않는다
            }
        }
    }

    @Override
    public void onMatchEnded(MatchResult result) {
        clock.stop();
    }

    /** 제한 시간이 모두 지났으면 대전에 시간 종료를 알린다. 진행 중이 아닌 대전은 대전 모델이 무시한다. */
    public void tick() {
        if (clock.isExpired() && match.phase() == MatchPhase.PLAYING) {
            match.timeUp();
        }
    }
}
