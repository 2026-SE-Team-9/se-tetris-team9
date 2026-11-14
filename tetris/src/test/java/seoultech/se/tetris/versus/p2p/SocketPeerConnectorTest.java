package seoultech.se.tetris.versus.p2p;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import seoultech.se.tetris.versus.p2p.ConnectListener.FailReason;
import seoultech.se.tetris.versus.p2p.ConnectListener.Step;
import seoultech.se.tetris.versus.p2p.PeerConnector.Hosting;

/**
 * 실제 소켓으로 한 PC 안에서 서버와 클라이언트를 연결해 확인한다. (3차 「접속 과정 자동화 테스트」)
 */
@DisplayName("SocketPeerConnector / SocketPeerConnection")
class SocketPeerConnectorTest {

    private static final String LOCALHOST = "127.0.0.1";
    private static final long WAIT_SECONDS = 5;

    /** 확인 신호를 직접 보내도록 하고, 시계도 직접 옮긴다. */
    private final AtomicLong clock = new AtomicLong(1_000_000);
    private final SocketPeerConnector connector = new SocketPeerConnector(Runnable::run, clock::get, false);

    // ------------------------------------------------------------------
    // 테스트 보조
    // ------------------------------------------------------------------

    /**
     * 연결 과정을 기록한다. 통로 리스너를 주면 연결 알림 안에서 등록한다.
     * 연결 직후 상대가 바로 보내거나 끊는 경우도 놓치지 않기 위해서다.
     */
    private static final class RecordingConnect implements ConnectListener {
        final List<Step> steps = new ArrayList<>();
        final BlockingQueue<Object> results = new LinkedBlockingQueue<>();
        final PeerListener peerListener;

        RecordingConnect() {
            this(null);
        }

        RecordingConnect(PeerListener peerListener) {
            this.peerListener = peerListener;
        }

        @Override public synchronized void onStep(Step step) { steps.add(step); }

        @Override
        public void onConnected(PeerConnection connection) {
            if (peerListener != null) {
                connection.addListener(peerListener);
            }
            results.add(connection);
        }

        @Override public void onFailed(FailReason reason) { results.add(reason); }

        PeerConnection awaitConnected() throws InterruptedException {
            Object result = results.poll(WAIT_SECONDS, TimeUnit.SECONDS);
            if (result instanceof PeerConnection connection) {
                return connection;
            }
            throw new AssertionError("연결되지 않았습니다: " + result);
        }

        FailReason awaitFailed() throws InterruptedException {
            Object result = results.poll(WAIT_SECONDS, TimeUnit.SECONDS);
            if (result instanceof FailReason reason) {
                return reason;
            }
            throw new AssertionError("실패하지 않았습니다: " + result);
        }

        synchronized List<Step> steps() {
            return List.copyOf(steps);
        }
    }

    /** 통로에서 일어난 일을 기록한다. 메시지, 상태 변화("상태:정상/0"), 닫힘 이유를 순서대로 담는다. */
    private static final class RecordingPeer implements PeerListener {
        final BlockingQueue<Object> events = new LinkedBlockingQueue<>();

        @Override public void onMessage(P2PMessage message) { events.add(message); }
        @Override public void onStateChanged(ConnectionState state, int roundTrip) { events.add("상태:" + state + "/" + roundTrip); }
        @Override public void onClosed(CloseReason reason) { events.add(reason); }

        Object next() throws InterruptedException {
            Object event = events.poll(WAIT_SECONDS, TimeUnit.SECONDS);
            if (event == null) {
                fail("기다린 일이 일어나지 않았습니다");
            }
            return event;
        }

        /** 잠깐 기다려도 아무 일이 없는지 */
        Object nextOrNull() throws InterruptedException {
            return events.poll(300, TimeUnit.MILLISECONDS);
        }
    }

    private record Pair(PeerConnection server, PeerConnection client, RecordingPeer serverEvents, RecordingPeer clientEvents) {
    }

    private Pair connectPair() throws InterruptedException {
        RecordingPeer serverEvents = new RecordingPeer();
        RecordingPeer clientEvents = new RecordingPeer();
        RecordingConnect hosting = new RecordingConnect(serverEvents);
        RecordingConnect joining = new RecordingConnect(clientEvents);
        Hosting host = connector.host(0, hosting);
        connector.connect(LOCALHOST, host.port(), joining);
        return new Pair(hosting.awaitConnected(), joining.awaitConnected(), serverEvents, clientEvents);
    }

    /** 접속을 하나 받아 정해진 동작을 하는 가짜 상대 */
    private interface FakePeer {
        void run(DataInputStream in, DataOutputStream out, Socket socket) throws IOException;
    }

    private static int fakeServer(FakePeer behavior) throws IOException {
        ServerSocket server = new ServerSocket(0);
        Thread thread = new Thread(() -> {
            try (server; Socket socket = server.accept()) {
                behavior.run(new DataInputStream(socket.getInputStream()),
                        new DataOutputStream(socket.getOutputStream()), socket);
            } catch (IOException ignored) {
                // 테스트 대상이 연결을 끊으면 여기로 온다
            }
        });
        thread.setDaemon(true);
        thread.start();
        return server.getLocalPort();
    }

    /** 버전 확인까지 정상으로 마치는 가짜 상대의 앞부분 */
    private static void answerHello(DataInputStream in, DataOutputStream out) throws IOException {
        MessageCodec.read(in);
        MessageCodec.write(out, new P2PMessage.Hello(P2PMessage.PROTOCOL_VERSION));
        out.flush();
    }

    private static int unusedPort() throws IOException {
        try (ServerSocket probe = new ServerSocket(0)) {
            return probe.getLocalPort();
        }
    }

    // ------------------------------------------------------------------
    // 연결 만들기
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("연결 만들기")
    class Connecting {

        @Test
        @DisplayName("서버가 기다리고 클라이언트가 접속하면 양쪽 모두 연결된다")
        void connects() throws InterruptedException {
            RecordingConnect hosting = new RecordingConnect();
            RecordingConnect joining = new RecordingConnect();
            Hosting host = connector.host(0, hosting);
            connector.connect(LOCALHOST, host.port(), joining);

            assertEquals(Role.SERVER, hosting.awaitConnected().role());
            assertEquals(Role.CLIENT, joining.awaitConnected().role());
        }

        @Test
        @DisplayName("각 단계를 순서대로 알린다")
        void reportsSteps() throws InterruptedException {
            RecordingConnect hosting = new RecordingConnect();
            RecordingConnect joining = new RecordingConnect();
            Hosting host = connector.host(0, hosting);
            assertEquals(List.of(Step.OPENING, Step.WAITING), hosting.steps());

            connector.connect(LOCALHOST, host.port(), joining);
            hosting.awaitConnected();
            joining.awaitConnected();

            assertEquals(List.of(Step.OPENING, Step.WAITING, Step.CHECKING_VERSION), hosting.steps());
            assertEquals(List.of(Step.CONNECTING, Step.CHECKING_VERSION), joining.steps());
        }

        @Test
        @DisplayName("서버는 기다리는 포트와 이 PC의 IPv4 주소를 알려준다")
        void reportsPortAndAddresses() {
            Hosting host = connector.host(0, new RecordingConnect());

            assertTrue(host.port() > 0);
            assertFalse(host.addresses().isEmpty());
            for (String address : host.addresses()) {
                assertTrue(address.matches("\\d+\\.\\d+\\.\\d+\\.\\d+"), address);
            }
            host.cancel();
        }

        @Test
        @DisplayName("포트가 이미 사용 중이면 실패한다")
        void portInUse() throws IOException, InterruptedException {
            try (ServerSocket occupied = new ServerSocket(0)) {
                RecordingConnect hosting = new RecordingConnect();

                connector.host(occupied.getLocalPort(), hosting);

                assertEquals(FailReason.PORT_UNAVAILABLE, hosting.awaitFailed());
                assertEquals(List.of(Step.OPENING), hosting.steps());
            }
        }

        @Test
        @DisplayName("주소 형식이 IPv4가 아니면 접속을 시도하지 않고 실패한다")
        void invalidAddress() throws InterruptedException {
            for (String address : new String[] {"abc", "256.1.1.1", "1.2.3", "1.2.3.4.5", "", "   ", null}) {
                RecordingConnect joining = new RecordingConnect();

                connector.connect(address, 12345, joining);

                assertEquals(FailReason.INVALID_ADDRESS, joining.awaitFailed(), String.valueOf(address));
                assertTrue(joining.steps().isEmpty());
            }
        }

        @Test
        @DisplayName("앞뒤 공백은 무시한다")
        void stripsAddress() throws InterruptedException {
            RecordingConnect hosting = new RecordingConnect();
            RecordingConnect joining = new RecordingConnect();
            Hosting host = connector.host(0, hosting);

            connector.connect("  " + LOCALHOST + " ", host.port(), joining);

            joining.awaitConnected();
        }

        @Test
        @DisplayName("기다리는 서버가 없으면 실패한다")
        void unreachable() throws IOException, InterruptedException {
            RecordingConnect joining = new RecordingConnect();

            connector.connect(LOCALHOST, unusedPort(), joining);

            assertEquals(FailReason.UNREACHABLE, joining.awaitFailed());
        }

        @Test
        @DisplayName("상대 프로그램의 판이 다르면 실패한다")
        void versionMismatch() throws IOException, InterruptedException {
            int port = fakeServer((in, out, socket) -> {
                MessageCodec.read(in);
                MessageCodec.write(out, new P2PMessage.Hello(P2PMessage.PROTOCOL_VERSION + 1));
                out.flush();
                in.read();   // 상대가 끊을 때까지 기다린다
            });
            RecordingConnect joining = new RecordingConnect();

            connector.connect(LOCALHOST, port, joining);

            assertEquals(FailReason.VERSION_MISMATCH, joining.awaitFailed());
        }

        @Test
        @DisplayName("상대가 이 게임이 아니어서 메시지를 읽을 수 없으면 판이 다른 것으로 본다")
        void notThisGame() throws IOException, InterruptedException {
            int port = fakeServer((in, out, socket) -> {
                MessageCodec.read(in);   // 상대의 버전 확인을 다 읽어야 끊을 때 상대 쪽 데이터가 버려지지 않는다
                out.writeInt(5);
                out.write(new byte[] {1, 2, 3, 4, 5});
                out.flush();
                in.read();
            });
            RecordingConnect joining = new RecordingConnect();

            connector.connect(LOCALHOST, port, joining);

            assertEquals(FailReason.VERSION_MISMATCH, joining.awaitFailed());
        }

        @Test
        @DisplayName("기다리기를 취소하면 포트를 닫아 더 접속할 수 없고, 더 알리지 않는다")
        void cancelHosting() throws InterruptedException {
            RecordingConnect hosting = new RecordingConnect();
            Hosting host = connector.host(0, hosting);

            host.cancel();

            RecordingConnect joining = new RecordingConnect();
            connector.connect(LOCALHOST, host.port(), joining);
            assertEquals(FailReason.UNREACHABLE, joining.awaitFailed());
            assertNull(hosting.results.poll(300, TimeUnit.MILLISECONDS));
        }

        @Test
        @DisplayName("연결된 뒤에 취소해도 연결은 그대로다")
        void cancelAfterConnected() throws InterruptedException {
            RecordingPeer clientEvents = new RecordingPeer();
            RecordingConnect hosting = new RecordingConnect();
            RecordingConnect joining = new RecordingConnect(clientEvents);
            Hosting host = connector.host(0, hosting);
            connector.connect(LOCALHOST, host.port(), joining);
            PeerConnection server = hosting.awaitConnected();
            joining.awaitConnected();

            host.cancel();
            server.send(new P2PMessage.Ready(true));

            assertEquals(new P2PMessage.Ready(true), clientEvents.next());
        }

        @Test
        @DisplayName("포트가 범위를 벗어나면 예외를 던진다")
        void rejectsBadPort() {
            RecordingConnect listener = new RecordingConnect();

            assertThrows(IllegalArgumentException.class, () -> connector.host(-1, listener));
            assertThrows(IllegalArgumentException.class, () -> connector.host(65_536, listener));
            assertThrows(IllegalArgumentException.class, () -> connector.connect(LOCALHOST, 0, listener));
        }
    }

    // ------------------------------------------------------------------
    // 주고받기
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("주고받기")
    class Messaging {

        @Test
        @DisplayName("양쪽 방향으로 메시지를 주고받는다")
        void bothWays() throws InterruptedException {
            Pair pair = connectPair();

            pair.server().send(new P2PMessage.Ready(true));
            pair.client().send(new P2PMessage.Ready(false));

            assertEquals(new P2PMessage.Ready(true), pair.clientEvents().next());
            assertEquals(new P2PMessage.Ready(false), pair.serverEvents().next());
        }

        @Test
        @DisplayName("보낸 순서대로 도착한다")
        void keepsOrder() throws InterruptedException {
            Pair pair = connectPair();

            for (int i = 0; i < 50; i++) {
                pair.client().send(new P2PMessage.FinalScore(i));
            }

            for (int i = 0; i < 50; i++) {
                assertEquals(new P2PMessage.FinalScore(i), pair.serverEvents().next());
            }
        }

        @Test
        @DisplayName("상대가 알림 없이 끊으면 통신 오류로 닫힌다")
        void peerVanishes() throws IOException, InterruptedException {
            int port = fakeServer((in, out, socket) -> answerHello(in, out));   // 확인만 하고 바로 끊는다
            RecordingPeer events = new RecordingPeer();
            RecordingConnect joining = new RecordingConnect(events);
            connector.connect(LOCALHOST, port, joining);
            PeerConnection client = joining.awaitConnected();

            assertEquals(CloseReason.FAILED, events.next());
            assertEquals(ConnectionState.DISCONNECTED, client.state());
        }

        @Test
        @DisplayName("연결 뒤에 읽을 수 없는 데이터가 오면 통신 오류로 닫힌다")
        void garbageAfterConnect() throws IOException, InterruptedException {
            int port = fakeServer((in, out, socket) -> {
                answerHello(in, out);
                out.writeInt(3);
                out.write(new byte[] {9, 9, 9});
                out.flush();
                in.read();
            });
            RecordingPeer events = new RecordingPeer();
            RecordingConnect joining = new RecordingConnect(events);
            connector.connect(LOCALHOST, port, joining);
            PeerConnection client = joining.awaitConnected();

            assertEquals(CloseReason.FAILED, events.next());
        }

        @Test
        @DisplayName("버전 확인 직후 상대가 바로 보낸 메시지도 놓치지 않는다")
        void messageRightAfterHello() throws IOException, InterruptedException {
            int port = fakeServer((in, out, socket) -> {
                answerHello(in, out);
                MessageCodec.write(out, new P2PMessage.Settings(seoultech.se.tetris.versus.VersusMode.ITEM,
                        seoultech.se.tetris.engine.Difficulty.HARD));
                out.flush();
                in.read();
            });
            BlockingQueue<Object> events = new LinkedBlockingQueue<>();
            connector.connect(LOCALHOST, port, new ConnectListener() {
                @Override public void onStep(Step step) { }
                @Override public void onFailed(FailReason reason) { events.add(reason); }

                @Override
                public void onConnected(PeerConnection connection) {
                    // 연결 알림 안에서 등록하면 그 뒤의 메시지를 놓치지 않는다
                    connection.addListener(new PeerListener() {
                        @Override public void onMessage(P2PMessage message) { events.add(message); }
                        @Override public void onClosed(CloseReason reason) { }
                    });
                }
            });

            assertEquals(new P2PMessage.Settings(seoultech.se.tetris.versus.VersusMode.ITEM,
                    seoultech.se.tetris.engine.Difficulty.HARD), events.poll(WAIT_SECONDS, TimeUnit.SECONDS));
        }
    }

    // ------------------------------------------------------------------
    // 확인 신호와 연결 상태
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("확인 신호와 연결 상태")
    class Heartbeat {

        @Test
        @DisplayName("확인 신호를 보내면 상대가 자동으로 응답하고, 왕복 시간이 처음 잡히면 상태 변화를 알린다")
        void pingPong() throws InterruptedException {
            Pair pair = connectPair();
            SocketPeerConnection client = (SocketPeerConnection) pair.client();

            client.tick();                                // 확인 신호 전송
            waitUntil(() -> client.roundTripMillis() >= 0);
            client.tick();                                // 응답을 반영해 다시 계산

            assertEquals("상태:NORMAL/0", pair.clientEvents().next());
            assertEquals(0, client.roundTripMillis());
            assertEquals(ConnectionState.NORMAL, client.state());
        }

        @Test
        @DisplayName("확인 신호와 응답은 리스너에 전달하지 않는다")
        void heartbeatIsHidden() throws InterruptedException {
            Pair pair = connectPair();
            SocketPeerConnection client = (SocketPeerConnection) pair.client();

            client.tick();
            waitUntil(() -> client.roundTripMillis() >= 0);

            assertNull(pair.serverEvents().nextOrNull());
        }

        @Test
        @DisplayName("1초 동안 아무것도 받지 못하면 지연을 알린다")
        void lagging() throws InterruptedException {
            Pair pair = connectPair();
            SocketPeerConnection client = (SocketPeerConnection) pair.client();

            clock.addAndGet(1_000);
            client.tick();

            assertEquals("상태:LAGGING/-1", pair.clientEvents().next());
            assertEquals(ConnectionState.LAGGING, client.state());
        }

        @Test
        @DisplayName("10초 동안 아무것도 받지 못하면 연결을 닫는다")
        void timesOut() throws InterruptedException {
            Pair pair = connectPair();
            SocketPeerConnection client = (SocketPeerConnection) pair.client();

            clock.addAndGet(10_000);
            client.tick();

            assertEquals(CloseReason.TIMED_OUT, pair.clientEvents().next());
            assertEquals(CloseReason.FAILED, pair.serverEvents().next());   // 상대는 알림 없이 끊긴 것으로 본다
        }

        @Test
        @DisplayName("실제 시계로 만들면 확인 신호를 0.5초마다 자동으로 주고받는다")
        void automaticHeartbeat() throws InterruptedException {
            SocketPeerConnector real = new SocketPeerConnector(Runnable::run);
            RecordingPeer clientEvents = new RecordingPeer();
            RecordingConnect hosting = new RecordingConnect();
            RecordingConnect joining = new RecordingConnect(clientEvents);
            Hosting host = real.host(0, hosting);
            real.connect(LOCALHOST, host.port(), joining);
            PeerConnection server = hosting.awaitConnected();
            PeerConnection client = joining.awaitConnected();

            Object event = clientEvents.next();   // 처음 왕복 시간이 잡혀 상태 변화를 알린다

            assertTrue(event.toString().startsWith("상태:NORMAL/"), event.toString());
            assertTrue(client.roundTripMillis() >= 0);
            client.close();
            server.close();
        }

        private void waitUntil(java.util.function.BooleanSupplier condition) throws InterruptedException {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(WAIT_SECONDS);
            while (!condition.getAsBoolean()) {
                if (System.nanoTime() > deadline) {
                    fail("조건이 만족되지 않았습니다");
                }
                Thread.sleep(10);
            }
        }
    }

    // ------------------------------------------------------------------
    // 닫기
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("닫기")
    class Closing {

        @Test
        @DisplayName("직접 끊으면 먼저 보낸 메시지를 마저 보내고, 상대에게는 상대가 끊은 것으로 알린다")
        void closeAfterSending() throws InterruptedException {
            Pair pair = connectPair();

            pair.client().send(new P2PMessage.Forfeit());
            pair.client().close();

            assertEquals(CloseReason.LOCAL, pair.clientEvents().next());
            assertEquals(new P2PMessage.Forfeit(), pair.serverEvents().next());
            assertEquals(CloseReason.PEER_LEFT, pair.serverEvents().next());
            assertEquals(ConnectionState.DISCONNECTED, pair.client().state());
        }

        @Test
        @DisplayName("상대가 계속 보내는 중에 끊어도, 상대는 마지막 메시지와 끊는다는 신호를 받는다")
        void closeWhilePeerIsSending() throws InterruptedException {
            // 읽지 않은 데이터가 남은 채 소켓을 닫으면 강제 종료가 나가 마지막 메시지가 사라질 수 있다.
            // 타이밍에 따라 나타나므로 여러 번 반복한다
            for (int round = 0; round < 5; round++) {
                Pair pair = connectPair();
                Thread flood = new Thread(() -> {
                    for (int i = 0; i < 5_000; i++) {
                        pair.client().send(new P2PMessage.FinalScore(i));
                    }
                });
                flood.start();

                pair.server().send(new P2PMessage.Forfeit());
                pair.server().close();

                assertEquals(new P2PMessage.Forfeit(), pair.clientEvents().next(), "반복 " + round);
                assertEquals(CloseReason.PEER_LEFT, pair.clientEvents().next(), "반복 " + round);
                flood.join();
                pair.client().close();
            }
        }

        @Test
        @DisplayName("연결 알림 안에서 바로 닫아도 오류 없이 닫히고 상대도 끊김을 받는다")
        void closeInsideOnConnected() throws InterruptedException {
            List<Throwable> errors = new java.util.concurrent.CopyOnWriteArrayList<>();
            SocketPeerConnector real = new SocketPeerConnector(task -> {
                try {
                    task.run();
                } catch (RuntimeException e) {
                    errors.add(e);   // 실행기에서 난 예외를 모은다
                }
            });
            RecordingPeer serverEvents = new RecordingPeer();
            RecordingConnect hosting = new RecordingConnect(serverEvents);
            Hosting host = real.host(0, hosting);
            BlockingQueue<Object> clientEvents = new LinkedBlockingQueue<>();
            real.connect(LOCALHOST, host.port(), new ConnectListener() {
                @Override public void onStep(Step step) { }
                @Override public void onFailed(FailReason reason) { clientEvents.add(reason); }

                @Override
                public void onConnected(PeerConnection connection) {
                    connection.addListener(new PeerListener() {
                        @Override public void onMessage(P2PMessage message) { }
                        @Override public void onClosed(CloseReason reason) { clientEvents.add(reason); }
                    });
                    connection.close();   // 받기와 확인 신호를 시작하기도 전에 닫는다
                }
            });

            assertEquals(CloseReason.LOCAL, clientEvents.poll(WAIT_SECONDS, TimeUnit.SECONDS));
            hosting.awaitConnected();
            assertEquals(CloseReason.PEER_LEFT, serverEvents.next());
            assertTrue(errors.isEmpty(), errors.toString());
        }

        @Test
        @DisplayName("두 번 닫아도 닫힘은 한 번만 알린다")
        void closeTwice() throws InterruptedException {
            Pair pair = connectPair();

            pair.client().close();
            pair.client().close();

            assertEquals(CloseReason.LOCAL, pair.clientEvents().next());
            assertNull(pair.clientEvents().nextOrNull());
        }

        @Test
        @DisplayName("닫힌 뒤에 보낸 메시지는 무시한다")
        void sendAfterClose() throws InterruptedException {
            Pair pair = connectPair();
            pair.client().close();
            assertEquals(CloseReason.PEER_LEFT, pair.serverEvents().next());

            pair.client().send(new P2PMessage.Ready(true));

            assertNull(pair.serverEvents().nextOrNull());
        }
    }
}
