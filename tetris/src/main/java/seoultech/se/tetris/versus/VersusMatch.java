package seoultech.se.tetris.versus;

import java.time.Duration;
import java.util.Optional;
import seoultech.se.tetris.component.controller.GameController;
import seoultech.se.tetris.component.listener.GameEventListener;
import seoultech.se.tetris.component.model.GameState;

/**
 * 대전 한 판. 두 플레이어의 게임을 묶고, 공격 전달과 승패 판정을 맡는다.
 *
 * <p>시간을 재지 않는다. 시작 전 카운트다운과 제한 시간은 타이머를 가진 쪽이 재고,
 * 때가 되면 {@link #begin()}과 {@link #timeUp()}을 호출한다. 이 객체는 받은 신호에 따라 판정만 한다.
 * 덕분에 실제 시간을 기다리지 않고 대전 전체를 테스트할 수 있다.
 *
 * <p>모든 메서드는 Swing 이벤트 스레드에서 호출한다.
 *
 * <p>담당별로 주로 쓰는 메서드는 다음과 같다.
 * <ul>
 *   <li>화면: {@link #state}, {@link #mode}, {@link #phase}, {@link #result},
 *       {@link #addListener}, {@link #addGameListener}</li>
 *   <li>조작·흐름: {@link #controller}, {@link #begin}, {@link #pause}, {@link #resume}, {@link #timeUp},
 *       {@link #quit}, {@link #addListener}, {@link #addGameListener}</li>
 * </ul>
 */
public interface VersusMatch {

    /** 시작 전 카운트다운 길이. 모든 대전 모드에 적용한다. (3차 팀 결정) */
    Duration COUNTDOWN = Duration.ofSeconds(3);

    /** 이 대전의 모드 */
    VersusMode mode();

    /** 현재 진행 단계. 처음에는 {@link MatchPhase#READY} */
    MatchPhase phase();

    /** 그 자리의 보드를 그리기 위한 상태 */
    GameState state(Side side);

    /**
     * 그 자리를 조작하는 명령. 이 PC에서 조작할 수 없는 자리(P2P의 상대)는 비어 있다.
     *
     * <p>키 입력은 이동·회전·드롭만 이 명령으로 보낸다.
     * 일시정지와 종료는 두 플레이어에 함께 적용해야 하므로 이 객체의 {@link #pause()}, {@link #quit()} 등을 쓴다.
     */
    Optional<GameController> controller(Side side);

    void addListener(MatchListener listener);

    void removeListener(MatchListener listener);

    /**
     * 그 자리의 게임 이벤트(보드 변화, 줄 삭제, 속도 변화 등)를 받을 대상을 등록한다.
     *
     * <p>화면 갱신, 낙하 타이머, 줄 삭제 애니메이션 타이머처럼 1인용에서 엔진에 직접 등록하던 것을 여기에 등록한다.
     * 대전의 끝은 {@link GameEventListener#onGameOver(int)}가 아니라 {@link MatchListener}로 받는다.
     * 대전은 한쪽의 게임 오버를 받는 즉시 결과를 정하고 두 게임을 모두 멈추며,
     * 제한 시간 종료처럼 게임 오버 알림 없이 끝나는 경우도 있기 때문이다.
     */
    void addGameListener(Side side, GameEventListener listener);

    void removeGameListener(Side side, GameEventListener listener);

    /**
     * 카운트다운이 끝나 대전을 시작한다. {@link MatchPhase#READY}에서 {@link MatchPhase#PLAYING}이 된다.
     * 그 외 단계에서는 아무 일도 하지 않는다.
     */
    void begin();

    /** 일시정지할 수 있는 대전인지. 로컬 대전은 가능하고, P2P 대전은 일시정지가 없다. (3차 팀 결정) */
    boolean supportsPause();

    /**
     * 두 플레이어의 게임과 제한 시간을 함께 멈춘다. {@link MatchPhase#PLAYING}에서만 동작한다.
     *
     * @throws UnsupportedOperationException {@link #supportsPause()}가 {@code false}인 경우
     */
    void pause();

    /** 일시정지를 푼다. {@link MatchPhase#PAUSED}에서만 동작한다. */
    void resume();

    /**
     * 시간제한 모드의 제한 시간이 끝났다.
     *
     * <p>두 플레이어의 진행 중인 처리를 마무리한 뒤 점수를 비교해 결과를 정한다. 점수가 같으면 무승부다.
     * {@link MatchPhase#PLAYING}에서만 동작하며, 시간제한 모드가 아니면 아무 일도 하지 않는다.
     */
    void timeUp();

    /**
     * 대전 도중 나간다.
     *
     * <p>로컬 대전에서는 결과 없이 끝난다. P2P 대전에서는 상대의 기권승이 된다. (3차 팀 결정)
     * 이미 끝난 대전에서는 아무 일도 하지 않는다.
     */
    void quit();

    /** 결과. 끝나지 않았거나 결과 없이 끝났으면 비어 있다. */
    Optional<MatchResult> result();
}
