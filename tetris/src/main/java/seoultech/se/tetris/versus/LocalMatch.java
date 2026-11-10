package seoultech.se.tetris.versus;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import seoultech.se.tetris.component.controller.GameController;
import seoultech.se.tetris.component.listener.GameEventListener;
import seoultech.se.tetris.component.model.Attack;
import seoultech.se.tetris.component.model.GameState;
import seoultech.se.tetris.engine.Difficulty;
import seoultech.se.tetris.engine.TetrisEngine;

/**
 * 한 PC에서 두 사람이 하는 대전. (3차 「로컬 대전 모드」)
 *
 * <p>두 플레이어의 엔진을 묶어 다음을 맡는다.
 * <ul>
 *   <li>한쪽이 보낸 공격을 상대 엔진의 대기 줄에 넣는다</li>
 *   <li>한쪽이 블럭을 쌓지 못해 끝나면 상대의 승리로 정하고 두 게임을 모두 멈춘다</li>
 *   <li>제한 시간이 끝나면 두 게임을 마무리한 뒤 점수를 비교한다. 같으면 무승부다</li>
 *   <li>일시정지와 나가기를 두 플레이어에 함께 적용한다</li>
 * </ul>
 *
 * <p>{@link #controller}가 돌려주는 조작 명령은 진행 중({@link MatchPhase#PLAYING})일 때만 이동·회전·드롭을 엔진에 전달한다.
 * 카운트다운이나 일시정지 중에 들어온 키 입력이 한쪽 보드만 움직이는 일을 막기 위해서다.
 * 일시정지·재시작·종료 명령은 한쪽 엔진에만 적용되지 않도록 이 대전으로 넘기거나 무시한다.
 */
public final class LocalMatch implements VersusMatch {

    private final VersusMode mode;
    private final Map<Side, TetrisEngine> engines = new EnumMap<>(Side.class);
    private final Map<Side, GameController> controllers = new EnumMap<>(Side.class);
    private final List<MatchListener> listeners = new ArrayList<>();

    private MatchPhase phase = MatchPhase.READY;
    private MatchResult result;

    /**
     * 두 플레이어가 같은 블럭 순서로 시작하는 대전을 만든다.
     *
     * @param mode       대전 모드
     * @param difficulty 난이도. 로컬 대전은 설정의 난이도를 쓴다 (3차 팀 결정)
     * @param seed       블럭 순서를 정하는 시드
     */
    public static LocalMatch create(VersusMode mode, Difficulty difficulty, long seed) {
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(difficulty, "difficulty");
        return new LocalMatch(mode,
                TetrisEngine.seeded(seed, difficulty, mode.gameMode()),
                TetrisEngine.seeded(seed, difficulty, mode.gameMode()));
    }

    /** 테스트용. 지정한 두 엔진으로 대전을 만든다. */
    LocalMatch(VersusMode mode, TetrisEngine p1, TetrisEngine p2) {
        this.mode = Objects.requireNonNull(mode, "mode");
        engines.put(Side.P1, Objects.requireNonNull(p1, "p1"));
        engines.put(Side.P2, Objects.requireNonNull(p2, "p2"));
        for (Side side : Side.values()) {
            engines.get(side).addListener(new EngineListener(side));
            controllers.put(side, new MatchController(engines.get(side)));
        }
    }

    // ------------------------------------------------------------------
    // 조회
    // ------------------------------------------------------------------

    @Override
    public VersusMode mode() {
        return mode;
    }

    @Override
    public MatchPhase phase() {
        return phase;
    }

    @Override
    public GameState state(Side side) {
        return engines.get(Objects.requireNonNull(side, "side"));
    }

    /** 로컬 대전은 두 자리를 모두 이 PC에서 조작한다. */
    @Override
    public Optional<GameController> controller(Side side) {
        return Optional.of(controllers.get(Objects.requireNonNull(side, "side")));
    }

    @Override
    public Optional<MatchResult> result() {
        return Optional.ofNullable(result);
    }

    @Override
    public void addListener(MatchListener listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    @Override
    public void removeListener(MatchListener listener) {
        listeners.remove(listener);
    }

    /** 대전이 먼저 등록한 리스너보다 뒤에 알림을 받으므로, 게임 오버를 받을 때는 이미 대전이 끝나 있다. */
    @Override
    public void addGameListener(Side side, GameEventListener listener) {
        engines.get(Objects.requireNonNull(side, "side")).addListener(listener);
    }

    @Override
    public void removeGameListener(Side side, GameEventListener listener) {
        engines.get(Objects.requireNonNull(side, "side")).removeListener(listener);
    }

    // ------------------------------------------------------------------
    // 진행
    // ------------------------------------------------------------------

    @Override
    public void begin() {
        if (phase == MatchPhase.READY) {
            changePhase(MatchPhase.PLAYING);
        }
    }

    @Override
    public boolean supportsPause() {
        return true;
    }

    /** 두 엔진을 멈춘다. 줄 삭제 애니메이션 중인 엔진은 애니메이션이 끝난 뒤에 멈춘다({@link MatchController#finishClearing()}). */
    @Override
    public void pause() {
        if (phase != MatchPhase.PLAYING) {
            return;
        }
        engines.values().forEach(TetrisEngine::pauseGame);
        changePhase(MatchPhase.PAUSED);
    }

    @Override
    public void resume() {
        if (phase != MatchPhase.PAUSED) {
            return;
        }
        engines.values().forEach(TetrisEngine::resumeGame);
        changePhase(MatchPhase.PLAYING);
    }

    @Override
    public void timeUp() {
        if (phase != MatchPhase.PLAYING || !mode.hasTimeLimit()) {
            return;
        }
        engines.values().forEach(TetrisEngine::finishByTimeUp);
        int p1 = engines.get(Side.P1).getScore();
        int p2 = engines.get(Side.P2).getScore();
        if (p1 == p2) {
            end(MatchResult.draw(EndReason.TIME_UP, p1, p2));
        } else {
            end(MatchResult.win(p1 > p2 ? Side.P1 : Side.P2, EndReason.TIME_UP, p1, p2));
        }
    }

    /** 로컬 대전은 결과 없이 끝난다. (3차 팀 결정) */
    @Override
    public void quit() {
        if (phase == MatchPhase.ENDED) {
            return;
        }
        stopEngines();
        changePhase(MatchPhase.ENDED);
    }

    /** {@code loser}가 블럭을 쌓지 못해 끝났다. 상대의 승리로 정한다. */
    private void toppedOut(Side loser) {
        if (phase != MatchPhase.PLAYING && phase != MatchPhase.PAUSED) {
            return;
        }
        stopEngines();
        int p1 = engines.get(Side.P1).getScore();
        int p2 = engines.get(Side.P2).getScore();
        end(MatchResult.win(loser.opponent(), EndReason.TOP_OUT, p1, p2));
    }

    /** 결과를 정하고 끝낸다. 리스너가 단계 변경을 받을 때 이미 {@link #result()}를 볼 수 있다. */
    private void end(MatchResult matchResult) {
        result = matchResult;
        changePhase(MatchPhase.ENDED);
        for (MatchListener listener : List.copyOf(listeners)) {
            listener.onMatchEnded(matchResult);
        }
    }

    /** 끝나지 않은 엔진을 모두 멈춘다. 이미 끝난 엔진에 다시 알림이 가지 않도록 확인한다. */
    private void stopEngines() {
        for (TetrisEngine engine : engines.values()) {
            if (!engine.isGameOver()) {
                engine.exitGame();
            }
        }
    }

    private void changePhase(MatchPhase next) {
        phase = next;
        for (MatchListener listener : List.copyOf(listeners)) {
            listener.onPhaseChanged(next);
        }
    }

    // ------------------------------------------------------------------
    // 엔진 이벤트
    // ------------------------------------------------------------------

    /** 한 플레이어의 엔진에서 공격과 게임 오버를 받아 대전에 반영한다. */
    private final class EngineListener implements GameEventListener {

        private final Side side;

        EngineListener(Side side) {
            this.side = side;
        }

        @Override
        public void onAttack(Attack attack) {
            if (phase == MatchPhase.PLAYING || phase == MatchPhase.PAUSED) {
                engines.get(side.opponent()).receiveAttack(attack);
            }
        }

        @Override
        public void onGameOver(int finalScore) {
            toppedOut(side);
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
    }

    // ------------------------------------------------------------------
    // 조작 명령
    // ------------------------------------------------------------------

    /**
     * 한 플레이어에게 넘기는 조작 명령.
     *
     * <ul>
     *   <li>이동·회전·드롭: 진행 중일 때만 엔진에 전달한다</li>
     *   <li>줄 삭제 애니메이션 마무리: 항상 전달한다. 애니메이션 타이머가 보낸 신호를 버리면 엔진이 멈춘 채로 남는다.
     *       일시정지 중이면 마무리한 뒤 엔진을 다시 멈춘다</li>
     *   <li>일시정지·재개: 두 플레이어에 함께 적용하도록 대전으로 넘긴다</li>
     *   <li>종료: 대전 나가기로 넘긴다</li>
     *   <li>재시작: 한쪽만 다시 시작할 수 없으므로 무시한다</li>
     * </ul>
     */
    private final class MatchController implements GameController {

        private final TetrisEngine engine;

        MatchController(TetrisEngine engine) {
            this.engine = engine;
        }

        private boolean playing() {
            return phase == MatchPhase.PLAYING;
        }

        @Override
        public void moveLeft() {
            if (playing()) {
                engine.moveLeft();
            }
        }

        @Override
        public void moveRight() {
            if (playing()) {
                engine.moveRight();
            }
        }

        @Override
        public void moveDown() {
            if (playing()) {
                engine.moveDown();
            }
        }

        @Override
        public void dropToBottom() {
            if (playing()) {
                engine.dropToBottom();
            }
        }

        @Override
        public void rotate() {
            if (playing()) {
                engine.rotate();
            }
        }

        @Override
        public void rotateCounterclockwise() {
            if (playing()) {
                engine.rotateCounterclockwise();
            }
        }

        @Override
        public void finishClearing() {
            engine.finishClearing();
            // 일시정지 중에 애니메이션이 끝나면 엔진이 진행 중으로 돌아가므로 다시 멈춘다
            if (phase == MatchPhase.PAUSED) {
                engine.pauseGame();
            }
        }

        @Override
        public void togglePause() {
            if (phase == MatchPhase.PLAYING) {
                pause();
            } else if (phase == MatchPhase.PAUSED) {
                resume();
            }
        }

        @Override
        public void pauseGame() {
            pause();
        }

        @Override
        public void resumeGame() {
            resume();
        }

        @Override
        public void restartGame() {
        }

        @Override
        public void exitGame() {
            quit();
        }
    }
}
