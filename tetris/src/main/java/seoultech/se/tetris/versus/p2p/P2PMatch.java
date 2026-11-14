package seoultech.se.tetris.versus.p2p;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import seoultech.se.tetris.component.controller.GameController;
import seoultech.se.tetris.component.listener.GameEventListener;
import seoultech.se.tetris.component.model.Attack;
import seoultech.se.tetris.component.model.GameState;
import seoultech.se.tetris.engine.TetrisEngine;
import seoultech.se.tetris.versus.BoardSnapshot;
import seoultech.se.tetris.versus.EndReason;
import seoultech.se.tetris.versus.MatchListener;
import seoultech.se.tetris.versus.MatchPhase;
import seoultech.se.tetris.versus.MatchResult;
import seoultech.se.tetris.versus.Side;
import seoultech.se.tetris.versus.VersusMatch;
import seoultech.se.tetris.versus.VersusMode;

/**
 * 두 PC가 네트워크로 하는 대전 한 판. (3차 「P2P 대전 모드」)
 *
 * <p>각 PC는 자신의 보드만 진행하고, 화면 정보와 공격만 주고받는다. 상대 자리는 받은 화면 정보로 보여준다.
 * 승패는 서버가 정해 클라이언트에 보낸다. (3차 팀 결정)
 *
 * <ul>
 *   <li>블럭을 쌓을 수 없게 되면 종료 신호({@link P2PMessage.End})를 보낸다.
 *       상대의 종료 신호를 먼저 받으면 자신의 게임을 멈추고 그 사실을 담아 답한다</li>
 *   <li>서버는 양쪽의 종료 신호가 모이면 승패를 정한다. 둘 다 상대의 신호를 받기 전에 끝났으면 엇갈림으로 무승부다</li>
 *   <li>제한 시간은 서버 시간 기준이다. 서버가 시간 종료를 처리한 뒤에 도착한 클라이언트의 종료 신호는 승패에 쓰지 않는다</li>
 *   <li>게임 나가기는 기권패이며, 연결도 끊는다</li>
 * </ul>
 *
 * <p>받은 메시지는 P2P 대전 모드({@link P2PSession})가 {@link #handle}로 넘겨준다.
 * 모든 메서드는 Swing 이벤트 스레드에서 호출한다.
 */
final class P2PMatch implements VersusMatch {

    private final Role role;
    private final VersusMode mode;
    private final TetrisEngine engine;
    private final RemoteGameState remote;
    private final PeerConnection connection;
    private final Runnable afterQuit;
    private final Runnable afterResult;

    private final GameController controller = new MatchController();
    private final List<MatchListener> listeners = new ArrayList<>();
    private final List<GameEventListener> remoteListeners = new ArrayList<>();

    private MatchPhase phase = MatchPhase.READY;
    private MatchResult result;

    /** 보낸 종료 신호 */
    private P2PMessage.End myEnd;
    /** 받은 종료 신호 */
    private P2PMessage.End opponentEnd;
    /** 서버: 시간 종료를 처리했는지 */
    private boolean timeUpSent;

    /**
     * 시작 신호로 대전을 만든다. 두 PC는 같은 시드로 같은 블럭 순서를 받는다.
     *
     * @param afterQuit   이 PC의 플레이어가 게임 나가기로 기권한 뒤 할 일. 연결을 끊는다
     * @param afterResult 그 밖의 이유로 결과가 정해져 대전이 끝난 뒤 할 일. 대기 상태로 돌아간다.
     *                    대전의 리스너가 모두 결과를 받은 뒤에 부른다
     */
    static P2PMatch create(Role role, P2PMessage.Start start,
            PeerConnection connection, Runnable afterQuit, Runnable afterResult) {
        TetrisEngine engine = TetrisEngine.seeded(start.seed(), start.difficulty(), start.mode().gameMode());
        // 상대도 같은 시드로 시작하므로, 첫 화면 정보를 받기 전에는 같은 시작 상태를 보여준다
        BoardSnapshot opponentStart = BoardSnapshot.of(
                TetrisEngine.seeded(start.seed(), start.difficulty(), start.mode().gameMode()));
        return new P2PMatch(role, start.mode(), engine, opponentStart,
                connection, afterQuit, afterResult);
    }

    /** 테스트용. 엔진과 상대의 첫 화면을 지정한다. */
    P2PMatch(Role role, VersusMode mode, TetrisEngine engine, BoardSnapshot opponentStart,
            PeerConnection connection, Runnable afterQuit, Runnable afterResult) {
        this.role = Objects.requireNonNull(role, "role");
        this.mode = Objects.requireNonNull(mode, "mode");
        this.engine = Objects.requireNonNull(engine, "engine");
        this.remote = new RemoteGameState(opponentStart);
        this.connection = Objects.requireNonNull(connection, "connection");
        this.afterQuit = Objects.requireNonNull(afterQuit, "afterQuit");
        this.afterResult = Objects.requireNonNull(afterResult, "afterResult");
        engine.addListener(new LocalEngineListener());
        connection.send(new P2PMessage.Snapshot(BoardSnapshot.of(engine)));
    }

    private Side me() {
        return role.side();
    }

    private Side opponent() {
        return role.side().opponent();
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

    /** 내 자리는 이 PC의 엔진, 상대 자리는 받은 화면 정보다. */
    @Override
    public GameState state(Side side) {
        return Objects.requireNonNull(side, "side") == me() ? engine : remote;
    }

    /** 내 자리만 조작할 수 있다. */
    @Override
    public Optional<GameController> controller(Side side) {
        return Objects.requireNonNull(side, "side") == me() ? Optional.of(controller) : Optional.empty();
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

    /** 상대 자리의 리스너는 새 화면 정보를 받을 때마다 {@link GameEventListener#onBoardUpdated()}를 받는다. */
    @Override
    public void addGameListener(Side side, GameEventListener listener) {
        Objects.requireNonNull(listener, "listener");
        if (Objects.requireNonNull(side, "side") == me()) {
            engine.addListener(listener);
        } else {
            remoteListeners.add(listener);
        }
    }

    @Override
    public void removeGameListener(Side side, GameEventListener listener) {
        if (Objects.requireNonNull(side, "side") == me()) {
            engine.removeListener(listener);
        } else {
            remoteListeners.remove(listener);
        }
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
        return false;
    }

    @Override
    public void pause() {
        throw new UnsupportedOperationException("P2P 대전에는 일시정지가 없습니다.");
    }

    /** 일시정지가 없으므로 아무 일도 하지 않는다. */
    @Override
    public void resume() {
    }

    /**
     * 서버에서만 동작한다. 클라이언트는 서버의 시간 종료 신호를 받아 마무리한다. (3차 팀 결정: 서버 측 시간 기준)
     * 이미 종료 신호를 주고받기 시작했으면, 그쪽을 먼저 처리한 것이므로 무시한다.
     */
    @Override
    public void timeUp() {
        if (role != Role.SERVER || phase != MatchPhase.PLAYING || !mode.hasTimeLimit()
                || result != null || timeUpSent || myEnd != null || opponentEnd != null) {
            return;
        }
        timeUpSent = true;
        engine.finishByTimeUp();
        connection.send(new P2PMessage.TimeUp());
    }

    /** 기권하고 연결을 끊는다. 카운트다운 중에도 기권이다. */
    @Override
    public void quit() {
        if (phase == MatchPhase.ENDED) {
            return;
        }
        connection.send(new P2PMessage.Forfeit());
        finish(MatchResult.win(opponent(), EndReason.FORFEIT, score(Side.P1), score(Side.P2)));
        afterQuit.run();
    }

    // ------------------------------------------------------------------
    // 받은 메시지
    // ------------------------------------------------------------------

    /** 상대가 보낸 메시지를 반영한다. 대전과 관계없는 메시지는 무시한다. */
    void handle(P2PMessage message) {
        switch (message) {
            case P2PMessage.Snapshot s -> {
                remote.update(s.snapshot());
                remoteListeners.forEach(GameEventListener::onBoardUpdated);
            }
            case P2PMessage.SendAttack a -> {
                if (phase == MatchPhase.PLAYING && result == null) {
                    engine.receiveAttack(a.attack());
                }
            }
            case P2PMessage.End e -> opponentEnded(e);
            case P2PMessage.TimeUp t -> timeUpReceived();
            case P2PMessage.FinalScore f -> finalScoreReceived(f.score());
            case P2PMessage.Result r -> {
                if (role == Role.CLIENT && result == null) {
                    end(r.result());
                }
            }
            case P2PMessage.Forfeit f -> {
                if (result == null) {
                    end(MatchResult.win(me(), EndReason.FORFEIT, score(Side.P1), score(Side.P2)));
                }
            }
            default -> {
                // 연결·대기 메시지는 P2P 대전 모드가 처리한다
            }
        }
    }

    /** 연결이 끊겼다. 결과가 정해지지 않았으면 결과 없이 끝낸다. */
    void connectionClosed() {
        if (phase == MatchPhase.ENDED) {
            return;
        }
        stopEngine();
        changePhase(MatchPhase.ENDED);
    }

    /** 이 PC에서 블럭을 쌓을 수 없게 됐다. */
    private void toppedOut(int finalScore) {
        if (result != null || myEnd != null || timeUpSent) {
            return;
        }
        myEnd = new P2PMessage.End(true, opponentEnd != null, finalScore);
        connection.send(myEnd);
        judge();
    }

    /** 상대의 종료 신호를 받았다. 내 게임이 아직이면 멈추고 그 사실을 담아 답한다. */
    private void opponentEnded(P2PMessage.End end) {
        if (result != null || opponentEnd != null || (role == Role.SERVER && timeUpSent)) {
            return;
        }
        opponentEnd = end;
        if (myEnd == null) {
            stopEngine();
            myEnd = new P2PMessage.End(false, true, engine.getScore());
            connection.send(myEnd);
        }
        judge();
    }

    /**
     * 서버: 양쪽 종료 신호가 모이면 승패를 정해 보낸다. (3차 팀 결정)
     *
     * <ul>
     *   <li>두 플레이어 모두 상대의 종료 신호를 받기 전에 끝났으면 엇갈림으로 무승부</li>
     *   <li>아니면 먼저 끝난 플레이어, 즉 상대의 신호를 받기 전에 블럭을 쌓을 수 없게 된 쪽의 패배</li>
     * </ul>
     *
     * <p>지금 구조에서는 상대의 신호를 받는 즉시 게임을 멈추므로 "신호를 받은 뒤에 쌓을 수 없게 됨"은 생기지 않는다.
     * 그래도 팀 결정의 규칙을 그대로 따르도록 받았는지 여부까지 보고 판정한다.
     */
    private void judge() {
        if (role != Role.SERVER || myEnd == null || opponentEnd == null || result != null) {
            return;
        }
        int p1 = myEnd.finalScore();
        int p2 = opponentEnd.finalScore();
        boolean iEndedFirst = endedFirst(myEnd);
        boolean opponentEndedFirst = endedFirst(opponentEnd);
        MatchResult decided;
        if (iEndedFirst && opponentEndedFirst) {
            decided = MatchResult.draw(EndReason.TOP_OUT, p1, p2);   // 엇갈림
        } else if (iEndedFirst) {
            decided = MatchResult.win(opponent(), EndReason.TOP_OUT, p1, p2);
        } else if (opponentEndedFirst) {
            decided = MatchResult.win(me(), EndReason.TOP_OUT, p1, p2);
        } else {
            // 둘 다 상대의 신호를 받고 멈췄다는 것은 있을 수 없는 조합이다. 어느 쪽도 지게 하지 않는다
            decided = MatchResult.draw(EndReason.TOP_OUT, p1, p2);
        }
        connection.send(new P2PMessage.Result(decided));
        end(decided);
    }

    /** 상대의 종료 신호를 받기 전에 블럭을 쌓을 수 없게 됐는지 */
    private static boolean endedFirst(P2PMessage.End end) {
        return end.toppedOut() && !end.opponentEndReceived();
    }

    /** 클라이언트: 서버의 시간 종료를 받아 게임을 마무리하고 최종 점수를 보낸다. 이미 끝났어도 점수는 보낸다. */
    private void timeUpReceived() {
        if (role != Role.CLIENT || result != null) {
            return;
        }
        engine.finishByTimeUp();
        connection.send(new P2PMessage.FinalScore(engine.getScore()));
    }

    /** 서버: 클라이언트의 최종 점수로 승패를 정해 보낸다. */
    private void finalScoreReceived(int opponentScore) {
        if (role != Role.SERVER || !timeUpSent || result != null) {
            return;
        }
        int p1 = engine.getScore();
        int p2 = opponentScore;
        MatchResult decided = p1 == p2
                ? MatchResult.draw(EndReason.TIME_UP, p1, p2)
                : MatchResult.win(p1 > p2 ? Side.P1 : Side.P2, EndReason.TIME_UP, p1, p2);
        connection.send(new P2PMessage.Result(decided));
        end(decided);
    }

    // ------------------------------------------------------------------
    // 끝내기
    // ------------------------------------------------------------------

    /** 결과를 정해 끝내고 {@code afterResult}를 부른다. */
    private void end(MatchResult decided) {
        finish(decided);
        afterResult.run();
    }

    /** 결과를 정해 끝낸다. 리스너에는 단계 변경, 결과 순서로 알린다. */
    private void finish(MatchResult decided) {
        result = decided;
        stopEngine();
        changePhase(MatchPhase.ENDED);
        for (MatchListener listener : List.copyOf(listeners)) {
            listener.onMatchEnded(decided);
        }
    }

    private void stopEngine() {
        if (!engine.isGameOver()) {
            engine.exitGame();
        }
    }

    private void changePhase(MatchPhase next) {
        phase = next;
        for (MatchListener listener : List.copyOf(listeners)) {
            listener.onPhaseChanged(next);
        }
    }

    /** 그 자리의 지금 점수. 상대 점수는 마지막으로 받은 화면 정보의 점수다. */
    private int score(Side side) {
        return side == me() ? engine.getScore() : remote.getScore();
    }

    // ------------------------------------------------------------------
    // 이 PC의 엔진
    // ------------------------------------------------------------------

    /** 내 보드가 바뀌면 화면 정보를, 공격이 생기면 공격을 보낸다. */
    private final class LocalEngineListener implements GameEventListener {

        @Override
        public void onBoardUpdated() {
            connection.send(new P2PMessage.Snapshot(BoardSnapshot.of(engine)));
        }

        @Override
        public void onAttack(Attack attack) {
            if (phase == MatchPhase.PLAYING && result == null) {
                connection.send(new P2PMessage.SendAttack(attack));
            }
        }

        @Override
        public void onGameOver(int finalScore) {
            toppedOut(finalScore);
        }

        @Override
        public void onLinesCleared(int clearedCount, int gainedScore) {
        }

        @Override
        public void onSpeedIncreased(int currentInterval) {
        }
    }

    /**
     * 이 PC의 플레이어에게 넘기는 조작 명령. 이동·회전·드롭은 진행 중일 때만 전달한다.
     * 일시정지는 없으므로 무시하고, 종료는 게임 나가기(기권)로, 재시작은 무시한다.
     */
    private final class MatchController implements GameController {

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
        }

        @Override
        public void togglePause() {
        }

        @Override
        public void pauseGame() {
        }

        @Override
        public void resumeGame() {
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
