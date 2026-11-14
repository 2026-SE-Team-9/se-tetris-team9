package seoultech.se.tetris.versus.p2p;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seoultech.se.tetris.component.model.GarbageLineFixtures.line;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import seoultech.se.tetris.blocks.BlockFactory;
import seoultech.se.tetris.blocks.ItemFactory;
import seoultech.se.tetris.component.controller.GameController;
import seoultech.se.tetris.component.listener.GameEventListener;
import seoultech.se.tetris.component.model.Attack;
import seoultech.se.tetris.engine.Difficulty;
import seoultech.se.tetris.engine.GameMode;
import seoultech.se.tetris.engine.TetrisEngine;
import seoultech.se.tetris.versus.BoardSnapshot;
import seoultech.se.tetris.versus.EndReason;
import seoultech.se.tetris.versus.MatchListener;
import seoultech.se.tetris.versus.MatchPhase;
import seoultech.se.tetris.versus.MatchResult;
import seoultech.se.tetris.versus.Side;
import seoultech.se.tetris.versus.VersusMode;

@DisplayName("P2PMatch")
class P2PMatchTest {

    // ------------------------------------------------------------------
    // 테스트 보조
    // ------------------------------------------------------------------

    /** 보낸 메시지를 모아 두는 통로. 상대에게는 {@link Link#pump()}로 전달한다. */
    private static final class QueueConnection implements PeerConnection {
        final Role role;
        final List<P2PMessage> outbox = new ArrayList<>();
        final List<P2PMessage> sent = new ArrayList<>();

        QueueConnection(Role role) {
            this.role = role;
        }

        @Override public Role role() { return role; }
        @Override public void send(P2PMessage message) { outbox.add(message); sent.add(message); }
        @Override public void addListener(PeerListener listener) { }
        @Override public void removeListener(PeerListener listener) { }
        @Override public ConnectionState state() { return ConnectionState.NORMAL; }
        @Override public int roundTripMillis() { return 0; }
        @Override public void close() { }

        /** 화면 정보를 뺀, 보낸 메시지 */
        List<P2PMessage> sentExceptSnapshots() {
            return sent.stream().filter(m -> !(m instanceof P2PMessage.Snapshot)).toList();
        }
    }

    /** 서버와 클라이언트 대전을 이어 준다. 메시지는 {@link #pump()}를 불러야 상대에게 간다. */
    private static final class Link {
        final QueueConnection serverConnection = new QueueConnection(Role.SERVER);
        final QueueConnection clientConnection = new QueueConnection(Role.CLIENT);
        final P2PMatch server;
        final P2PMatch client;
        int serverQuits;
        int clientQuits;
        int serverResults;
        int clientResults;

        Link(VersusMode mode) {
            P2PMessage.Start start = new P2PMessage.Start(mode, Difficulty.NORMAL, 3L);
            server = P2PMatch.create(Role.SERVER, start, serverConnection, () -> serverQuits++, () -> serverResults++);
            client = P2PMatch.create(Role.CLIENT, start, clientConnection, () -> clientQuits++, () -> clientResults++);
        }

        /** 오갈 메시지가 없을 때까지 서로에게 전달한다. 실제 네트워크처럼 한쪽 메시지를 다 전달한 뒤 반대쪽으로 넘어간다. */
        void pump() {
            for (int round = 0; round < 100; round++) {
                if (serverConnection.outbox.isEmpty() && clientConnection.outbox.isEmpty()) {
                    return;
                }
                deliver(serverConnection, client);
                deliver(clientConnection, server);
            }
            throw new AssertionError("메시지가 끝없이 오갑니다");
        }

        private static void deliver(QueueConnection from, P2PMatch to) {
            List<P2PMessage> messages = List.copyOf(from.outbox);
            from.outbox.clear();
            messages.forEach(to::handle);
        }

        void beginBoth() {
            server.begin();
            client.begin();
        }
    }

    /** 대전 진행을 기록한다. */
    private static final class RecordingMatchListener implements MatchListener {
        final List<Object> events = new ArrayList<>();

        @Override public void onPhaseChanged(MatchPhase phase) { events.add(phase); }
        @Override public void onMatchEnded(MatchResult result) { events.add(result); }
    }

    /** 그 대전의 내 자리 블럭을 계속 떨어뜨려 쌓을 수 없게 만든다. 메시지는 전달하지 않는다. */
    private static void topOut(P2PMatch match, Side side) {
        GameController controller = match.controller(side).orElseThrow();
        for (int i = 0; i < 200 && !match.state(side).isGameOver(); i++) {
            controller.dropToBottom();
            controller.finishClearing();
        }
        assertTrue(match.state(side).isGameOver());
    }

    private static Attack attack() {
        return new Attack(List.of(line("XXX.XXXXXX"), line("XXXX.XXXXX")));
    }

    private static MatchResult resultOf(P2PMatch match) {
        return match.result().orElseThrow();
    }

    // ------------------------------------------------------------------
    // 만들기와 조회
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("만들기와 조회")
    class Basics {

        @Test
        @DisplayName("처음에는 시작 전 단계이고, 내 첫 화면 정보를 상대에게 보낸다")
        void startsReadyAndSendsBoard() {
            Link link = new Link(VersusMode.NORMAL);

            assertEquals(MatchPhase.READY, link.server.phase());
            assertTrue(link.serverConnection.sent.get(0) instanceof P2PMessage.Snapshot);
        }

        @Test
        @DisplayName("서버는 P1, 클라이언트는 P2 자리이고, 내 자리만 조작할 수 있다")
        void sidesAndControllers() {
            Link link = new Link(VersusMode.NORMAL);

            assertTrue(link.server.controller(Side.P1).isPresent());
            assertTrue(link.server.controller(Side.P2).isEmpty());
            assertTrue(link.client.controller(Side.P2).isPresent());
            assertTrue(link.client.controller(Side.P1).isEmpty());
        }

        @Test
        @DisplayName("상대 화면 정보를 받기 전에는 같은 시드의 시작 상태를 보여준다")
        void opponentStartsLikeMe() {
            Link link = new Link(VersusMode.NORMAL);

            assertEquals(link.server.state(Side.P1).getCurrentBlock(), link.server.state(Side.P2).getCurrentBlock());
            assertEquals(link.server.state(Side.P1).getNextBlock(), link.server.state(Side.P2).getNextBlock());
        }

        @Test
        @DisplayName("일시정지는 지원하지 않는다")
        void noPause() {
            Link link = new Link(VersusMode.NORMAL);
            link.beginBoth();

            assertFalse(link.server.supportsPause());
            assertThrows(UnsupportedOperationException.class, link.server::pause);
            link.server.resume();
            link.server.controller(Side.P1).orElseThrow().togglePause();
            assertEquals(MatchPhase.PLAYING, link.server.phase());
            assertFalse(link.server.state(Side.P1).isPaused());
        }

        @Test
        @DisplayName("시작 전에는 조작이 전달되지 않고, 시작하면 전달된다")
        void controlsGatedByPhase() {
            Link link = new Link(VersusMode.NORMAL);
            GameController controller = link.server.controller(Side.P1).orElseThrow();
            int x = link.server.state(Side.P1).getCurrentX();

            controller.moveLeft();
            assertEquals(x, link.server.state(Side.P1).getCurrentX());

            link.server.begin();
            controller.moveLeft();
            assertEquals(x - 1, link.server.state(Side.P1).getCurrentX());
        }
    }

    // ------------------------------------------------------------------
    // 조작 명령
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("조작 명령")
    class Controls {

        @Test
        @DisplayName("시작 전에는 어떤 이동·회전 명령도 엔진에 전달되지 않는다")
        void nothingBeforeBegin() {
            Link link = new Link(VersusMode.NORMAL);
            GameController controller = link.server.controller(Side.P1).orElseThrow();
            BoardSnapshot before = BoardSnapshot.of(link.server.state(Side.P1));

            controller.moveLeft();
            controller.moveRight();
            controller.moveDown();
            controller.rotate();
            controller.rotateCounterclockwise();
            controller.dropToBottom();

            assertEquals(before, BoardSnapshot.of(link.server.state(Side.P1)));
        }

        @Test
        @DisplayName("진행 중에는 이동·회전 명령이 모두 엔진에 전달된다")
        void everyMoveWhilePlaying() {
            Link link = new Link(VersusMode.NORMAL);   // 시드 3: 첫 블럭은 J
            link.server.begin();
            GameController controller = link.server.controller(Side.P1).orElseThrow();
            var state = link.server.state(Side.P1);
            controller.moveDown();   // 회전할 공간을 만든다
            int x = state.getCurrentX();
            int y = state.getCurrentY();

            controller.moveRight();
            assertEquals(x + 1, state.getCurrentX());

            controller.moveDown();
            assertEquals(y + 1, state.getCurrentY());

            controller.rotate();
            assertEquals(1, state.getCurrentBlock().rotation());

            controller.rotateCounterclockwise();
            assertEquals(0, state.getCurrentBlock().rotation());
        }

        @Test
        @DisplayName("일시정지·재개·재시작 명령은 아무 일도 하지 않는다")
        void pauseAndRestartDoNothing() {
            Link link = new Link(VersusMode.NORMAL);
            link.server.begin();
            GameController controller = link.server.controller(Side.P1).orElseThrow();
            controller.dropToBottom();
            BoardSnapshot before = BoardSnapshot.of(link.server.state(Side.P1));

            controller.pauseGame();
            controller.resumeGame();
            controller.togglePause();
            controller.restartGame();

            assertEquals(before, BoardSnapshot.of(link.server.state(Side.P1)));
            assertEquals(MatchPhase.PLAYING, link.server.phase());
        }
    }

    // ------------------------------------------------------------------
    // 화면 정보와 공격
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("화면 정보와 공격")
    class Exchange {

        @Test
        @DisplayName("내 보드가 바뀌면 바뀐 화면 정보를 보낸다")
        void sendsSnapshotOnChange() {
            Link link = new Link(VersusMode.NORMAL);
            link.beginBoth();

            link.server.controller(Side.P1).orElseThrow().moveLeft();

            P2PMessage last = link.serverConnection.sent.get(link.serverConnection.sent.size() - 1);
            assertEquals(new P2PMessage.Snapshot(BoardSnapshot.of(link.server.state(Side.P1))), last);
        }

        @Test
        @DisplayName("받은 화면 정보로 상대 자리를 바꾸고, 상대 자리의 리스너에 알린다")
        void showsOpponentSnapshot() {
            Link link = new Link(VersusMode.NORMAL);
            link.beginBoth();
            int[] updates = new int[1];
            link.server.addGameListener(Side.P2, new GameEventListener() {
                @Override public void onBoardUpdated() { updates[0]++; }
                @Override public void onLinesCleared(int clearedCount, int gainedScore) { }
                @Override public void onSpeedIncreased(int currentInterval) { }
                @Override public void onGameOver(int finalScore) { }
            });

            link.client.controller(Side.P2).orElseThrow().dropToBottom();
            link.pump();

            assertEquals(link.client.state(Side.P2).getScore(), link.server.state(Side.P2).getScore());
            assertEquals(BoardSnapshot.of(link.client.state(Side.P2)), BoardSnapshot.of(link.server.state(Side.P2)));
            assertTrue(updates[0] > 0);
        }

        @Test
        @DisplayName("받은 공격은 내 공격 대기 줄에 들어간다")
        void receivesAttack() {
            Link link = new Link(VersusMode.NORMAL);
            link.beginBoth();

            link.server.handle(new P2PMessage.SendAttack(attack()));

            assertEquals(attack().lines(), link.server.state(Side.P1).getPendingGarbage());
        }

        @Test
        @DisplayName("시작 전이나 끝난 뒤에 받은 공격은 무시한다")
        void ignoresAttackOutsidePlay() {
            Link link = new Link(VersusMode.NORMAL);
            link.server.handle(new P2PMessage.SendAttack(attack()));
            assertTrue(link.server.state(Side.P1).getPendingGarbage().isEmpty());

            link.beginBoth();
            link.server.connectionClosed();
            link.server.handle(new P2PMessage.SendAttack(attack()));
            assertTrue(link.server.state(Side.P1).getPendingGarbage().isEmpty());
        }

        @Test
        @DisplayName("내 엔진에서 공격이 생기면 상대에게 보낸다")
        void sendsAttack() {
            CapturingEngine engine = new CapturingEngine();
            QueueConnection connection = new QueueConnection(Role.SERVER);
            P2PMatch match = new P2PMatch(Role.SERVER, VersusMode.NORMAL, engine,
                    BoardSnapshot.of(new CapturingEngine()), connection, () -> { }, () -> { });

            engine.matchListener().onAttack(attack());   // 시작 전: 보내지 않는다
            match.begin();
            engine.matchListener().onAttack(attack());

            assertEquals(List.of(new P2PMessage.SendAttack(attack())), connection.sentExceptSnapshots());
        }
    }

    /** 대전이 등록한 리스너를 붙잡아 두는 엔진 */
    private static final class CapturingEngine extends TetrisEngine {
        private final List<GameEventListener> captured = new ArrayList<>();

        CapturingEngine() {
            super(new BlockFactory(new Random(1)), new ItemFactory(new Random(1)), GameMode.NORMAL);
        }

        @Override
        public void addListener(GameEventListener listener) {
            captured.add(listener);
            super.addListener(listener);
        }

        GameEventListener matchListener() {
            return captured.get(0);
        }
    }

    // ------------------------------------------------------------------
    // 리스너 등록과 해제
    // ------------------------------------------------------------------

    /** 보드 변화 횟수만 세는 게임 리스너 */
    private static final class CountingGameListener implements GameEventListener {
        int boardUpdated;

        @Override public void onBoardUpdated() { boardUpdated++; }
        @Override public void onLinesCleared(int clearedCount, int gainedScore) { }
        @Override public void onSpeedIncreased(int currentInterval) { }
        @Override public void onGameOver(int finalScore) { }
    }

    @Nested
    @DisplayName("리스너 등록과 해제")
    class Listeners {

        @Test
        @DisplayName("내 자리에 등록한 리스너는 내 엔진의 변화를 받는다")
        void mySideListener() {
            Link link = new Link(VersusMode.NORMAL);
            link.server.begin();
            CountingGameListener mine = new CountingGameListener();
            link.server.addGameListener(Side.P1, mine);

            link.server.controller(Side.P1).orElseThrow().moveLeft();

            assertTrue(mine.boardUpdated > 0);
        }

        @Test
        @DisplayName("해제한 게임 리스너는 내 자리든 상대 자리든 더 받지 않는다")
        void removeGameListeners() {
            Link link = new Link(VersusMode.NORMAL);
            link.beginBoth();
            CountingGameListener mine = new CountingGameListener();
            CountingGameListener theirs = new CountingGameListener();
            link.server.addGameListener(Side.P1, mine);
            link.server.addGameListener(Side.P2, theirs);
            link.server.removeGameListener(Side.P1, mine);
            link.server.removeGameListener(Side.P2, theirs);

            link.server.controller(Side.P1).orElseThrow().moveLeft();
            link.client.controller(Side.P2).orElseThrow().moveLeft();
            link.pump();

            assertEquals(0, mine.boardUpdated);
            assertEquals(0, theirs.boardUpdated);
        }

        @Test
        @DisplayName("해제한 대전 리스너는 결과를 받지 않는다")
        void removeMatchListener() {
            Link link = new Link(VersusMode.NORMAL);
            link.beginBoth();
            RecordingMatchListener listener = new RecordingMatchListener();
            link.server.addListener(listener);
            link.server.removeListener(listener);

            link.server.quit();

            assertTrue(listener.events.isEmpty());
        }
    }

    // ------------------------------------------------------------------
    // 블럭을 쌓을 수 없게 됐을 때
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("블럭을 쌓을 수 없게 됐을 때")
    class TopOut {

        @Test
        @DisplayName("클라이언트가 먼저 끝나면 서버는 게임을 멈추고 서버의 승리로 정해 보낸다")
        void clientFirst() {
            Link link = new Link(VersusMode.NORMAL);
            link.beginBoth();

            topOut(link.client, Side.P2);
            link.pump();

            MatchResult result = resultOf(link.server);
            assertEquals(Side.P1, result.winner());
            assertEquals(EndReason.TOP_OUT, result.reason());
            assertEquals(result, resultOf(link.client));
            assertTrue(link.server.state(Side.P1).isGameOver());
            assertEquals(MatchPhase.ENDED, link.client.phase());
        }

        @Test
        @DisplayName("서버가 먼저 끝나면 클라이언트가 이긴다")
        void serverFirst() {
            Link link = new Link(VersusMode.NORMAL);
            link.beginBoth();

            topOut(link.server, Side.P1);
            assertTrue(link.server.result().isEmpty());   // 클라이언트의 답을 기다린다
            link.pump();

            assertEquals(Side.P2, resultOf(link.server).winner());
            assertEquals(resultOf(link.server), resultOf(link.client));
        }

        @Test
        @DisplayName("상대의 종료 신호를 받고 멈춘 쪽은 그 사실을 담아 답한다")
        void stoppedSideAnswers() {
            Link link = new Link(VersusMode.NORMAL);
            link.beginBoth();

            topOut(link.server, Side.P1);
            link.pump();

            P2PMessage.End answer = (P2PMessage.End) link.clientConnection.sentExceptSnapshots().get(0);
            assertFalse(answer.toppedOut());
            assertTrue(answer.opponentEndReceived());
            assertEquals(link.client.state(Side.P2).getScore(), answer.finalScore());
        }

        @Test
        @DisplayName("양쪽이 서로의 신호를 받기 전에 끝나면 엇갈림으로 무승부다")
        void crossingIsDraw() {
            Link link = new Link(VersusMode.NORMAL);
            link.beginBoth();

            topOut(link.server, Side.P1);
            topOut(link.client, Side.P2);   // 서버의 신호가 아직 도착하지 않았다
            link.pump();

            assertTrue(resultOf(link.server).isDraw());
            assertEquals(EndReason.TOP_OUT, resultOf(link.server).reason());
            assertTrue(resultOf(link.client).isDraw());
        }

        @Test
        @DisplayName("상대가 내 신호를 받은 뒤에 끝났다면 엇갈림이 아니라 먼저 끝난 내가 진다")
        void endedAfterReceivingIsNotCrossing() {
            Link link = new Link(VersusMode.NORMAL);
            link.beginBoth();
            topOut(link.server, Side.P1);   // 서버가 먼저 끝났다

            // 클라이언트가 "서버의 신호를 받은 뒤에 쌓을 수 없게 됐다"고 알려 온다
            link.server.handle(new P2PMessage.End(true, true, 0));

            assertEquals(Side.P2, resultOf(link.server).winner());
            assertEquals(EndReason.TOP_OUT, resultOf(link.server).reason());
        }

        @Test
        @DisplayName("둘 다 상대의 신호를 받고 멈췄다는 있을 수 없는 조합이면 어느 쪽도 지지 않는다")
        void impossibleCombinationIsDraw() {
            Link link = new Link(VersusMode.NORMAL);
            link.beginBoth();
            link.server.handle(new P2PMessage.End(true, false, 0));   // 서버는 멈추고 "받고 멈춤"으로 답한다

            assertEquals(Side.P1, resultOf(link.server).winner());     // 정상 조합: 클라이언트가 먼저 끝남

            Link odd = new Link(VersusMode.NORMAL);
            odd.beginBoth();
            odd.server.handle(new P2PMessage.End(false, true, 0));     // 클라이언트도 "받고 멈춤"이라고 한다

            assertTrue(resultOf(odd.server).isDraw());
        }

        @Test
        @DisplayName("결과의 점수는 양쪽 종료 신호에 담긴 최종 점수다")
        void resultScores() {
            Link link = new Link(VersusMode.NORMAL);
            link.beginBoth();
            link.client.controller(Side.P2).orElseThrow().dropToBottom();

            topOut(link.server, Side.P1);
            link.pump();

            MatchResult result = resultOf(link.client);
            assertEquals(link.server.state(Side.P1).getScore(), result.score(Side.P1));
            assertEquals(link.client.state(Side.P2).getScore(), result.score(Side.P2));
        }

        @Test
        @DisplayName("끝나면 단계 변경과 결과를 한 번씩 알린다")
        void notifiesOnce() {
            Link link = new Link(VersusMode.NORMAL);
            link.beginBoth();
            RecordingMatchListener listener = new RecordingMatchListener();
            link.client.addListener(listener);

            topOut(link.client, Side.P2);
            link.pump();

            assertEquals(List.of(MatchPhase.ENDED, resultOf(link.client)), listener.events);
        }

        @Test
        @DisplayName("결과가 정해지면 리스너에 알린 뒤 양쪽에서 대기로 돌아갈 일을 한 번 부른다")
        void callsAfterResult() {
            Link link = new Link(VersusMode.NORMAL);
            link.beginBoth();
            List<Object> order = new ArrayList<>();
            link.client.addListener(new MatchListener() {
                @Override public void onMatchEnded(MatchResult result) { order.add("결과 알림:" + link.clientResults); }
            });

            topOut(link.client, Side.P2);
            link.pump();

            assertEquals(1, link.serverResults);
            assertEquals(1, link.clientResults);
            assertEquals(List.of("결과 알림:0"), order);   // 리스너가 먼저 받았다
        }
    }

    // ------------------------------------------------------------------
    // 제한 시간
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("제한 시간")
    class TimeLimit {

        @Test
        @DisplayName("서버의 시간이 끝나면 클라이언트의 최종 점수를 받아 점수가 높은 쪽의 승리로 정한다")
        void higherScoreWins() {
            Link link = new Link(VersusMode.TIME_LIMIT);
            link.beginBoth();
            link.client.controller(Side.P2).orElseThrow().dropToBottom();   // 드롭 점수로 클라이언트가 앞선다

            link.server.timeUp();
            link.pump();

            MatchResult result = resultOf(link.server);
            assertEquals(Side.P2, result.winner());
            assertEquals(EndReason.TIME_UP, result.reason());
            assertEquals(result, resultOf(link.client));
            assertTrue(link.client.state(Side.P2).isGameOver());
        }

        @Test
        @DisplayName("점수가 같으면 무승부다")
        void sameScoreDraw() {
            Link link = new Link(VersusMode.TIME_LIMIT);
            link.beginBoth();

            link.server.timeUp();
            link.pump();

            assertTrue(resultOf(link.client).isDraw());
            assertEquals(EndReason.TIME_UP, resultOf(link.client).reason());
        }

        @Test
        @DisplayName("클라이언트에서 부른 시간 종료와 시간제한 모드가 아닌 대전의 시간 종료는 무시한다")
        void ignoredOtherwise() {
            Link timeLimit = new Link(VersusMode.TIME_LIMIT);
            timeLimit.beginBoth();
            timeLimit.client.timeUp();
            assertTrue(timeLimit.clientConnection.sentExceptSnapshots().isEmpty());

            Link normal = new Link(VersusMode.NORMAL);
            normal.beginBoth();
            normal.server.timeUp();
            assertTrue(normal.serverConnection.sentExceptSnapshots().isEmpty());
        }

        @Test
        @DisplayName("서버가 시간 종료를 먼저 처리했으면, 뒤에 도착한 클라이언트의 종료 신호는 쓰지 않고 점수로 정한다")
        void timeUpBeforeTopOut() {
            Link link = new Link(VersusMode.TIME_LIMIT);
            link.beginBoth();

            link.server.timeUp();
            topOut(link.client, Side.P2);   // 클라이언트는 시간 종료 신호를 받기 전에 끝났다
            link.pump();

            assertEquals(EndReason.TIME_UP, resultOf(link.server).reason());
            assertEquals(resultOf(link.server), resultOf(link.client));
        }

        @Test
        @DisplayName("서버가 종료 신호를 먼저 처리했으면, 그 뒤의 시간 종료는 무시한다")
        void topOutBeforeTimeUp() {
            Link link = new Link(VersusMode.TIME_LIMIT);
            link.beginBoth();

            topOut(link.server, Side.P1);
            link.server.timeUp();
            link.pump();

            assertEquals(EndReason.TOP_OUT, resultOf(link.server).reason());
            assertEquals(Side.P2, resultOf(link.client).winner());
        }
    }

    // ------------------------------------------------------------------
    // 엉뚱한 쪽에서 온 메시지
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("엉뚱한 쪽에서 온 메시지")
    class WrongSender {

        @Test
        @DisplayName("서버는 시간 종료 신호를 받아도 무시한다 (서버 측 시간 기준)")
        void serverIgnoresTimeUp() {
            Link link = new Link(VersusMode.TIME_LIMIT);
            link.beginBoth();

            link.server.handle(new P2PMessage.TimeUp());

            assertFalse(link.server.state(Side.P1).isGameOver());
            assertTrue(link.serverConnection.sentExceptSnapshots().isEmpty());
        }

        @Test
        @DisplayName("서버는 시간 종료를 처리하기 전에 온 최종 점수를 무시한다")
        void serverIgnoresEarlyFinalScore() {
            Link link = new Link(VersusMode.TIME_LIMIT);
            link.beginBoth();

            link.server.handle(new P2PMessage.FinalScore(100));

            assertTrue(link.server.result().isEmpty());
        }

        @Test
        @DisplayName("클라이언트는 최종 점수를 받아도 무시한다")
        void clientIgnoresFinalScore() {
            Link link = new Link(VersusMode.TIME_LIMIT);
            link.beginBoth();

            link.client.handle(new P2PMessage.FinalScore(100));

            assertTrue(link.client.result().isEmpty());
            assertTrue(link.clientConnection.sentExceptSnapshots().isEmpty());
        }
    }

    // ------------------------------------------------------------------
    // 나가기와 연결 끊김
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("나가기와 연결 끊김")
    class Leaving {

        @Test
        @DisplayName("나가면 기권패로 끝나고 연결을 끊으며, 상대에게는 기권승이 된다")
        void quitIsForfeit() {
            Link link = new Link(VersusMode.NORMAL);
            link.beginBoth();

            link.client.quit();

            assertEquals(new MatchResult(Side.P1, EndReason.FORFEIT,
                    resultOf(link.client).score(Side.P1), resultOf(link.client).score(Side.P2)), resultOf(link.client));
            assertEquals(1, link.clientQuits);
            assertEquals(0, link.clientResults);   // 나간 쪽은 대기로 돌아가지 않는다
            assertTrue(link.clientConnection.sentExceptSnapshots().contains(new P2PMessage.Forfeit()));

            link.pump();
            assertEquals(Side.P1, resultOf(link.server).winner());
            assertEquals(EndReason.FORFEIT, resultOf(link.server).reason());
            assertEquals(1, link.serverResults);
        }

        @Test
        @DisplayName("카운트다운 중에 나가도 기권이다")
        void quitDuringCountdown() {
            Link link = new Link(VersusMode.NORMAL);

            link.server.quit();

            assertEquals(EndReason.FORFEIT, resultOf(link.server).reason());
            assertEquals(1, link.serverQuits);
        }

        @Test
        @DisplayName("조작의 종료 명령도 나가기다")
        void exitKeyQuits() {
            Link link = new Link(VersusMode.NORMAL);
            link.beginBoth();

            link.server.controller(Side.P1).orElseThrow().exitGame();

            assertEquals(1, link.serverQuits);
            assertEquals(MatchPhase.ENDED, link.server.phase());
        }

        @Test
        @DisplayName("끝난 대전에서 나가도 아무 일도 없다")
        void quitAfterEnd() {
            Link link = new Link(VersusMode.NORMAL);
            link.beginBoth();
            topOut(link.client, Side.P2);
            link.pump();

            link.client.quit();

            assertEquals(0, link.clientQuits);
            assertEquals(Side.P1, resultOf(link.client).winner());
        }

        @Test
        @DisplayName("결과가 정해진 뒤에 받은 기권은 결과를 바꾸지 않는다")
        void forfeitAfterResult() {
            Link link = new Link(VersusMode.NORMAL);
            link.beginBoth();
            topOut(link.client, Side.P2);
            link.pump();
            MatchResult decided = resultOf(link.server);

            link.server.handle(new P2PMessage.Forfeit());

            assertSame(decided, resultOf(link.server));
        }

        @Test
        @DisplayName("연결이 끊기면 결과 없이 끝나고 내 게임을 멈춘다")
        void connectionLost() {
            Link link = new Link(VersusMode.NORMAL);
            link.beginBoth();
            RecordingMatchListener listener = new RecordingMatchListener();
            link.server.addListener(listener);

            link.server.connectionClosed();

            assertEquals(MatchPhase.ENDED, link.server.phase());
            assertTrue(link.server.result().isEmpty());
            assertTrue(link.server.state(Side.P1).isGameOver());
            assertEquals(List.of(MatchPhase.ENDED), listener.events);
            assertEquals(0, link.serverResults);

            link.server.connectionClosed();
            assertEquals(1, listener.events.size());
        }

        @Test
        @DisplayName("클라이언트는 서버가 보낸 결과만 받아들인다")
        void serverIgnoresResultMessage() {
            Link link = new Link(VersusMode.NORMAL);
            link.beginBoth();

            link.server.handle(new P2PMessage.Result(MatchResult.win(Side.P2, EndReason.TOP_OUT, 0, 0)));

            assertNull(link.server.result().orElse(null));
        }
    }
}
