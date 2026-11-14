package seoultech.se.tetris.versus.p2p;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import seoultech.se.tetris.component.controller.GameController;
import seoultech.se.tetris.engine.Difficulty;
import seoultech.se.tetris.engine.TetrisEngine;
import seoultech.se.tetris.versus.EndReason;
import seoultech.se.tetris.versus.MatchListener;
import seoultech.se.tetris.versus.MatchPhase;
import seoultech.se.tetris.versus.MatchResult;
import seoultech.se.tetris.versus.Side;
import seoultech.se.tetris.versus.VersusMatch;
import seoultech.se.tetris.versus.VersusMode;
import seoultech.se.tetris.versus.p2p.P2PSession.SessionListener;

@DisplayName("PeerSession")
class PeerSessionTest {

    // ------------------------------------------------------------------
    // 테스트 보조
    // ------------------------------------------------------------------

    /**
     * 서로 이어진 가짜 통로. 보낸 메시지와 닫힘은 상대의 받은 편지함에 쌓였다가 {@link #pump}로 전달된다.
     * 닫기 전에 보낸 메시지는 닫힘보다 먼저 도착한다.
     */
    private static final class PipeConnection implements PeerConnection {
        final Role role;
        PipeConnection peer;
        final List<PeerListener> listeners = new ArrayList<>();
        final Deque<Runnable> inbox = new ArrayDeque<>();
        final List<P2PMessage> sent = new ArrayList<>();
        boolean closed;

        PipeConnection(Role role) {
            this.role = role;
        }

        @Override public Role role() { return role; }
        @Override public void addListener(PeerListener listener) { listeners.add(listener); }
        @Override public void removeListener(PeerListener listener) { listeners.remove(listener); }
        @Override public ConnectionState state() { return closed ? ConnectionState.DISCONNECTED : ConnectionState.NORMAL; }
        @Override public int roundTripMillis() { return 7; }

        @Override
        public void send(P2PMessage message) {
            if (closed) {
                return;
            }
            sent.add(message);
            PipeConnection target = peer;
            peer.inbox.add(() -> List.copyOf(target.listeners).forEach(l -> l.onMessage(message)));
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            inbox.add(() -> List.copyOf(listeners).forEach(l -> l.onClosed(CloseReason.LOCAL)));
            PipeConnection target = peer;
            peer.inbox.add(() -> {
                target.closed = true;
                List.copyOf(target.listeners).forEach(l -> l.onClosed(CloseReason.PEER_LEFT));
            });
        }

        /** 알림 없이 끊긴다. */
        void lose() {
            closed = true;
            inbox.add(() -> List.copyOf(listeners).forEach(l -> l.onClosed(CloseReason.FAILED)));
        }

        long count(Class<?> type) {
            return sent.stream().filter(type::isInstance).count();
        }
    }

    /** 세션에서 일어난 일을 순서대로 기록한다. 대전 결과도 함께 기록한다. */
    private static final class Recorder implements SessionListener {
        final List<Object> events = new ArrayList<>();
        VersusMatch started;

        @Override public void onSettingsChanged(VersusMode mode, Difficulty difficulty) { events.add("설정:" + mode + "/" + difficulty); }
        @Override public void onReadyChanged(boolean mine, boolean opponent) { events.add("준비:" + mine + "/" + opponent); }
        @Override public void onReturnedToWaiting() { events.add("대기"); }
        @Override public void onConnectionChanged(ConnectionState state, int roundTrip) { events.add("연결:" + state + "/" + roundTrip); }
        @Override public void onClosed(CloseReason reason) { events.add(reason); }

        @Override
        public void onMatchStarted(VersusMatch match) {
            started = match;
            events.add("시작");
            // 화면처럼 대전 시작 알림 안에서 대전 리스너를 등록한다
            match.addListener(new MatchListener() {
                @Override public void onMatchEnded(MatchResult result) { events.add(result); }
            });
        }

        void clear() {
            events.clear();
        }
    }

    private final PipeConnection serverConnection = new PipeConnection(Role.SERVER);
    private final PipeConnection clientConnection = new PipeConnection(Role.CLIENT);
    private final AtomicLong seeds = new AtomicLong(100);
    private final Recorder serverEvents = new Recorder();
    private final Recorder clientEvents = new Recorder();
    private final PeerSession server;
    private final PeerSession client;

    PeerSessionTest() {
        serverConnection.peer = clientConnection;
        clientConnection.peer = serverConnection;
        server = new PeerSession(serverConnection, Difficulty.HARD, seeds::getAndIncrement);
        client = new PeerSession(clientConnection, Difficulty.EASY, () -> { throw new AssertionError("클라이언트는 시드를 만들지 않습니다"); });
        server.addListener(serverEvents);
        client.addListener(clientEvents);
    }

    /** 오갈 것이 없을 때까지 양쪽 받은 편지함을 비운다. */
    private void pump() {
        for (int round = 0; round < 1_000; round++) {
            if (serverConnection.inbox.isEmpty() && clientConnection.inbox.isEmpty()) {
                return;
            }
            drain(clientConnection);
            drain(serverConnection);
        }
        throw new AssertionError("끝없이 오갑니다");
    }

    private static void drain(PipeConnection connection) {
        while (!connection.inbox.isEmpty()) {
            connection.inbox.poll().run();
        }
    }

    /** 양쪽이 준비해서 대전을 시작하고 카운트다운을 마친다. */
    private void startMatch() {
        server.setReady(true);
        client.setReady(true);
        pump();
        server.match().orElseThrow().begin();
        client.match().orElseThrow().begin();
    }

    private static void topOut(VersusMatch match, Side side) {
        GameController controller = match.controller(side).orElseThrow();
        for (int i = 0; i < 200 && !match.state(side).isGameOver(); i++) {
            controller.dropToBottom();
            controller.finishClearing();
        }
    }

    // ------------------------------------------------------------------
    // 대기
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("대기")
    class Waiting {

        @Test
        @DisplayName("연결되면 서버의 모드와 난이도를 클라이언트에 알린다")
        void serverAnnouncesSettings() {
            pump();

            assertEquals(VersusMode.NORMAL, client.mode());
            assertEquals(Difficulty.HARD, client.difficulty());
            assertEquals(List.of("설정:NORMAL/HARD", "준비:false/false"), clientEvents.events);
        }

        @Test
        @DisplayName("서버가 설정을 바꾸면 클라이언트에 반영된다")
        void serverChangesSettings() {
            pump();
            clientEvents.clear();

            server.changeSettings(VersusMode.TIME_LIMIT, Difficulty.EASY);
            pump();

            assertEquals(VersusMode.TIME_LIMIT, client.mode());
            assertEquals(Difficulty.EASY, client.difficulty());
            assertEquals(List.of("설정:TIME_LIMIT/EASY", "준비:false/false"), clientEvents.events);
        }

        @Test
        @DisplayName("클라이언트는 설정을 바꿀 수 없다")
        void clientCannotChangeSettings() {
            assertThrows(IllegalStateException.class, () -> client.changeSettings(VersusMode.ITEM, Difficulty.EASY));
        }

        @Test
        @DisplayName("준비하면 상대에게 알려진다")
        void readyIsShared() {
            pump();

            client.setReady(true);
            pump();

            assertTrue(client.isReady());
            assertTrue(server.isOpponentReady());
            assertFalse(server.isReady());
            assertEquals("준비:false/true", serverEvents.events.get(serverEvents.events.size() - 1));
        }

        @Test
        @DisplayName("같은 준비 상태를 다시 눌러도 다시 보내지 않는다")
        void sameReadyNotResent() {
            client.setReady(true);
            client.setReady(true);

            assertEquals(1, clientConnection.count(P2PMessage.Ready.class));
        }

        @Test
        @DisplayName("설정을 바꾸면 양쪽 준비가 풀린다")
        void settingsResetReady() {
            pump();
            server.setReady(true);
            pump();

            server.changeSettings(VersusMode.ITEM, Difficulty.NORMAL);
            pump();

            assertFalse(server.isReady());
            assertFalse(client.isOpponentReady());
        }

        @Test
        @DisplayName("설정이 바뀌기 전에 누른 준비가 늦게 도착해도 남지 않는다")
        void staleReadyIsCancelled() {
            pump();
            client.setReady(true);                                   // 아직 서버에 도착하지 않았다
            server.changeSettings(VersusMode.ITEM, Difficulty.NORMAL);

            pump();

            assertFalse(server.isOpponentReady());
            assertFalse(client.isReady());
        }

        @Test
        @DisplayName("두 플레이어가 모두 준비하면 서버가 시작 신호를 보내고 양쪽 대전이 시작된다")
        void startsWhenBothReady() {
            pump();
            server.changeSettings(VersusMode.ITEM, Difficulty.EASY);
            pump();

            server.setReady(true);
            client.setReady(true);
            pump();

            assertNotNull(serverEvents.started);
            assertNotNull(clientEvents.started);
            assertTrue(server.match().isPresent());
            assertTrue(client.match().isPresent());
            assertEquals(VersusMode.ITEM, clientEvents.started.mode());
            assertEquals(Difficulty.EASY, ((TetrisEngine) clientEvents.started.state(Side.P2)).getDifficulty());
            assertFalse(server.isReady());
            assertFalse(client.isOpponentReady());
        }

        @Test
        @DisplayName("두 PC는 같은 블럭 순서로 시작한다")
        void sameBlocks() {
            startMatch();

            assertEquals(serverEvents.started.state(Side.P1).getCurrentBlock(), clientEvents.started.state(Side.P2).getCurrentBlock());
            assertEquals(serverEvents.started.state(Side.P1).getNextBlock(), clientEvents.started.state(Side.P2).getNextBlock());
        }

        @Test
        @DisplayName("클라이언트는 대전 중에 설정 메시지가 와도 무시한다")
        void clientIgnoresSettingsDuringMatch() {
            startMatch();

            serverConnection.send(new P2PMessage.Settings(VersusMode.TIME_LIMIT, Difficulty.EASY));
            pump();

            assertEquals(VersusMode.NORMAL, client.mode());
            assertTrue(client.match().isPresent());
        }

        @Test
        @DisplayName("해제한 리스너는 더 알림을 받지 않는다")
        void removedListener() {
            client.removeListener(clientEvents);

            pump();
            server.changeSettings(VersusMode.ITEM, Difficulty.EASY);
            pump();

            assertTrue(clientEvents.events.isEmpty());
        }

        @Test
        @DisplayName("대전 중에는 설정을 바꿀 수 없고, 준비 버튼도 무시한다")
        void lockedDuringMatch() {
            startMatch();

            assertThrows(IllegalStateException.class, () -> server.changeSettings(VersusMode.ITEM, Difficulty.EASY));
            client.setReady(true);
            assertFalse(client.isReady());
        }
    }

    // ------------------------------------------------------------------
    // 대전과 대기 복귀
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("대전과 대기 복귀")
    class Matches {

        @Test
        @DisplayName("대전 메시지는 대전에 전달되고, 결과가 나오면 결과를 알린 뒤 대기로 돌아간다")
        void playsAndReturns() {
            startMatch();
            serverEvents.clear();
            clientEvents.clear();

            topOut(client.match().orElseThrow(), Side.P2);
            pump();

            MatchResult result = serverEvents.started.result().orElseThrow();
            assertEquals(Side.P1, result.winner());
            assertEquals(List.of(result, "대기"), serverEvents.events);
            assertEquals(List.of(result, "대기"), clientEvents.events);
            assertTrue(server.match().isEmpty());
            assertTrue(client.match().isEmpty());
        }

        @Test
        @DisplayName("대기로 돌아온 뒤 다시 준비하면 새 시드로 다음 대전을 시작한다")
        void nextMatch() {
            startMatch();
            VersusMatch first = clientEvents.started;
            topOut(client.match().orElseThrow(), Side.P2);
            pump();

            startMatch();

            assertTrue(clientEvents.started != first);
            assertEquals(102, seeds.get());   // 대전마다 시드를 하나씩 썼다
            assertEquals(MatchPhase.PLAYING, client.match().orElseThrow().phase());
        }
    }

    // ------------------------------------------------------------------
    // 나가기와 연결
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("나가기와 연결")
    class Leaving {

        @Test
        @DisplayName("대기 중에 나가면 연결을 끊는다")
        void leaveWhileWaiting() {
            pump();

            client.leave();
            pump();

            assertEquals(CloseReason.LOCAL, last(clientEvents));
            assertEquals(CloseReason.PEER_LEFT, last(serverEvents));
        }

        @Test
        @DisplayName("대전 중에 나가면 기권패로 처리한 뒤 끊고, 상대는 기권승 결과를 받은 뒤 끊김을 받는다")
        void leaveDuringMatch() {
            startMatch();
            serverEvents.clear();
            clientEvents.clear();

            client.leave();
            pump();

            MatchResult forfeit = serverEvents.started.result().orElseThrow();
            assertEquals(Side.P1, forfeit.winner());
            assertEquals(EndReason.FORFEIT, forfeit.reason());
            assertEquals(List.of(forfeit, "대기", CloseReason.PEER_LEFT), serverEvents.events);
            assertEquals(List.of(clientEvents.started.result().orElseThrow(), CloseReason.LOCAL), clientEvents.events);
        }

        @Test
        @DisplayName("대전 화면에서 대전을 나가도 같다")
        void quitFromMatch() {
            startMatch();

            client.match().orElseThrow().quit();
            pump();

            assertEquals(EndReason.FORFEIT, serverEvents.started.result().orElseThrow().reason());
            assertEquals(CloseReason.LOCAL, last(clientEvents));
            assertEquals(CloseReason.PEER_LEFT, last(serverEvents));
        }

        @Test
        @DisplayName("대전 중에 연결이 끊기면 대전은 결과 없이 끝나고 끊김을 알린다")
        void lostDuringMatch() {
            startMatch();

            serverConnection.lose();
            pump();

            assertEquals(MatchPhase.ENDED, serverEvents.started.phase());
            assertTrue(serverEvents.started.result().isEmpty());
            assertEquals(CloseReason.FAILED, last(serverEvents));
        }

        @Test
        @DisplayName("끊긴 뒤에는 준비와 나가기가 아무 일도 하지 않는다")
        void nothingAfterClosed() {
            pump();
            serverConnection.lose();
            pump();
            int sent = serverConnection.sent.size();

            server.setReady(true);
            server.leave();
            server.changeSettings(VersusMode.ITEM, Difficulty.EASY);

            assertEquals(sent, serverConnection.sent.size());
            assertFalse(server.isReady());
        }

        @Test
        @DisplayName("연결 상태 변화와 왕복 시간을 그대로 알린다")
        void forwardsConnectionState() {
            serverConnection.inbox.add(() -> serverConnection.listeners.forEach(
                    l -> l.onStateChanged(ConnectionState.LAGGING, 320)));
            pump();

            assertEquals("연결:LAGGING/320", last(serverEvents));
            assertEquals(7, server.roundTripMillis());
            assertEquals(ConnectionState.NORMAL, server.connectionState());
        }

        private Object last(Recorder recorder) {
            return recorder.events.get(recorder.events.size() - 1);
        }
    }

    // ------------------------------------------------------------------
    // 실제 소켓
    // ------------------------------------------------------------------

    @Test
    @DisplayName("실제 소켓으로 연결해 대기에서 대전 결과까지 진행한다")
    void endToEndOverSockets() throws Exception {
        ExecutorService eventThread = Executors.newSingleThreadExecutor();   // Swing 이벤트 스레드 역할
        try {
            SocketPeerConnector connector = new SocketPeerConnector(eventThread::execute);
            LinkedBlockingQueue<P2PSession> sessions = new LinkedBlockingQueue<>();
            LinkedBlockingQueue<VersusMatch> matches = new LinkedBlockingQueue<>();
            LinkedBlockingQueue<MatchResult> results = new LinkedBlockingQueue<>();
            ConnectListener opener = new ConnectListener() {
                @Override public void onStep(Step step) { }
                @Override public void onFailed(FailReason reason) { throw new AssertionError(reason.toString()); }

                @Override
                public void onConnected(PeerConnection connection) {
                    P2PSession session = P2PSession.open(connection, Difficulty.NORMAL);
                    session.addListener(new SessionListener() {
                        @Override
                        public void onMatchStarted(VersusMatch match) {
                            match.addListener(new MatchListener() {
                                @Override public void onMatchEnded(MatchResult result) { results.add(result); }
                            });
                            match.begin();
                            matches.add(match);
                        }

                        @Override public void onClosed(CloseReason reason) { }
                    });
                    sessions.add(session);
                }
            };
            AtomicReference<PeerConnector.Hosting> hosting = new AtomicReference<>();
            eventThread.submit(() -> hosting.set(connector.host(0, opener))).get();
            eventThread.submit(() -> connector.connect("127.0.0.1", hosting.get().port(), opener)).get();
            P2PSession first = sessions.poll(5, TimeUnit.SECONDS);
            P2PSession second = sessions.poll(5, TimeUnit.SECONDS);
            assertNotNull(first);
            assertNotNull(second);
            P2PSession clientSession = first.role() == Role.CLIENT ? first : second;
            P2PSession serverSession = first.role() == Role.SERVER ? first : second;

            eventThread.submit(() -> serverSession.setReady(true)).get();
            eventThread.submit(() -> clientSession.setReady(true)).get();
            VersusMatch one = matches.poll(5, TimeUnit.SECONDS);
            VersusMatch two = matches.poll(5, TimeUnit.SECONDS);
            assertNotNull(one);
            assertNotNull(two);

            eventThread.submit(() -> topOut(clientSession.match().orElseThrow(), Side.P2)).get();
            MatchResult a = results.poll(5, TimeUnit.SECONDS);
            MatchResult b = results.poll(5, TimeUnit.SECONDS);

            assertNotNull(a);
            assertEquals(a, b);
            assertEquals(Side.P1, a.winner());
            assertEquals(EndReason.TOP_OUT, a.reason());
            eventThread.submit(clientSession::leave).get();
        } finally {
            eventThread.shutdownNow();
        }
    }
}
