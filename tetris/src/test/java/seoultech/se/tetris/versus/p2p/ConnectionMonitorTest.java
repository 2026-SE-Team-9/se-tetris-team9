package seoultech.se.tetris.versus.p2p;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ConnectionMonitor")
class ConnectionMonitorTest {

    /** 연결이 완료된 시각. 0이 아닌 값으로 두어 시각을 빼는 계산을 확인한다. */
    private static final long START = 1_000_000;

    private final ConnectionMonitor monitor = new ConnectionMonitor(START);

    /** 지금 시각에 왕복 시간 {@code millis}짜리 응답을 받는다. */
    private void roundTripAt(long now, long millis) {
        monitor.roundTrip(now - millis, now);
    }

    @Test
    @DisplayName("판정 기준은 팀 결정과 같다")
    void thresholds() {
        assertEquals(500, ConnectionMonitor.PING_INTERVAL_MILLIS);
        assertEquals(10, ConnectionMonitor.ROUND_TRIP_WINDOW);
        assertEquals(300, ConnectionMonitor.LAG_ROUND_TRIP_MILLIS);
        assertEquals(1_000, ConnectionMonitor.LAG_SILENCE_MILLIS);
        assertEquals(3_000, ConnectionMonitor.CHECKING_SILENCE_MILLIS);
        assertEquals(10_000, ConnectionMonitor.DISCONNECT_SILENCE_MILLIS);
    }

    // ------------------------------------------------------------------
    // 왕복 시간
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("왕복 시간")
    class RoundTrip {

        @Test
        @DisplayName("아직 잰 적이 없으면 -1이다")
        void noneYet() {
            assertEquals(-1, monitor.roundTripMillis());
        }

        @Test
        @DisplayName("받은 응답의 왕복 시간을 평균 낸다")
        void average() {
            roundTripAt(START + 100, 40);
            roundTripAt(START + 600, 60);

            assertEquals(50, monitor.roundTripMillis());
        }

        @Test
        @DisplayName("평균은 반올림한다")
        void rounded() {
            roundTripAt(START + 100, 10);
            roundTripAt(START + 600, 11);

            assertEquals(11, monitor.roundTripMillis());   // 10.5
        }

        @Test
        @DisplayName("최근 10회만 평균에 넣는다")
        void onlyRecentTen() {
            long now = START;
            roundTripAt(now += 500, 1_000);   // 오래된 값. 밀려나야 한다
            for (int i = 0; i < 10; i++) {
                roundTripAt(now += 500, 20);
            }

            assertEquals(20, monitor.roundTripMillis());
        }

        @Test
        @DisplayName("보낸 시각이 받은 시각보다 늦은 응답은 버린다")
        void ignoresNegative() {
            monitor.roundTrip(START + 200, START + 100);

            assertEquals(-1, monitor.roundTripMillis());
        }
    }

    // ------------------------------------------------------------------
    // 상태 판정
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("상태 판정")
    class State {

        @Test
        @DisplayName("연결 직후에는 정상이다")
        void normalAtStart() {
            assertEquals(ConnectionState.NORMAL, monitor.state(START));
        }

        @Test
        @DisplayName("평균 왕복 시간이 300ms 미만이면 정상, 300ms 이상이면 지연이다")
        void lagByRoundTrip() {
            roundTripAt(START + 500, 299);
            assertEquals(ConnectionState.NORMAL, monitor.state(START + 500));

            ConnectionMonitor slow = new ConnectionMonitor(START);
            slow.roundTrip(START + 200, START + 500);   // 300ms
            assertEquals(ConnectionState.LAGGING, slow.state(START + 500));
        }

        @Test
        @DisplayName("마지막 신호 수신 후 1초가 지나면 지연이다")
        void lagBySilence() {
            assertEquals(ConnectionState.NORMAL, monitor.state(START + 999));
            assertEquals(ConnectionState.LAGGING, monitor.state(START + 1_000));
        }

        @Test
        @DisplayName("마지막 신호 수신 후 3초가 지나면 연결 확인 중이다")
        void checking() {
            assertEquals(ConnectionState.LAGGING, monitor.state(START + 2_999));
            assertEquals(ConnectionState.CHECKING, monitor.state(START + 3_000));
        }

        @Test
        @DisplayName("마지막 신호 수신 후 10초가 지나면 연결 끊김이다")
        void disconnected() {
            assertEquals(ConnectionState.CHECKING, monitor.state(START + 9_999));
            assertEquals(ConnectionState.DISCONNECTED, monitor.state(START + 10_000));
        }

        @Test
        @DisplayName("확인 신호가 아닌 메시지를 받아도 마지막 수신 시각이 갱신된다")
        void anyMessageCounts() {
            monitor.received(START + 2_500);

            assertEquals(ConnectionState.NORMAL, monitor.state(START + 3_000));
        }

        @Test
        @DisplayName("응답을 받으면 마지막 수신 시각도 갱신된다")
        void pongCounts() {
            roundTripAt(START + 2_500, 20);

            assertEquals(ConnectionState.NORMAL, monitor.state(START + 3_000));
        }

        @Test
        @DisplayName("신호가 다시 오면 지연에서 정상으로 돌아온다")
        void recovers() {
            assertEquals(ConnectionState.LAGGING, monitor.state(START + 1_500));

            roundTripAt(START + 1_600, 30);

            assertEquals(ConnectionState.NORMAL, monitor.state(START + 1_600));
        }

        @Test
        @DisplayName("여러 조건에 해당하면 가장 나쁜 상태다 (평균 지연 + 3초 무응답 → 연결 확인 중)")
        void worstWins() {
            monitor.roundTrip(START, START + 400);   // 평균 400ms

            assertEquals(ConnectionState.CHECKING, monitor.state(START + 400 + 3_000));
        }

        @Test
        @DisplayName("상대가 직접 끊으면 시각과 상관없이 바로 연결 끊김이다")
        void peerLeftImmediately() {
            monitor.peerLeft();

            assertEquals(ConnectionState.DISCONNECTED, monitor.state(START));
        }

        @Test
        @DisplayName("늦게 처리된 이전 시각의 수신은 마지막 수신 시각을 되돌리지 않는다")
        void receivedNeverGoesBack() {
            monitor.received(START + 2_000);
            monitor.received(START + 1_000);

            assertEquals(ConnectionState.NORMAL, monitor.state(START + 2_900));
        }
    }

    // ------------------------------------------------------------------
    // 변화 알림
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("변화 확인")
    class Refresh {

        @Test
        @DisplayName("처음 상태(정상, 왕복 시간 없음)와 같으면 바뀌지 않은 것이다")
        void unchangedAtStart() {
            assertFalse(monitor.refresh(START + 500));
        }

        @Test
        @DisplayName("왕복 시간을 처음 재면 바뀐 것이다")
        void firstRoundTrip() {
            roundTripAt(START + 500, 30);

            assertTrue(monitor.refresh(START + 500));
            assertFalse(monitor.refresh(START + 600));   // 이미 알렸다
        }

        @Test
        @DisplayName("평균 왕복 시간이 달라지면 바뀐 것이다")
        void roundTripChanged() {
            roundTripAt(START + 500, 30);
            monitor.refresh(START + 500);

            roundTripAt(START + 1_000, 50);

            assertTrue(monitor.refresh(START + 1_000));
        }

        @Test
        @DisplayName("시간이 흘러 상태가 달라지면 바뀐 것이다")
        void stateChangedOverTime() {
            assertFalse(monitor.refresh(START + 900));
            assertTrue(monitor.refresh(START + 1_000));   // 지연
            assertFalse(monitor.refresh(START + 2_000));
            assertTrue(monitor.refresh(START + 3_000));   // 연결 확인 중
        }
    }
}
