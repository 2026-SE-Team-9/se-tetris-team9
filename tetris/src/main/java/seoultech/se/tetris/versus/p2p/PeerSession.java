package seoultech.se.tetris.versus.p2p;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.LongSupplier;
import seoultech.se.tetris.engine.Difficulty;
import seoultech.se.tetris.versus.MatchPhase;
import seoultech.se.tetris.versus.VersusMatch;
import seoultech.se.tetris.versus.VersusMode;

/**
 * {@link P2PSession}의 구현. 연결이 완료된 통로 하나로 대기와 대전을 오간다.
 *
 * <p>대기 메시지({@link P2PMessage.Settings}, {@link P2PMessage.Ready}, {@link P2PMessage.Start})는 직접 처리하고,
 * 그 밖의 메시지는 진행 중인 대전에 넘긴다.
 */
final class PeerSession implements P2PSession {

    private final PeerConnection connection;
    private final LongSupplier seeds;
    private final List<SessionListener> listeners = new ArrayList<>();

    private VersusMode mode = VersusMode.NORMAL;
    private Difficulty difficulty;
    private boolean ready;
    private boolean opponentReady;
    private P2PMatch match;
    private boolean closed;

    /**
     * @param initialDifficulty 처음 난이도. 서버는 자신의 설정 난이도를 넘긴다. 클라이언트는 서버가 알려줄 때까지 임시로 쓴다
     * @param seeds             대전마다 블럭 순서를 정할 시드를 만든다. 서버에서만 쓴다
     */
    PeerSession(PeerConnection connection, Difficulty initialDifficulty, LongSupplier seeds) {
        this.connection = Objects.requireNonNull(connection, "connection");
        this.difficulty = Objects.requireNonNull(initialDifficulty, "initialDifficulty");
        this.seeds = Objects.requireNonNull(seeds, "seeds");
        connection.addListener(new ConnectionListener());
        if (role() == Role.SERVER) {
            connection.send(new P2PMessage.Settings(mode, difficulty));
        }
    }

    // ------------------------------------------------------------------
    // 조회
    // ------------------------------------------------------------------

    @Override
    public Role role() {
        return connection.role();
    }

    @Override
    public VersusMode mode() {
        return mode;
    }

    @Override
    public Difficulty difficulty() {
        return difficulty;
    }

    @Override
    public boolean isReady() {
        return ready;
    }

    @Override
    public boolean isOpponentReady() {
        return opponentReady;
    }

    @Override
    public Optional<VersusMatch> match() {
        return Optional.ofNullable(match);
    }

    @Override
    public ConnectionState connectionState() {
        return connection.state();
    }

    @Override
    public int roundTripMillis() {
        return connection.roundTripMillis();
    }

    @Override
    public void addListener(SessionListener listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    @Override
    public void removeListener(SessionListener listener) {
        listeners.remove(listener);
    }

    private boolean waiting() {
        return match == null && !closed;
    }

    // ------------------------------------------------------------------
    // 대기
    // ------------------------------------------------------------------

    @Override
    public void changeSettings(VersusMode newMode, Difficulty newDifficulty) {
        Objects.requireNonNull(newMode, "mode");
        Objects.requireNonNull(newDifficulty, "difficulty");
        if (role() != Role.SERVER) {
            throw new IllegalStateException("대전 모드와 난이도는 서버만 바꿀 수 있습니다.");
        }
        if (match != null) {
            throw new IllegalStateException("대전 중에는 설정을 바꿀 수 없습니다.");
        }
        if (closed) {
            return;
        }
        mode = newMode;
        difficulty = newDifficulty;
        ready = false;
        opponentReady = false;
        connection.send(new P2PMessage.Settings(mode, difficulty));
        notifySettingsChanged();
    }

    @Override
    public void setReady(boolean value) {
        if (!waiting() || ready == value) {
            return;
        }
        ready = value;
        connection.send(new P2PMessage.Ready(value));
        notifyReadyChanged();
        startIfBothReady();
    }

    /** 서버: 두 플레이어가 모두 준비되면 대전을 시작한다. */
    private void startIfBothReady() {
        if (role() != Role.SERVER || !ready || !opponentReady || !waiting()) {
            return;
        }
        P2PMessage.Start start = new P2PMessage.Start(mode, difficulty, seeds.getAsLong());
        connection.send(start);
        startMatch(start);
    }

    private void startMatch(P2PMessage.Start start) {
        mode = start.mode();
        difficulty = start.difficulty();
        ready = false;
        opponentReady = false;
        match = P2PMatch.create(role(), start, connection, this::closeConnection, this::returnToWaiting);
        VersusMatch started = match;
        for (SessionListener listener : List.copyOf(listeners)) {
            listener.onMatchStarted(started);
        }
    }

    /** 대전 결과가 정해져 대기로 돌아간다. 대전의 리스너가 결과를 모두 받은 뒤에 불린다. */
    private void returnToWaiting() {
        match = null;
        if (closed) {
            return;
        }
        for (SessionListener listener : List.copyOf(listeners)) {
            listener.onReturnedToWaiting();
        }
    }

    @Override
    public void leave() {
        if (closed) {
            return;
        }
        if (match != null && match.phase() != MatchPhase.ENDED) {
            match.quit();   // 기권한 뒤 closeConnection을 부른다
        } else {
            closeConnection();
        }
    }

    private void closeConnection() {
        connection.close();
    }

    // ------------------------------------------------------------------
    // 받은 메시지
    // ------------------------------------------------------------------

    private void settingsReceived(P2PMessage.Settings settings) {
        if (role() != Role.CLIENT || !waiting()) {
            return;
        }
        mode = settings.mode();
        difficulty = settings.difficulty();
        if (ready) {
            // 바뀌기 전 설정으로 누른 준비가 서버에 늦게 도착했을 수 있으므로 취소를 알린다
            connection.send(new P2PMessage.Ready(false));
        }
        ready = false;
        opponentReady = false;
        notifySettingsChanged();
    }

    private void readyReceived(boolean value) {
        if (!waiting()) {
            return;
        }
        opponentReady = value;
        notifyReadyChanged();
        startIfBothReady();
    }

    private void startReceived(P2PMessage.Start start) {
        if (role() == Role.CLIENT && waiting()) {
            startMatch(start);
        }
    }

    private void notifySettingsChanged() {
        for (SessionListener listener : List.copyOf(listeners)) {
            listener.onSettingsChanged(mode, difficulty);
            listener.onReadyChanged(ready, opponentReady);
        }
    }

    private void notifyReadyChanged() {
        for (SessionListener listener : List.copyOf(listeners)) {
            listener.onReadyChanged(ready, opponentReady);
        }
    }

    /** 통로에서 일어난 일을 대기 상태나 대전에 반영한다. */
    private final class ConnectionListener implements PeerListener {

        @Override
        public void onMessage(P2PMessage message) {
            switch (message) {
                case P2PMessage.Settings s -> settingsReceived(s);
                case P2PMessage.Ready r -> readyReceived(r.ready());
                case P2PMessage.Start s -> startReceived(s);
                default -> {
                    if (match != null) {
                        match.handle(message);
                    }
                }
            }
        }

        @Override
        public void onStateChanged(ConnectionState state, int roundTripMillis) {
            for (SessionListener listener : List.copyOf(listeners)) {
                listener.onConnectionChanged(state, roundTripMillis);
            }
        }

        @Override
        public void onClosed(CloseReason reason) {
            closed = true;
            if (match != null) {
                match.connectionClosed();
            }
            for (SessionListener listener : List.copyOf(listeners)) {
                listener.onClosed(reason);
            }
        }
    }
}
