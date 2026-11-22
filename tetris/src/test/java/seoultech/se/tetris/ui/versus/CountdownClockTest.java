package seoultech.se.tetris.ui.versus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class CountdownClockTest {

    private static final Duration THREE_MINUTES = Duration.ofMinutes(3);

    /** 테스트에서 시각을 직접 정하는 시계 */
    private long now = 1_000L;
    private final CountdownClock clock = new CountdownClock(THREE_MINUTES, () -> now);

    private void advance(long millis) {
        now += millis;
    }

    @Test
    void 시작_전에는_제한_시간이_그대로이고_줄어들지_않는다() {
        advance(5_000);

        assertFalse(clock.isRunning());
        assertEquals(180_000, clock.remainingMillis());
        assertEquals("03:00", clock.text());
    }

    @Test
    void 시작하면_실제_시간만큼_줄어든다() {
        clock.start();
        advance(1_000);

        assertTrue(clock.isRunning());
        assertEquals(179_000, clock.remainingMillis());
        assertEquals("02:59", clock.text());
    }

    @Test
    void 일초가_지나기_전에는_시작_시간이_그대로_보인다() {
        clock.start();
        advance(999);

        assertEquals("03:00", clock.text());
    }

    @Test
    void 일시정지_동안에는_줄어들지_않고_재개하면_이어서_줄어든다() {
        clock.start();
        advance(10_000);
        clock.pause();
        advance(60_000);

        assertEquals(170_000, clock.remainingMillis());

        clock.start();
        advance(5_000);

        assertEquals(165_000, clock.remainingMillis());
    }

    @Test
    void 중복_시작이나_중복_일시정지는_무시한다() {
        clock.start();
        advance(2_000);
        clock.start();
        advance(2_000);
        clock.pause();
        clock.pause();

        assertEquals(176_000, clock.remainingMillis());
    }

    @Test
    void 제한_시간이_지나면_0에서_멈추고_끝난_것으로_본다() {
        clock.start();
        advance(179_999);
        assertFalse(clock.isExpired());
        assertEquals("00:01", clock.text());

        advance(1);
        assertTrue(clock.isExpired());
        assertEquals("00:00", clock.text());

        advance(30_000);
        assertEquals(0, clock.remainingMillis());
    }

    @Test
    void 끝낸_시계는_그_시점의_남은_시간으로_고정되고_다시_시작할_수_없다() {
        clock.start();
        advance(40_000);
        clock.stop();
        advance(10_000);
        clock.start();
        advance(10_000);

        assertTrue(clock.isStopped());
        assertFalse(clock.isRunning());
        assertEquals("02:20", clock.text());
    }

    @Test
    void 남은_시간이_10초_이하면_강조한다() {
        clock.start();
        advance(169_999);
        assertFalse(clock.isUrgent());

        advance(1);
        assertTrue(clock.isUrgent());
    }

    @Test
    void 분_초_형식으로_나타낸다() {
        assertEquals("03:00", CountdownClock.format(180_000));
        assertEquals("01:05", CountdownClock.format(64_001));
        assertEquals("00:10", CountdownClock.format(10_000));
        assertEquals("00:00", CountdownClock.format(0));
        assertEquals("00:00", CountdownClock.format(-5));
    }

    @Test
    void 제한_시간은_0보다_커야_한다() {
        assertThrows(IllegalArgumentException.class, () -> new CountdownClock(Duration.ZERO, () -> 0L));
        assertThrows(IllegalArgumentException.class, () -> new CountdownClock(Duration.ofSeconds(-1), () -> 0L));
        assertThrows(NullPointerException.class, () -> new CountdownClock(null, () -> 0L));
        assertThrows(NullPointerException.class, () -> new CountdownClock(THREE_MINUTES, null));
    }

    @Test
    void 실제_시간_시계는_시작_시_제한_시간을_보여준다() {
        CountdownClock real = CountdownClock.realTime(THREE_MINUTES);

        assertNotNull(real);
        assertEquals("03:00", real.text());
    }
}
