package seoultech.se.tetris.versus.p2p;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 확인 신호로 연결 상태를 판정한다. (3차 「네트워크 문제 처리」)
 *
 * <p>시각은 모두 호출하는 쪽이 넘긴다.
 * 실제로는 시스템 시각 대신 되돌아가지 않는 시계(ms)를 넘기고, 테스트에서는 시각만 옮겨서 기다리지 않고 확인한다.
 * 확인 신호({@link P2PMessage.Ping})에 담는 시각도 같은 시계를 써야 왕복 시간이 맞는다.
 *
 * <p>한 스레드에서만 쓴다고 가정한다.
 */
final class ConnectionMonitor {

    /** 확인 신호를 보내는 간격 */
    static final long PING_INTERVAL_MILLIS = 500;

    /** 평균을 내는 최근 왕복 시간 갯수 */
    static final int ROUND_TRIP_WINDOW = 10;

    /** 지연을 판정할 평균 왕복 시간 */
    static final long LAG_ROUND_TRIP_MILLIS = 300;

    /** 마지막 신호 수신 후 이만큼 지나면 지연으로 판정 */
    static final long LAG_SILENCE_MILLIS = 1_000;

    /** 마지막 신호 수신 후 이만큼 지나면 연결 확인 중으로 판정 */
    static final long CHECKING_SILENCE_MILLIS = 3_000;

    /** 마지막 신호 수신 후 이만큼 지나면 연결 끊김으로 판정 */
    static final long DISCONNECT_SILENCE_MILLIS = 10_000;

    private final Deque<Long> roundTrips = new ArrayDeque<>(ROUND_TRIP_WINDOW);
    private long roundTripSum;
    private long lastReceivedAt;
    private boolean peerLeft;

    private ConnectionState reportedState;
    private int reportedRoundTrip;

    /** @param connectedAt 연결이 완료된 시각. 처음에는 이 시각에 신호를 받은 것으로 본다 */
    ConnectionMonitor(long connectedAt) {
        this.lastReceivedAt = connectedAt;
        this.reportedState = ConnectionState.NORMAL;
        this.reportedRoundTrip = -1;
    }

    /** 상대에게서 무엇이든(메시지, 확인 신호) 받았다. */
    void received(long now) {
        lastReceivedAt = Math.max(lastReceivedAt, now);
    }

    /**
     * 확인 신호의 응답({@link P2PMessage.Pong})을 받았다. 왕복 시간을 기록한다.
     *
     * @param sentAt 응답에 담겨 돌아온, 이 PC가 확인 신호를 보낸 시각
     */
    void roundTrip(long sentAt, long now) {
        received(now);
        long elapsed = now - sentAt;
        if (elapsed < 0) {
            return;   // 시계가 맞지 않는 값은 버린다
        }
        if (roundTrips.size() == ROUND_TRIP_WINDOW) {
            roundTripSum -= roundTrips.removeFirst();
        }
        roundTrips.addLast(elapsed);
        roundTripSum += elapsed;
    }

    /** 상대가 직접 연결을 끊었다({@link P2PMessage.Bye}). 이후로는 항상 연결 끊김이다. */
    void peerLeft() {
        peerLeft = true;
    }

    /** 최근 {@value #ROUND_TRIP_WINDOW}회 왕복 시간의 평균(ms, 반올림). 아직 잰 적이 없으면 -1 */
    int roundTripMillis() {
        if (roundTrips.isEmpty()) {
            return -1;
        }
        return (int) Math.round((double) roundTripSum / roundTrips.size());
    }

    /** 지금 시각의 연결 상태. 여러 조건에 해당하면 가장 나쁜 상태다. */
    ConnectionState state(long now) {
        if (peerLeft) {
            return ConnectionState.DISCONNECTED;
        }
        long silence = now - lastReceivedAt;
        if (silence >= DISCONNECT_SILENCE_MILLIS) {
            return ConnectionState.DISCONNECTED;
        }
        if (silence >= CHECKING_SILENCE_MILLIS) {
            return ConnectionState.CHECKING;
        }
        int average = roundTripMillis();
        if (silence >= LAG_SILENCE_MILLIS || average >= LAG_ROUND_TRIP_MILLIS) {
            return ConnectionState.LAGGING;
        }
        return ConnectionState.NORMAL;
    }

    /**
     * 상태를 다시 계산하고, 마지막으로 알린 뒤에 상태나 평균 왕복 시간이 바뀌었는지 돌려준다.
     * 주기적으로 불러서 바뀌었을 때만 화면에 알리는 데 쓴다.
     */
    boolean refresh(long now) {
        ConnectionState state = state(now);
        int roundTrip = roundTripMillis();
        boolean changed = state != reportedState || roundTrip != reportedRoundTrip;
        reportedState = state;
        reportedRoundTrip = roundTrip;
        return changed;
    }
}
