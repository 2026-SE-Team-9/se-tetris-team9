package seoultech.se.tetris.versus.p2p;

import java.security.SecureRandom;
import java.util.Optional;
import seoultech.se.tetris.engine.Difficulty;
import seoultech.se.tetris.versus.VersusMatch;
import seoultech.se.tetris.versus.VersusMode;

/**
 * 연결이 완료된 뒤의 P2P 대전 모드. 대기 상태와 대전을 오가며, 연결이 닫히면 끝난다.
 *
 * <pre>
 * 대기 ──두 플레이어 모두 준비──▶ 대전 ──결과 표시 후──▶ 대기 ...
 *   └──────────── 연결이 닫힘 ───────────┴──────────▶ 끝 (첫 화면으로)
 * </pre>
 *
 * <ul>
 *   <li>대기: 서버가 모드와 난이도를 고르고, 양쪽이 '게임 시작'을 누르면 대전을 시작한다</li>
 *   <li>대전: {@link #match()}로 얻는 {@link VersusMatch}로 진행한다. 상대 자리의 조작 명령은 비어 있고, 일시정지는 없다</li>
 *   <li>대전이 끝나면 준비 상태를 모두 풀고 대기로 돌아간다. 화면은 결과를 보여준 뒤 대기 화면으로 넘어간다</li>
 * </ul>
 *
 * <p>모든 메서드는 Swing 이벤트 스레드에서 호출한다.
 */
public interface P2PSession {

    /**
     * 연결이 완료된 통로로 P2P 대전 모드를 시작한다. {@link ConnectListener#onConnected} 안에서 부른다.
     * 그래야 연결 직후 서버가 보내는 설정을 놓치지 않는다.
     *
     * @param initialDifficulty 설정 화면의 난이도. 서버는 이 값으로 시작하고, 클라이언트는 서버의 설정을 받을 때까지만 쓴다
     */
    static P2PSession open(PeerConnection connection, Difficulty initialDifficulty) {
        SecureRandom random = new SecureRandom();
        return new PeerSession(connection, initialDifficulty, random::nextLong);
    }

    /** 이 PC의 역할 */
    Role role();

    /** 서버가 고른 대전 모드. 처음에는 {@link VersusMode#NORMAL} */
    VersusMode mode();

    /** 서버가 고른 난이도. 처음에는 서버의 설정 난이도 */
    Difficulty difficulty();

    /**
     * 대전 모드와 난이도를 바꾼다. 서버만, 대기 중에만 쓸 수 있다.
     * 바꾸면 양쪽의 준비 상태가 풀린다. 클라이언트가 바뀐 설정을 보고 다시 '게임 시작'을 누르게 하기 위해서다.
     *
     * @throws IllegalStateException 클라이언트이거나 대전 중인 경우
     */
    void changeSettings(VersusMode mode, Difficulty difficulty);

    /** '게임 시작' 버튼을 누르거나 취소한다. 대기 중에만 동작한다. */
    void setReady(boolean ready);

    boolean isReady();

    boolean isOpponentReady();

    /** 진행 중인 대전. 대기 중이면 비어 있다. */
    Optional<VersusMatch> match();

    /** 현재 연결 상태 */
    ConnectionState connectionState();

    /** 최근 10회 왕복 시간의 평균(ms). 아직 잰 적이 없으면 -1 */
    int roundTripMillis();

    /**
     * 대기 중이면 연결을 끊는다. 대전 중(카운트다운 포함)이면 기권패로 처리한 뒤 끊는다.
     * 대전의 {@link VersusMatch#quit()}도 같은 동작을 한다.
     */
    void leave();

    void addListener(SessionListener listener);

    void removeListener(SessionListener listener);

    /** P2P 대전 모드의 진행을 받는 쪽 */
    interface SessionListener {

        /** 서버가 대전 모드나 난이도를 바꿨을 때. 준비 상태도 함께 풀린다. */
        default void onSettingsChanged(VersusMode mode, Difficulty difficulty) {
        }

        /** 어느 한쪽의 준비 상태가 바뀌었을 때 */
        default void onReadyChanged(boolean mine, boolean opponent) {
        }

        /**
         * 대전이 시작됐을 때. 받은 대전은 시작 전({@link seoultech.se.tetris.versus.MatchPhase#READY}) 단계이며,
         * 화면은 로컬 대전과 같이 카운트다운 후 {@link VersusMatch#begin()}을 호출한다.
         */
        void onMatchStarted(VersusMatch match);

        /** 대전이 끝나 대기로 돌아왔을 때. 결과는 대전의 {@link VersusMatch#result()}로 확인한다. */
        default void onReturnedToWaiting() {
        }

        /** 연결 상태나 왕복 시간이 바뀌었을 때 */
        default void onConnectionChanged(ConnectionState state, int roundTripMillis) {
        }

        /**
         * 연결이 닫혔을 때. 이유에 맞는 메시지를 표시하고 P2P 대전 모드 첫 화면으로 돌아간다.
         *
         * <p>상대가 게임 나가기를 선택하면 대전 결과(기권승) 바로 뒤에 {@link CloseReason#PEER_LEFT}가 온다.
         * 이때는 결과 화면을 먼저 보여주고, 플레이어가 확인한 뒤 첫 화면으로 돌아간다.
         */
        void onClosed(CloseReason reason);
    }
}
