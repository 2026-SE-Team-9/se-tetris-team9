package seoultech.se.tetris.versus;

import java.time.Duration;
import java.util.Optional;
import seoultech.se.tetris.engine.GameMode;

/**
 * 대전 모드의 종류. 대전을 시작할 때 일반 / 아이템 / 시간제한 중 하나를 고른다.
 *
 * <p>시간제한은 블럭이 움직이는 규칙(엔진)이 아니라 경기를 끝내는 규칙이므로
 * {@link GameMode}에 넣지 않고 여기서 따로 다룬다. 엔진에는 {@link #gameMode()}만 넘긴다. (3차 팀 결정)
 *
 * <p>대전 모드는 스코어보드를 관리하지 않으므로 이 상수 이름은 저장 파일에 기록되지 않는다.
 * 화면에 보여줄 이름은 화면을 표시하는 쪽에서 별도로 처리한다.
 */
public enum VersusMode {

    /** 일반 모드 */
    NORMAL(GameMode.NORMAL, null),

    /** 아이템 모드. 아이템이 등장하는 것 외에는 일반 모드와 같다. */
    ITEM(GameMode.ITEM, null),

    /** 시간제한 모드. 일반 모드를 기반으로 제한 시간 3분을 둔다. (3차 팀 결정) */
    TIME_LIMIT(GameMode.NORMAL, Duration.ofMinutes(3));

    private final GameMode gameMode;
    private final Duration timeLimit;

    VersusMode(GameMode gameMode, Duration timeLimit) {
        this.gameMode = gameMode;
        this.timeLimit = timeLimit;
    }

    /** 엔진에 넘길 게임 모드 */
    public GameMode gameMode() {
        return gameMode;
    }

    /** 제한 시간. 시간제한 모드가 아니면 비어 있다. */
    public Optional<Duration> timeLimit() {
        return Optional.ofNullable(timeLimit);
    }

    /** 제한 시간이 있는 모드인지 반환 */
    public boolean hasTimeLimit() {
        return timeLimit != null;
    }
}
