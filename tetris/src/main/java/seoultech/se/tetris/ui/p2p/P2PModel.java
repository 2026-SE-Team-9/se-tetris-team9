package seoultech.se.tetris.ui.p2p;

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import seoultech.se.tetris.config.P2PSettingsService;
import seoultech.se.tetris.config.P2PSettingsService.RecentPeer;
import seoultech.se.tetris.engine.Difficulty;
import seoultech.se.tetris.versus.VersusMatch;
import seoultech.se.tetris.versus.VersusMode;
import seoultech.se.tetris.versus.p2p.CloseReason;
import seoultech.se.tetris.versus.p2p.ConnectListener;
import seoultech.se.tetris.versus.p2p.ConnectListener.FailReason;
import seoultech.se.tetris.versus.p2p.ConnectListener.Step;
import seoultech.se.tetris.versus.p2p.ConnectionState;
import seoultech.se.tetris.versus.p2p.P2PSession;
import seoultech.se.tetris.versus.p2p.PeerConnection;
import seoultech.se.tetris.versus.p2p.PeerConnector;
import seoultech.se.tetris.versus.p2p.PeerConnector.Hosting;
import seoultech.se.tetris.versus.p2p.PeerConnector.Pending;
import seoultech.se.tetris.versus.p2p.Role;

/**
 * P2P 대전 모드 연결 화면({@link P2PPanel})의 상태와 키 동작을 관리한다.
 *
 * <p>첫 화면(방 생성 / 참여 / 뒤로 가기), 방 생성, 참여, 대기실 흐름과
 * {@link PeerConnector}·{@link P2PSession} 연동을 담당한다. Swing/AWT에 의존하지 않는다.
 */
public final class P2PModel implements P2PScreenState, P2PScreenActions {

    private static final int MENU_HOST = 0;
    private static final int MENU_JOIN = 1;
    private static final int MENU_BACK = 2;
    private static final int RECENT_ROWS = 5;
    private static final int MAX_RECENT_DISPLAY = 10;

    private static final Pattern IPV4 = Pattern.compile(
            "((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)");

    private final PeerConnector connector;
    private final P2PSettingsService settings;
    private final Supplier<Difficulty> initialDifficulty;
    private final Runnable onBackToMainMenu;
    private final BiConsumer<P2PSession, VersusMatch> onMatchStarted;
    private final Consumer<CloseReason> onMatchAborted;

    private Runnable repaintCallback = () -> { };

    private Screen screen = Screen.MENU;
    private int menuIndex;
    private CloseReason closeReason;

    private Step step;
    private FailReason failure;

    private Hosting hosting;
    private List<String> hostAddresses = List.of();
    private int hostPort = P2PSettingsService.DEFAULT_PORT;

    private Pending pending;
    private String ipText = "";
    private String portText = Integer.toString(P2PSettingsService.DEFAULT_PORT);
    private int joinRow = JOIN_ROW_IP;
    private boolean connecting;
    private InputError inputError;
    private List<RecentPeer> recentPeers = List.of();

    private P2PSession session;
    private int roomRow = ROOM_ROW_MODE;
    private boolean inMatch;
    private CloseReason pendingCloseReason;

    public P2PModel(PeerConnector connector, P2PSettingsService settings,
            Supplier<Difficulty> initialDifficulty, Runnable onBackToMainMenu,
            BiConsumer<P2PSession, VersusMatch> onMatchStarted,
            Consumer<CloseReason> onMatchAborted) {
        this.connector = Objects.requireNonNull(connector, "connector");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.initialDifficulty = Objects.requireNonNull(initialDifficulty, "initialDifficulty");
        this.onBackToMainMenu = Objects.requireNonNull(onBackToMainMenu, "onBackToMainMenu");
        this.onMatchStarted = Objects.requireNonNull(onMatchStarted, "onMatchStarted");
        this.onMatchAborted = Objects.requireNonNull(onMatchAborted, "onMatchAborted");
    }

    /** 상태가 바뀔 때 화면을 다시 그릴 콜백을 연결한다. */
    public void setRepaintCallback(Runnable repaintCallback) {
        this.repaintCallback = Objects.requireNonNull(repaintCallback, "repaintCallback");
    }

    /** 대전이 끝나고 P2P 화면으로 돌아왔을 때 호출한다. */
    public void onMatchClosed() {
        if (!inMatch && session == null && screen == Screen.MENU) {
            repaint();
            return;
        }
        inMatch = false;
        if (pendingCloseReason != null || session == null) {
            CloseReason reason = pendingCloseReason;
            pendingCloseReason = null;
            session = null;
            returnToP2PMenu(reason == CloseReason.LOCAL ? null : reason);
            return;
        }
        screen = Screen.WAITING_ROOM;
        roomRow = session.role() == Role.SERVER ? ROOM_ROW_MODE : ROOM_ROW_START;
        repaint();
    }

    /** 진행 중인 연결이나 세션을 정리한다. */
    public void dispose() {
        cancelHosting();
        cancelPending();
        if (session != null) {
            P2PSession active = session;
            session = null;
            active.leave();
        }
    }

    // ------------------------------------------------------------------
    // P2PScreenState
    // ------------------------------------------------------------------

    @Override public Screen screen() { return screen; }
    @Override public int menuIndex() { return menuIndex; }
    @Override public CloseReason closeReason() { return closeReason; }
    @Override public Step step() { return step; }
    @Override public FailReason failure() { return failure; }
    @Override public List<String> hostAddresses() { return hostAddresses; }
    @Override public int hostPort() { return hostPort; }
    @Override public String ipText() { return ipText; }
    @Override public String portText() { return portText; }
    @Override public int joinRow() { return joinRow; }
    @Override public boolean isConnecting() { return connecting; }
    @Override public InputError inputError() { return inputError; }
    @Override public List<RecentPeer> recentPeers() { return recentPeers; }

    @Override
    public int roomRow() {
        if (session != null && session.role() == Role.CLIENT) {
            return ROOM_ROW_START;
        }
        return roomRow;
    }

    @Override public boolean isReady() { return session != null && session.isReady(); }
    @Override public boolean isOpponentReady() { return session != null && session.isOpponentReady(); }
    @Override public Role role() { return session != null ? session.role() : Role.SERVER; }
    @Override public VersusMode mode() { return session != null ? session.mode() : VersusMode.NORMAL; }
    @Override public Difficulty difficulty() { return session != null ? session.difficulty() : Difficulty.DEFAULT; }

    @Override
    public ConnectionState connectionState() {
        return session != null ? session.connectionState() : ConnectionState.DISCONNECTED;
    }

    @Override
    public int roundTripMillis() {
        return session != null ? session.roundTripMillis() : -1;
    }

    // ------------------------------------------------------------------
    // P2PScreenActions
    // ------------------------------------------------------------------

    @Override
    public void up() {
        switch (screen) {
            case MENU -> {
                menuIndex = (menuIndex - 1 + MENU_ITEM_COUNT) % MENU_ITEM_COUNT;
                repaint();
            }
            case JOIN -> {
                if (connecting) {
                    return;
                }
                int count = joinRowCount();
                joinRow = (joinRow - 1 + count) % count;
                repaint();
            }
            case WAITING_ROOM -> {
                if (session != null && session.role() == Role.SERVER) {
                    roomRow = (roomRow - 1 + 3) % 3;
                    repaint();
                }
            }
            case HOST -> { }
        }
    }

    @Override
    public void down() {
        switch (screen) {
            case MENU -> {
                menuIndex = (menuIndex + 1) % MENU_ITEM_COUNT;
                repaint();
            }
            case JOIN -> {
                if (connecting) {
                    return;
                }
                int count = joinRowCount();
                joinRow = (joinRow + 1) % count;
                repaint();
            }
            case WAITING_ROOM -> {
                if (session != null && session.role() == Role.SERVER) {
                    roomRow = (roomRow + 1) % 3;
                    repaint();
                }
            }
            case HOST -> { }
        }
    }

    @Override
    public void left() {
        if (screen == Screen.WAITING_ROOM && session != null && session.role() == Role.SERVER) {
            shiftRoomSetting(-1);
        } else if (screen == Screen.JOIN && !connecting && joinRow > JOIN_ROW_CONNECT) {
            shiftRecentColumn(-1);
        }
    }

    @Override
    public void right() {
        if (screen == Screen.WAITING_ROOM && session != null && session.role() == Role.SERVER) {
            shiftRoomSetting(1);
        } else if (screen == Screen.JOIN && !connecting && joinRow > JOIN_ROW_CONNECT) {
            shiftRecentColumn(1);
        }
    }

    @Override
    public void select() {
        switch (screen) {
            case MENU -> selectMenu();
            case HOST -> {
                if (failure != null) {
                    startHosting();
                }
            }
            case JOIN -> selectJoin();
            case WAITING_ROOM -> selectWaitingRoom();
        }
    }

    @Override
    public void back() {
        switch (screen) {
            case MENU -> onBackToMainMenu.run();
            case HOST -> {
                cancelHosting();
                returnToP2PMenu(null);
            }
            case JOIN -> {
                if (connecting) {
                    cancelPending();
                    connecting = false;
                    step = null;
                    repaint();
                } else {
                    returnToP2PMenu(null);
                }
            }
            case WAITING_ROOM -> {
                if (session != null) {
                    P2PSession active = session;
                    session = null;
                    active.leave();
                }
                returnToP2PMenu(null);
            }
        }
    }

    @Override
    public void type(char c) {
        if (screen != Screen.JOIN || connecting) {
            return;
        }
        if (joinRow == JOIN_ROW_IP) {
            if ((Character.isDigit(c) || c == '.') && ipText.length() < 15) {
                ipText += c;
                inputError = null;
                failure = null;
                repaint();
            }
        } else if (joinRow == JOIN_ROW_PORT) {
            if (Character.isDigit(c) && portText.length() < 5) {
                portText += c;
                inputError = null;
                failure = null;
                repaint();
            }
        }
    }

    @Override
    public void erase() {
        if (screen != Screen.JOIN || connecting) {
            return;
        }
        if (joinRow == JOIN_ROW_IP && !ipText.isEmpty()) {
            ipText = ipText.substring(0, ipText.length() - 1);
            inputError = null;
            failure = null;
            repaint();
        } else if (joinRow == JOIN_ROW_PORT && !portText.isEmpty()) {
            portText = portText.substring(0, portText.length() - 1);
            inputError = null;
            failure = null;
            repaint();
        }
    }

    // ------------------------------------------------------------------
    // 화면별 동작
    // ------------------------------------------------------------------

    private void selectMenu() {
        closeReason = null;
        switch (menuIndex) {
            case MENU_HOST -> startHosting();
            case MENU_JOIN -> openJoinScreen();
            case MENU_BACK -> onBackToMainMenu.run();
            default -> { }
        }
    }

    private void startHosting() {
        cancelHosting();
        screen = Screen.HOST;
        step = null;
        failure = null;
        hostPort = settings.port();
        hostAddresses = List.of();
        hosting = connector.host(hostPort, new ConnectListener() {
            @Override
            public void onStep(Step nextStep) {
                if (screen != Screen.HOST) {
                    return;
                }
                step = nextStep;
                repaint();
            }

            @Override
            public void onConnected(PeerConnection connection) {
                hosting = null;
                if (screen != Screen.HOST) {
                    connection.close();
                    return;
                }
                enterWaitingRoom(connection);
            }

            @Override
            public void onFailed(FailReason reason) {
                hosting = null;
                if (screen != Screen.HOST) {
                    return;
                }
                step = null;
                failure = reason;
                repaint();
            }
        });
        hostAddresses = hosting.addresses();
        hostPort = hosting.port();
        repaint();
    }

    private void openJoinScreen() {
        screen = Screen.JOIN;
        step = null;
        failure = null;
        inputError = null;
        connecting = false;
        joinRow = JOIN_ROW_IP;
        portText = Integer.toString(settings.port());
        recentPeers = settings.recentPeers();
        repaint();
    }

    private void selectJoin() {
        if (connecting) {
            return;
        }
        if (joinRow > JOIN_ROW_CONNECT) {
            int index = joinRow - JOIN_ROW_CONNECT - 1;
            if (index >= 0 && index < Math.min(recentPeers.size(), MAX_RECENT_DISPLAY)) {
                RecentPeer peer = recentPeers.get(index);
                ipText = peer.address();
                portText = Integer.toString(peer.port());
                inputError = null;
                failure = null;
                joinRow = JOIN_ROW_CONNECT;
                repaint();
            }
            return;
        }
        attemptConnect();
    }

    private void attemptConnect() {
        String host = ipText.strip();
        if (!IPV4.matcher(host).matches()) {
            inputError = InputError.INVALID_IP;
            failure = null;
            repaint();
            return;
        }
        int port;
        try {
            port = Integer.parseInt(portText.strip());
        } catch (NumberFormatException e) {
            inputError = InputError.INVALID_PORT;
            failure = null;
            repaint();
            return;
        }
        if (!P2PSettingsService.isValidPort(port)) {
            inputError = InputError.INVALID_PORT;
            failure = null;
            repaint();
            return;
        }

        inputError = null;
        failure = null;
        step = null;
        connecting = true;
        repaint();

        pending = connector.connect(host, port, new ConnectListener() {
            @Override
            public void onStep(Step nextStep) {
                if (screen != Screen.JOIN || !connecting) {
                    return;
                }
                step = nextStep;
                repaint();
            }

            @Override
            public void onConnected(PeerConnection connection) {
                pending = null;
                connecting = false;
                if (screen != Screen.JOIN) {
                    connection.close();
                    return;
                }
                settings.addRecentPeer(host, port);
                recentPeers = settings.recentPeers();
                enterWaitingRoom(connection);
            }

            @Override
            public void onFailed(FailReason reason) {
                pending = null;
                connecting = false;
                if (screen != Screen.JOIN) {
                    return;
                }
                step = null;
                failure = reason;
                repaint();
            }
        });
    }

    private void selectWaitingRoom() {
        if (session == null) {
            return;
        }
        if (session.role() == Role.CLIENT || roomRow == ROOM_ROW_START) {
            session.setReady(!session.isReady());
        }
    }

    private void shiftRoomSetting(int delta) {
        if (session == null) {
            return;
        }
        if (roomRow == ROOM_ROW_MODE) {
            VersusMode[] modes = VersusMode.values();
            int next = Math.floorMod(session.mode().ordinal() + delta, modes.length);
            session.changeSettings(modes[next], session.difficulty());
        } else if (roomRow == ROOM_ROW_DIFFICULTY) {
            Difficulty[] difficulties = Difficulty.values();
            int next = Math.floorMod(session.difficulty().ordinal() + delta, difficulties.length);
            session.changeSettings(session.mode(), difficulties[next]);
        }
    }

    private void shiftRecentColumn(int delta) {
        int recentCount = Math.min(recentPeers.size(), MAX_RECENT_DISPLAY);
        int index = joinRow - JOIN_ROW_CONNECT - 1;
        int target = index + delta * RECENT_ROWS;
        if (target >= 0 && target < recentCount) {
            joinRow = JOIN_ROW_CONNECT + 1 + target;
            repaint();
        }
    }

    private int joinRowCount() {
        return JOIN_ROW_CONNECT + 1 + Math.min(recentPeers.size(), MAX_RECENT_DISPLAY);
    }

    private void enterWaitingRoom(PeerConnection connection) {
        step = null;
        failure = null;
        inputError = null;
        inMatch = false;
        pendingCloseReason = null;
        session = P2PSession.open(connection, initialDifficulty.get());
        session.addListener(new SessionHandler(session));
        screen = Screen.WAITING_ROOM;
        roomRow = session.role() == Role.SERVER ? ROOM_ROW_MODE : ROOM_ROW_START;
        repaint();
    }

    private void returnToP2PMenu(CloseReason reason) {
        screen = Screen.MENU;
        step = null;
        failure = null;
        inputError = null;
        connecting = false;
        inMatch = false;
        pendingCloseReason = null;
        closeReason = reason;
        repaint();
    }

    private void cancelHosting() {
        if (hosting != null) {
            Hosting current = hosting;
            hosting = null;
            current.cancel();
        }
    }

    private void cancelPending() {
        if (pending != null) {
            Pending current = pending;
            pending = null;
            current.cancel();
        }
    }

    private void repaint() {
        repaintCallback.run();
    }

    private final class SessionHandler implements P2PSession.SessionListener {

        private final P2PSession owner;

        SessionHandler(P2PSession owner) {
            this.owner = owner;
        }

        @Override
        public void onSettingsChanged(VersusMode mode, Difficulty difficulty) {
            if (session == owner) {
                repaint();
            }
        }

        @Override
        public void onReadyChanged(boolean mine, boolean opponent) {
            if (session == owner) {
                repaint();
            }
        }

        @Override
        public void onConnectionChanged(ConnectionState state, int roundTripMillis) {
            if (session == owner && !inMatch) {
                repaint();
            }
        }

        @Override
        public void onMatchStarted(VersusMatch match) {
            if (session != owner) {
                return;
            }
            inMatch = true;
            pendingCloseReason = null;
            onMatchStarted.accept(owner, match);
        }

        @Override
        public void onReturnedToWaiting() {
            if (session != owner) {
                return;
            }
            inMatch = false;
            screen = Screen.WAITING_ROOM;
            repaint();
        }

        @Override
        public void onClosed(CloseReason reason) {
            if (session != owner) {
                return;
            }
            session = null;
            if (inMatch) {
                pendingCloseReason = reason;
                boolean hasResult = owner.match().flatMap(VersusMatch::result).isPresent();
                if (!hasResult) {
                    inMatch = false;
                    pendingCloseReason = null;
                    returnToP2PMenu(reason == CloseReason.LOCAL ? null : reason);
                    onMatchAborted.accept(reason);
                }
                return;
            }
            returnToP2PMenu(reason == CloseReason.LOCAL ? null : reason);
        }
    }
}
