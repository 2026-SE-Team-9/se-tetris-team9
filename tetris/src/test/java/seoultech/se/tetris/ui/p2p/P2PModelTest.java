package seoultech.se.tetris.ui.p2p;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import seoultech.se.tetris.config.InMemoryP2PSettingsService;
import seoultech.se.tetris.config.P2PSettingsService;
import seoultech.se.tetris.engine.Difficulty;
import seoultech.se.tetris.ui.p2p.P2PScreenState.InputError;
import seoultech.se.tetris.ui.p2p.P2PScreenState.Screen;
import seoultech.se.tetris.versus.VersusMatch;
import seoultech.se.tetris.versus.VersusMode;
import seoultech.se.tetris.versus.p2p.CloseReason;
import seoultech.se.tetris.versus.p2p.ConnectListener;
import seoultech.se.tetris.versus.p2p.ConnectListener.FailReason;
import seoultech.se.tetris.versus.p2p.ConnectListener.Step;
import seoultech.se.tetris.versus.p2p.P2PSession;
import seoultech.se.tetris.versus.p2p.PeerConnector;
import seoultech.se.tetris.versus.p2p.Role;
import seoultech.se.tetris.versus.p2p.SocketPeerConnector;

class P2PModelTest {

    private static final class StubConnector implements PeerConnector {
        ConnectListener lastListener;
        boolean hostCancelled;
        boolean connectCancelled;
        String lastAddress;
        int lastPort;
        int hostCalls;

        @Override
        public Hosting host(int port, ConnectListener listener) {
            hostCalls++;
            lastListener = listener;
            lastPort = port;
            listener.onStep(Step.WAITING);
            return new Hosting() {
                @Override public int port() { return port; }
                @Override public List<String> addresses() { return List.of("192.168.0.10"); }
                @Override public void cancel() { hostCancelled = true; }
            };
        }

        @Override
        public Pending connect(String address, int port, ConnectListener listener) {
            lastAddress = address;
            lastPort = port;
            lastListener = listener;
            listener.onStep(Step.CONNECTING);
            return () -> connectCancelled = true;
        }
    }

    @Test
    void menuNavigationAndBackToMain() {
        StubConnector connector = new StubConnector();
        InMemoryP2PSettingsService settings = new InMemoryP2PSettingsService();
        AtomicBoolean backCalled = new AtomicBoolean();
        P2PModel model = new P2PModel(connector, settings, () -> Difficulty.NORMAL,
                () -> backCalled.set(true), (s, m) -> { }, r -> { });

        assertEquals(Screen.MENU, model.screen());
        assertEquals(0, model.menuIndex());

        model.down();
        assertEquals(1, model.menuIndex());
        model.down();
        assertEquals(2, model.menuIndex());
        model.select();
        assertTrue(backCalled.get());
    }

    @Test
    void hostScreenRetryAndCancel() {
        StubConnector connector = new StubConnector();
        InMemoryP2PSettingsService settings = new InMemoryP2PSettingsService();
        settings.setPort(25000);
        AtomicInteger repaints = new AtomicInteger();
        P2PModel model = new P2PModel(connector, settings, () -> Difficulty.NORMAL,
                () -> { }, (s, m) -> { }, r -> { });
        model.setRepaintCallback(repaints::incrementAndGet);

        model.select(); // 방 생성
        assertEquals(Screen.HOST, model.screen());
        assertEquals(25000, model.hostPort());
        assertEquals(List.of("192.168.0.10"), model.hostAddresses());
        assertEquals(Step.WAITING, model.step());

        connector.lastListener.onFailed(FailReason.PORT_UNAVAILABLE);
        assertEquals(FailReason.PORT_UNAVAILABLE, model.failure());

        model.select(); // 실패 시 Enter로 다시 시도
        assertEquals(2, connector.hostCalls);
        assertNull(model.failure());

        model.back();
        assertTrue(connector.hostCancelled);
        assertEquals(Screen.MENU, model.screen());
    }

    @Test
    void joinValidationRecentPeerAndCancel() {
        StubConnector connector = new StubConnector();
        InMemoryP2PSettingsService settings = new InMemoryP2PSettingsService();
        settings.addRecentPeer("10.0.0.5", 24800);
        P2PModel model = new P2PModel(connector, settings, () -> Difficulty.NORMAL,
                () -> { }, (s, m) -> { }, r -> { });

        model.down(); // 참여
        model.select();
        assertEquals(Screen.JOIN, model.screen());
        assertEquals("24700", model.portText());

        // 잘못된 IP 검증
        for (char c : "999.1.1.1".toCharArray()) {
            model.type(c);
        }
        model.select();
        assertEquals(InputError.INVALID_IP, model.inputError());
        assertFalse(model.isConnecting());

        // 최근 접속 기록 선택
        model.down(); // port
        model.down(); // connect button
        model.down(); // first recent peer
        assertEquals(P2PScreenState.JOIN_ROW_CONNECT + 1, model.joinRow());
        model.select();
        assertEquals("10.0.0.5", model.ipText());
        assertEquals("24800", model.portText());
        assertEquals(P2PScreenState.JOIN_ROW_CONNECT, model.joinRow());

        // 접속 시도 후 취소
        model.select();
        assertTrue(model.isConnecting());
        assertEquals("10.0.0.5", connector.lastAddress);
        assertEquals(24800, connector.lastPort);

        model.back();
        assertTrue(connector.connectCancelled);
        assertFalse(model.isConnecting());
        assertEquals(Screen.JOIN, model.screen());
    }

    private static P2PSettingsService ephemeralPortSettings() {
        return new P2PSettingsService() {
            @Override public int port() { return 0; }
            @Override public void setPort(int port) { }
            @Override public List<RecentPeer> recentPeers() { return List.of(); }
            @Override public void addRecentPeer(RecentPeer peer) { }
            @Override public boolean removeRecentPeer(RecentPeer peer) { return false; }
            @Override public void clearRecentPeers() { }
        };
    }

    @Test
    void endToEndLoopbackConnectWaitingRoomAndStartMatch() throws Exception {
        Executor direct = Runnable::run;
        SocketPeerConnector serverConnector = new SocketPeerConnector(direct);
        SocketPeerConnector clientConnector = new SocketPeerConnector(direct);

        P2PSettingsService serverSettings = ephemeralPortSettings();
        InMemoryP2PSettingsService clientSettings = new InMemoryP2PSettingsService();

        CountDownLatch matchLatch = new CountDownLatch(2);
        AtomicReference<VersusMatch> serverMatch = new AtomicReference<>();
        AtomicReference<VersusMatch> clientMatch = new AtomicReference<>();
        AtomicReference<P2PSession> serverSessionRef = new AtomicReference<>();

        P2PModel serverModel = new P2PModel(serverConnector, serverSettings, () -> Difficulty.HARD,
                () -> { }, (s, m) -> {
                    serverSessionRef.set(s);
                    serverMatch.set(m);
                    matchLatch.countDown();
                }, r -> { });

        P2PModel clientModel = new P2PModel(clientConnector, clientSettings, () -> Difficulty.EASY,
                () -> { }, (s, m) -> {
                    clientMatch.set(m);
                    matchLatch.countDown();
                }, r -> { });

        serverModel.select(); // HOST
        int boundPort = serverModel.hostPort();
        assertTrue(boundPort > 0);

        clientModel.down();
        clientModel.select(); // JOIN
        for (char c : "127.0.0.1".toCharArray()) {
            clientModel.type(c);
        }
        clientModel.down(); // PORT row
        while (!clientModel.portText().isEmpty()) {
            clientModel.erase();
        }
        for (char c : Integer.toString(boundPort).toCharArray()) {
            clientModel.type(c);
        }
        clientModel.down(); // CONNECT button
        clientModel.select();

        long deadline = System.currentTimeMillis() + 5000;
        while ((serverModel.screen() != Screen.WAITING_ROOM || clientModel.screen() != Screen.WAITING_ROOM)
                && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
        }
        assertEquals(Screen.WAITING_ROOM, serverModel.screen());
        assertEquals(Screen.WAITING_ROOM, clientModel.screen());
        assertEquals(Role.SERVER, serverModel.role());
        assertEquals(Role.CLIENT, clientModel.role());
        assertEquals(1, clientSettings.recentPeers().size());
        assertEquals("127.0.0.1", clientSettings.recentPeers().get(0).address());
        assertEquals(boundPort, clientSettings.recentPeers().get(0).port());

        // 서버가 모드 변경 -> NORMAL -> ITEM
        serverModel.right();
        deadline = System.currentTimeMillis() + 2000;
        while (clientModel.mode() != VersusMode.ITEM && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
        }
        assertEquals(VersusMode.ITEM, clientModel.mode());

        // 양쪽 모두 준비 완료
        serverModel.down(); // DIFFICULTY
        serverModel.down(); // START
        serverModel.select();
        clientModel.select();

        assertTrue(matchLatch.await(5, TimeUnit.SECONDS));
        assertNotNull(serverMatch.get());
        assertNotNull(clientMatch.get());
        assertEquals(VersusMode.ITEM, serverMatch.get().mode());
        assertEquals(VersusMode.ITEM, clientMatch.get().mode());

        // 정리
        serverModel.dispose();
        clientModel.dispose();
    }

    @Test
    void waitingRoomLeaveShowsReasonToPeer() throws Exception {
        Executor direct = Runnable::run;
        SocketPeerConnector serverConnector = new SocketPeerConnector(direct);
        SocketPeerConnector clientConnector = new SocketPeerConnector(direct);

        P2PSettingsService serverSettings = ephemeralPortSettings();
        InMemoryP2PSettingsService clientSettings = new InMemoryP2PSettingsService();

        List<CloseReason> aborted = new ArrayList<>();
        P2PModel serverModel = new P2PModel(serverConnector, serverSettings, () -> Difficulty.NORMAL,
                () -> { }, (s, m) -> { }, aborted::add);
        P2PModel clientModel = new P2PModel(clientConnector, clientSettings, () -> Difficulty.NORMAL,
                () -> { }, (s, m) -> { }, aborted::add);

        serverModel.select();
        int port = serverModel.hostPort();
        clientModel.down();
        clientModel.select();
        for (char c : "127.0.0.1".toCharArray()) {
            clientModel.type(c);
        }
        clientModel.down();
        while (!clientModel.portText().isEmpty()) {
            clientModel.erase();
        }
        for (char c : Integer.toString(port).toCharArray()) {
            clientModel.type(c);
        }
        clientModel.down();
        clientModel.select();

        long deadline = System.currentTimeMillis() + 5000;
        while (clientModel.screen() != Screen.WAITING_ROOM && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
        }

        // 서버가 대기실에서 나가면 클라이언트는 PEER_LEFT 안내를 받는다
        serverModel.back();
        deadline = System.currentTimeMillis() + 3000;
        while (clientModel.screen() != Screen.MENU && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
        }
        assertEquals(Screen.MENU, serverModel.screen());
        assertNull(serverModel.closeReason());
        assertEquals(Screen.MENU, clientModel.screen());
        assertEquals(CloseReason.PEER_LEFT, clientModel.closeReason());
    }

    @Test
    void copyAddressPasteIpPortAndOpenSettingsOnPortFailure() {
        StubConnector connector = new StubConnector();
        InMemoryP2PSettingsService settings = new InMemoryP2PSettingsService();
        settings.setPort(24700);
        AtomicReference<String> copied = new AtomicReference<>();
        AtomicBoolean settingsOpened = new AtomicBoolean();
        P2PModel model = new P2PModel(connector, settings, () -> Difficulty.NORMAL,
                () -> { }, (s, m) -> { }, r -> { },
                copied::set, () -> settingsOpened.set(true), () -> true);

        // 방 생성에서 Ctrl+C 복사
        model.select();
        assertFalse(model.isAddressCopied());
        model.copy();
        assertTrue(model.isAddressCopied());
        assertEquals("192.168.0.10:24700", copied.get());

        // 대기 중에도 S로 설정 열기 가능 (기존 대기는 취소)
        model.openSettings();
        assertTrue(settingsOpened.get());
        assertTrue(connector.hostCancelled);

        // 포트 충돌 시에도 S로 설정 열기 후 변경된 포트로 재시도
        settingsOpened.set(false);
        model.retryHosting();
        connector.lastListener.onFailed(FailReason.PORT_UNAVAILABLE);
        model.openSettings();
        assertTrue(settingsOpened.get());

        settings.setPort(25555);
        model.retryHosting();
        assertEquals(25555, model.hostPort());
        assertFalse(model.isAddressCopied());

        // 참여 화면에서 Ctrl+V로 IP:포트 한 번에 붙여넣기
        model.back();
        model.down();
        model.select();
        model.paste("192.168.0.10:25555");
        assertEquals("192.168.0.10", model.ipText());
        assertEquals("25555", model.portText());
    }

    @Test
    void waitingRoomBackRequiresConfirmation() throws Exception {
        Executor direct = Runnable::run;
        SocketPeerConnector serverConnector = new SocketPeerConnector(direct);
        SocketPeerConnector clientConnector = new SocketPeerConnector(direct);

        P2PSettingsService serverSettings = ephemeralPortSettings();
        InMemoryP2PSettingsService clientSettings = new InMemoryP2PSettingsService();

        AtomicBoolean allowLeave = new AtomicBoolean(false);
        P2PModel serverModel = new P2PModel(serverConnector, serverSettings, () -> Difficulty.NORMAL,
                () -> { }, (s, m) -> { }, r -> { },
                s -> { }, () -> { }, allowLeave::get);
        P2PModel clientModel = new P2PModel(clientConnector, clientSettings, () -> Difficulty.NORMAL,
                () -> { }, (s, m) -> { }, r -> { });

        serverModel.select();
        int port = serverModel.hostPort();
        clientModel.down();
        clientModel.select();
        clientModel.paste("127.0.0.1:" + port);
        clientModel.down();
        clientModel.down();
        clientModel.select();

        long deadline = System.currentTimeMillis() + 5000;
        while (serverModel.screen() != Screen.WAITING_ROOM && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
        }
        assertEquals(Screen.WAITING_ROOM, serverModel.screen());

        // 확인 팝업에서 '아니오' 선택 시 대기실 유지
        serverModel.back();
        assertEquals(Screen.WAITING_ROOM, serverModel.screen());

        // '예' 선택 시 나가기
        allowLeave.set(true);
        serverModel.back();
        assertEquals(Screen.MENU, serverModel.screen());
        clientModel.dispose();
    }
}
