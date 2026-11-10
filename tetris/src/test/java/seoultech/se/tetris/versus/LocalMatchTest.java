package seoultech.se.tetris.versus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seoultech.se.tetris.component.model.GarbageLineFixtures.line;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import seoultech.se.tetris.blocks.BlockFactory;
import seoultech.se.tetris.blocks.BlockType;
import seoultech.se.tetris.blocks.ItemFactory;
import seoultech.se.tetris.component.controller.GameController;
import seoultech.se.tetris.component.listener.GameEventListener;
import seoultech.se.tetris.component.model.Attack;
import seoultech.se.tetris.component.model.GameState;
import seoultech.se.tetris.engine.Board;
import seoultech.se.tetris.engine.Difficulty;
import seoultech.se.tetris.engine.GameMode;
import seoultech.se.tetris.engine.TetrisEngine;

@DisplayName("LocalMatch")
class LocalMatchTest {

    // ------------------------------------------------------------------
    // 테스트 보조
    // ------------------------------------------------------------------

    /**
     * 대전이 등록한 리스너를 붙잡아 두는 엔진. 게임 동작은 실제 엔진 그대로다.
     *
     * <p>엔진이 실제로 공격을 만들려면 보드를 정교하게 쌓아야 하므로,
     * 공격 전달은 붙잡은 리스너에 직접 신호를 보내 확인한다. 공격 줄을 만드는 규칙은 엔진 테스트에서 다룬다.
     */
    private static final class CapturingEngine extends TetrisEngine {
        final List<GameEventListener> captured = new ArrayList<>();

        CapturingEngine() {
            super(new BlockFactory(new Random(1)), new ItemFactory(new Random(1)), GameMode.NORMAL);
        }

        @Override
        public void addListener(GameEventListener listener) {
            captured.add(listener);
            super.addListener(listener);
        }

        /** 대전이 등록한 리스너에 신호를 보낸다. */
        GameEventListener matchListener() {
            return captured.get(0);
        }
    }

    /** 대전 진행을 기록하는 리스너 */
    private static final class RecordingMatchListener implements MatchListener {
        final List<MatchPhase> phases = new ArrayList<>();
        final List<MatchResult> results = new ArrayList<>();
        Map<MatchPhase, Boolean> resultVisibleAt = new EnumMap<>(MatchPhase.class);
        LocalMatch match;

        @Override
        public void onPhaseChanged(MatchPhase phase) {
            phases.add(phase);
            if (match != null) {
                resultVisibleAt.put(phase, match.result().isPresent());
            }
        }

        @Override
        public void onMatchEnded(MatchResult result) {
            results.add(result);
        }
    }

    private final CapturingEngine p1 = new CapturingEngine();
    private final CapturingEngine p2 = new CapturingEngine();
    private final RecordingMatchListener listener = new RecordingMatchListener();

    private LocalMatch matchOf(VersusMode mode) {
        LocalMatch match = new LocalMatch(mode, p1, p2);
        listener.match = match;
        match.addListener(listener);
        return match;
    }

    private LocalMatch playing(VersusMode mode) {
        LocalMatch match = matchOf(mode);
        match.begin();
        listener.phases.clear();
        return match;
    }

    private static GameController control(LocalMatch match, Side side) {
        return match.controller(side).orElseThrow();
    }

    /** 그 자리의 블럭을 계속 떨어뜨려 쌓을 수 없게 만든다. */
    private static void topOut(LocalMatch match, Side side) {
        GameController controller = control(match, side);
        GameState state = match.state(side);
        for (int i = 0; i < 200 && !state.isGameOver(); i++) {
            controller.dropToBottom();
            controller.finishClearing();
        }
    }

    /** O 블럭만 나오는 엔진. O 블럭 5개를 왼쪽부터 놓으면 아래 2줄이 꽉 찬다. */
    private static TetrisEngine onlyOEngine() {
        BlockFactory onlyO = new BlockFactory(new Random(0)) {
            @Override
            public BlockType next() {
                return BlockType.O;
            }
        };
        return new TetrisEngine(onlyO, new ItemFactory(new Random(0)), GameMode.NORMAL);
    }

    /** O 블럭 5개로 아래 2줄을 채워 줄 삭제 애니메이션을 시작한다. */
    private static void fillBottomTwoRows(GameController controller) {
        for (int col = 0; col < Board.WIDTH; col += 2) {
            for (int i = 0; i < Board.WIDTH; i++) {
                controller.moveLeft();
            }
            for (int i = 0; i < col; i++) {
                controller.moveRight();
            }
            controller.dropToBottom();
        }
    }

    private static Attack attack() {
        return new Attack(List.of(line("XXX.XXXXXX"), line("XXXX.XXXXX")));
    }

    // ------------------------------------------------------------------
    // 만들기
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("만들기")
    class Create {

        @Test
        @DisplayName("처음에는 시작 전 단계이고 결과가 없다")
        void startsReady() {
            LocalMatch match = LocalMatch.create(VersusMode.NORMAL, Difficulty.NORMAL, 1L);

            assertEquals(VersusMode.NORMAL, match.mode());
            assertEquals(MatchPhase.READY, match.phase());
            assertTrue(match.result().isEmpty());
        }

        @Test
        @DisplayName("두 자리 모두 이 PC에서 조작할 수 있고, 일시정지를 지원한다")
        void bothSidesControllable() {
            LocalMatch match = LocalMatch.create(VersusMode.NORMAL, Difficulty.NORMAL, 1L);

            assertTrue(match.controller(Side.P1).isPresent());
            assertTrue(match.controller(Side.P2).isPresent());
            assertTrue(match.supportsPause());
        }

        @Test
        @DisplayName("두 플레이어는 서로 다른 보드에서 같은 블럭 순서로 시작한다")
        void sameOrderSeparateBoards() {
            LocalMatch match = LocalMatch.create(VersusMode.ITEM, Difficulty.HARD, 42L);
            GameState first = match.state(Side.P1);
            GameState second = match.state(Side.P2);

            assertNotSame(first, second);
            assertEquals(first.getCurrentBlock().shape(), second.getCurrentBlock().shape());
            assertEquals(first.getNextBlock().shape(), second.getNextBlock().shape());
        }
    }

    // ------------------------------------------------------------------
    // 진행 단계
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("진행 단계")
    class Phases {

        @Test
        @DisplayName("카운트다운이 끝나 시작하면 진행 중이 된다")
        void beginStartsPlaying() {
            LocalMatch match = matchOf(VersusMode.NORMAL);

            match.begin();

            assertEquals(MatchPhase.PLAYING, match.phase());
            assertEquals(List.of(MatchPhase.PLAYING), listener.phases);
        }

        @Test
        @DisplayName("이미 시작한 대전에서 다시 시작해도 아무 일도 없다")
        void beginTwiceDoesNothing() {
            LocalMatch match = playing(VersusMode.NORMAL);

            match.begin();

            assertTrue(listener.phases.isEmpty());
        }

        @Test
        @DisplayName("일시정지하면 두 엔진이 모두 멈추고, 풀면 모두 다시 움직인다")
        void pauseAndResumeBoth() {
            LocalMatch match = playing(VersusMode.NORMAL);

            match.pause();
            assertEquals(MatchPhase.PAUSED, match.phase());
            assertTrue(p1.isPaused());
            assertTrue(p2.isPaused());

            match.resume();
            assertEquals(MatchPhase.PLAYING, match.phase());
            assertFalse(p1.isPaused());
            assertFalse(p2.isPaused());
            assertEquals(List.of(MatchPhase.PAUSED, MatchPhase.PLAYING), listener.phases);
        }

        @Test
        @DisplayName("줄 삭제 애니메이션 중에 일시정지하면, 애니메이션이 끝난 뒤 그 엔진도 멈춘다")
        void pauseDuringClearing() {
            TetrisEngine clearing = onlyOEngine();
            LocalMatch match = new LocalMatch(VersusMode.NORMAL, clearing, onlyOEngine());
            match.begin();
            GameController controller = control(match, Side.P1);
            fillBottomTwoRows(controller);
            assertTrue(clearing.isClearing());

            match.pause();
            controller.finishClearing();

            assertFalse(clearing.isClearing());
            assertTrue(clearing.isPaused());

            match.resume();
            assertFalse(clearing.isPaused());
        }

        @Test
        @DisplayName("시작 전에는 일시정지할 수 없다")
        void cannotPauseBeforeBegin() {
            LocalMatch match = matchOf(VersusMode.NORMAL);

            match.pause();

            assertEquals(MatchPhase.READY, match.phase());
            assertFalse(p1.isPaused());
        }

        @Test
        @DisplayName("진행 중에 나가면 결과 없이 끝나고 두 게임이 모두 멈춘다")
        void quitWhilePlaying() {
            LocalMatch match = playing(VersusMode.NORMAL);

            match.quit();

            assertEquals(MatchPhase.ENDED, match.phase());
            assertTrue(match.result().isEmpty());
            assertTrue(listener.results.isEmpty());
            assertTrue(p1.isGameOver());
            assertTrue(p2.isGameOver());
        }

        @Test
        @DisplayName("카운트다운 중이나 일시정지 중에도 나갈 수 있다")
        void quitFromReadyOrPaused() {
            LocalMatch ready = matchOf(VersusMode.NORMAL);
            ready.quit();
            assertEquals(MatchPhase.ENDED, ready.phase());

            LocalMatch paused = new LocalMatch(VersusMode.NORMAL, new CapturingEngine(), new CapturingEngine());
            paused.begin();
            paused.pause();
            paused.quit();
            assertEquals(MatchPhase.ENDED, paused.phase());
        }

        @Test
        @DisplayName("끝난 대전에서 나가도 아무 일도 없다")
        void quitAfterEnd() {
            LocalMatch match = playing(VersusMode.NORMAL);
            match.quit();
            listener.phases.clear();

            match.quit();

            assertTrue(listener.phases.isEmpty());
        }
    }

    // ------------------------------------------------------------------
    // 조작 명령
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("조작 명령")
    class Controls {

        @Test
        @DisplayName("시작 전에는 이동·회전·드롭이 엔진에 전달되지 않는다")
        void ignoredBeforeBegin() {
            LocalMatch match = matchOf(VersusMode.NORMAL);
            int x = p1.getCurrentX();
            int y = p1.getCurrentY();

            GameController controller = control(match, Side.P1);
            controller.moveLeft();
            controller.moveDown();
            controller.dropToBottom();

            assertEquals(x, p1.getCurrentX());
            assertEquals(y, p1.getCurrentY());
            assertEquals(0, p1.getScore());
        }

        @Test
        @DisplayName("진행 중에는 그 자리의 엔진에만 전달된다")
        void forwardedWhilePlaying() {
            LocalMatch match = playing(VersusMode.NORMAL);
            int x = p2.getCurrentX();

            control(match, Side.P1).moveLeft();

            assertEquals(x - 1, p1.getCurrentX());
            assertEquals(x, p2.getCurrentX());
        }

        @Test
        @DisplayName("일시정지 중에는 전달되지 않는다")
        void ignoredWhilePaused() {
            LocalMatch match = playing(VersusMode.NORMAL);
            match.pause();
            int y = p1.getCurrentY();

            control(match, Side.P1).moveDown();

            assertEquals(y, p1.getCurrentY());
        }

        @Test
        @DisplayName("한 자리에서 일시정지하면 대전 전체가 일시정지되고, 다시 누르면 풀린다")
        void pauseKeyAppliesToMatch() {
            LocalMatch match = playing(VersusMode.NORMAL);

            control(match, Side.P2).togglePause();
            assertEquals(MatchPhase.PAUSED, match.phase());
            assertTrue(p1.isPaused());

            control(match, Side.P1).togglePause();
            assertEquals(MatchPhase.PLAYING, match.phase());
        }

        @Test
        @DisplayName("한 자리에서 종료하면 대전에서 나간다")
        void exitKeyQuitsMatch() {
            LocalMatch match = playing(VersusMode.NORMAL);

            control(match, Side.P1).exitGame();

            assertEquals(MatchPhase.ENDED, match.phase());
            assertTrue(p2.isGameOver());
        }

        @Test
        @DisplayName("한 자리만 다시 시작할 수는 없다")
        void restartIgnored() {
            LocalMatch match = playing(VersusMode.NORMAL);
            control(match, Side.P1).dropToBottom();
            int score = p1.getScore();

            control(match, Side.P1).restartGame();

            assertEquals(score, p1.getScore());
            assertEquals(MatchPhase.PLAYING, match.phase());
        }
    }

    // ------------------------------------------------------------------
    // 게임 이벤트 리스너
    // ------------------------------------------------------------------

    /** 한 자리의 게임 이벤트를 기록하는 리스너 */
    private static final class RecordingGameListener implements GameEventListener {
        int boardUpdated;
        MatchPhase phaseAtGameOver;
        boolean resultAtGameOver;
        LocalMatch match;

        @Override public void onBoardUpdated() { boardUpdated++; }
        @Override public void onLinesCleared(int clearedCount, int gainedScore) { }
        @Override public void onSpeedIncreased(int currentInterval) { }

        @Override
        public void onGameOver(int finalScore) {
            phaseAtGameOver = match.phase();
            resultAtGameOver = match.result().isPresent();
        }
    }

    @Nested
    @DisplayName("게임 이벤트 리스너")
    class GameListeners {

        @Test
        @DisplayName("등록한 리스너는 그 자리의 이벤트만 받는다")
        void receivesOnlyOwnSide() {
            LocalMatch match = playing(VersusMode.NORMAL);
            RecordingGameListener first = new RecordingGameListener();
            RecordingGameListener second = new RecordingGameListener();
            match.addGameListener(Side.P1, first);
            match.addGameListener(Side.P2, second);

            control(match, Side.P1).moveLeft();

            assertTrue(first.boardUpdated > 0);
            assertEquals(0, second.boardUpdated);
        }

        @Test
        @DisplayName("상대의 공격을 받으면 대기 영역을 다시 그리도록 보드 변화를 받는다")
        void notifiedWhenAttacked() {
            LocalMatch match = playing(VersusMode.NORMAL);
            RecordingGameListener defender = new RecordingGameListener();
            match.addGameListener(Side.P2, defender);

            p1.matchListener().onAttack(attack());

            assertTrue(defender.boardUpdated > 0);
        }

        @Test
        @DisplayName("해제한 리스너는 더 이상 받지 않는다")
        void removed() {
            LocalMatch match = playing(VersusMode.NORMAL);
            RecordingGameListener gameListener = new RecordingGameListener();
            match.addGameListener(Side.P1, gameListener);
            match.removeGameListener(Side.P1, gameListener);

            control(match, Side.P1).moveLeft();

            assertEquals(0, gameListener.boardUpdated);
        }

        @Test
        @DisplayName("게임 오버를 받을 때는 이미 대전이 끝나 결과가 정해져 있다")
        void matchEndedBeforeGameOver() {
            LocalMatch match = playing(VersusMode.NORMAL);
            RecordingGameListener gameListener = new RecordingGameListener();
            gameListener.match = match;
            match.addGameListener(Side.P2, gameListener);

            topOut(match, Side.P2);

            assertEquals(MatchPhase.ENDED, gameListener.phaseAtGameOver);
            assertTrue(gameListener.resultAtGameOver);
        }
    }

    // ------------------------------------------------------------------
    // 공격
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("공격")
    class Attacks {

        @Test
        @DisplayName("한쪽이 보낸 공격은 상대의 대기 줄에 들어간다")
        void sentToOpponent() {
            playing(VersusMode.NORMAL);
            Attack attack = attack();

            p1.matchListener().onAttack(attack);

            assertEquals(attack.lines(), p2.getPendingGarbage());
            assertTrue(p1.getPendingGarbage().isEmpty());
        }

        @Test
        @DisplayName("P2가 보낸 공격은 P1에게 간다")
        void p2AttacksP1() {
            playing(VersusMode.NORMAL);

            p2.matchListener().onAttack(attack());

            assertEquals(2, p1.getPendingGarbage().size());
        }

        @Test
        @DisplayName("끝난 대전에서는 공격을 전달하지 않는다")
        void notSentAfterEnd() {
            LocalMatch match = playing(VersusMode.NORMAL);
            match.quit();

            p1.matchListener().onAttack(attack());

            assertTrue(p2.getPendingGarbage().isEmpty());
        }
    }

    // ------------------------------------------------------------------
    // 승패
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("승패")
    class Results {

        @Test
        @DisplayName("한쪽이 블럭을 쌓지 못하면 상대가 이기고 두 게임이 모두 멈춘다")
        void topOutLoses() {
            LocalMatch match = playing(VersusMode.NORMAL);

            topOut(match, Side.P2);

            MatchResult result = match.result().orElseThrow();
            assertEquals(Side.P1, result.winner());
            assertEquals(EndReason.TOP_OUT, result.reason());
            assertEquals(p2.getScore(), result.score(Side.P2));
            assertTrue(p1.isGameOver());
            assertEquals(MatchPhase.ENDED, match.phase());
        }

        @Test
        @DisplayName("끝나면 단계 변경과 결과를 한 번씩 알리고, 단계 변경을 받을 때 이미 결과를 볼 수 있다")
        void notifiesOnce() {
            LocalMatch match = playing(VersusMode.NORMAL);

            topOut(match, Side.P1);

            assertEquals(List.of(MatchPhase.ENDED), listener.phases);
            assertEquals(1, listener.results.size());
            assertEquals(Side.P2, listener.results.get(0).winner());
            assertTrue(listener.resultVisibleAt.get(MatchPhase.ENDED));
        }

        @Test
        @DisplayName("이미 끝난 뒤에 들어온 게임 오버는 결과를 바꾸지 않는다")
        void laterGameOverIgnored() {
            LocalMatch match = playing(VersusMode.NORMAL);
            topOut(match, Side.P2);

            p1.matchListener().onGameOver(0);

            assertEquals(Side.P1, match.result().orElseThrow().winner());
            assertEquals(1, listener.results.size());
        }

        @Test
        @DisplayName("시간제한 모드에서 시간이 끝나면 점수가 높은 쪽이 이긴다")
        void timeUpHigherScoreWins() {
            LocalMatch match = playing(VersusMode.TIME_LIMIT);
            control(match, Side.P2).dropToBottom();   // 드롭 점수로 P2가 앞선다

            match.timeUp();

            MatchResult result = match.result().orElseThrow();
            assertEquals(Side.P2, result.winner());
            assertEquals(EndReason.TIME_UP, result.reason());
            assertTrue(result.score(Side.P2) > result.score(Side.P1));
            assertTrue(p1.isGameOver());
            assertTrue(p2.isGameOver());
        }

        @Test
        @DisplayName("시간이 끝났을 때 점수가 같으면 무승부다")
        void timeUpSameScoreDraw() {
            LocalMatch match = playing(VersusMode.TIME_LIMIT);

            match.timeUp();

            MatchResult result = match.result().orElseThrow();
            assertTrue(result.isDraw());
            assertEquals(EndReason.TIME_UP, result.reason());
        }

        @Test
        @DisplayName("시간 종료는 시간제한 모드가 아니거나 진행 중이 아니면 무시한다")
        void timeUpIgnoredOtherwise() {
            LocalMatch normal = playing(VersusMode.NORMAL);
            normal.timeUp();
            assertEquals(MatchPhase.PLAYING, normal.phase());

            LocalMatch paused = new LocalMatch(VersusMode.TIME_LIMIT, new CapturingEngine(), new CapturingEngine());
            paused.begin();
            paused.pause();
            paused.timeUp();
            assertEquals(MatchPhase.PAUSED, paused.phase());
        }
    }
}
