package seoultech.se.tetris.ui.versus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import seoultech.se.tetris.engine.Difficulty;
import seoultech.se.tetris.versus.EndReason;
import seoultech.se.tetris.versus.LocalMatch;
import seoultech.se.tetris.versus.MatchPhase;
import seoultech.se.tetris.versus.MatchResult;
import seoultech.se.tetris.versus.Side;
import seoultech.se.tetris.versus.VersusMode;

/** 실제 로컬 대전과 시각을 직접 정하는 시계로, 시간제한 모드의 진행과 시간 종료를 확인한다. */
class TimeLimitWatcherTest {

    private static final long LIMIT = VersusMode.TIME_LIMIT.timeLimit().orElseThrow().toMillis();

    private long now;
    private final LocalMatch match = LocalMatch.create(VersusMode.TIME_LIMIT, Difficulty.NORMAL, 11L);
    private final CountdownClock clock = new CountdownClock(Duration.ofMillis(LIMIT), () -> now);
    private final TimeLimitWatcher watcher = new TimeLimitWatcher(match, clock);

    TimeLimitWatcherTest() {
        match.addListener(watcher);
    }

    @Test
    void 대전이_시작되기_전에는_재지_않는다() {
        now += 5_000;

        assertFalse(clock.isRunning());
        assertEquals(LIMIT, clock.remainingMillis());
    }

    @Test
    void 대전이_시작되면_재기_시작한다() {
        match.begin();
        now += 3_000;

        assertTrue(clock.isRunning());
        assertEquals(LIMIT - 3_000, clock.remainingMillis());
    }

    @Test
    void 일시정지하면_타이머도_멈추고_재개하면_이어서_줄어든다() {
        match.begin();
        now += 10_000;
        match.pause();
        now += 60_000;
        watcher.tick();

        assertEquals(MatchPhase.PAUSED, match.phase());
        assertEquals(LIMIT - 10_000, clock.remainingMillis());

        match.resume();
        now += 1_000;

        assertEquals(LIMIT - 11_000, clock.remainingMillis());
    }

    @Test
    void 제한_시간_전에는_시간_종료를_알리지_않는다() {
        match.begin();
        now += LIMIT - 1;
        watcher.tick();

        assertEquals(MatchPhase.PLAYING, match.phase());
        assertTrue(match.result().isEmpty());
    }

    @Test
    void 제한_시간이_끝나면_점수가_높은_쪽이_승리한다() {
        match.begin();
        // P1만 블럭을 바로 떨어뜨려 점수를 얻는다
        match.controller(Side.P1).orElseThrow().dropToBottom();
        now += LIMIT;
        watcher.tick();

        MatchResult result = match.result().orElseThrow();
        assertEquals(MatchPhase.ENDED, match.phase());
        assertEquals(EndReason.TIME_UP, result.reason());
        assertTrue(result.p1Score() > result.p2Score());
        assertSame(Side.P1, result.winner());
    }

    @Test
    void 제한_시간이_끝났을_때_점수가_같으면_무승부다() {
        match.begin();
        now += LIMIT;
        watcher.tick();

        MatchResult result = match.result().orElseThrow();
        assertTrue(result.isDraw());
        assertEquals(EndReason.TIME_UP, result.reason());
        assertNull(result.winner());
    }

    @Test
    void 일시정지_중에는_제한_시간이_지나도_끝나지_않는다() {
        match.begin();
        now += LIMIT - 1_000;
        match.pause();
        now += 10_000;
        watcher.tick();

        assertEquals(MatchPhase.PAUSED, match.phase());
        assertFalse(clock.isExpired());
    }

    @Test
    void 대전이_끝나면_타이머가_멈춘다() {
        match.begin();
        now += 30_000;
        match.quit();
        now += 30_000;

        assertTrue(clock.isStopped());
        assertEquals(LIMIT - 30_000, clock.remainingMillis());
    }

    @Test
    void 시간_종료_후의_tick은_결과를_바꾸지_않는다() {
        match.begin();
        now += LIMIT;
        watcher.tick();
        MatchResult first = match.result().orElseThrow();

        now += 5_000;
        watcher.tick();

        assertEquals(first, match.result().orElseThrow());
    }

    @Test
    void 대전_종료_알림을_받으면_시계를_고정한다() {
        match.begin();
        watcher.onMatchEnded(MatchResult.win(Side.P2, EndReason.TOP_OUT, 0, 0));

        assertTrue(clock.isStopped());
    }

    @Test
    void 대전과_시계가_있어야_만들_수_있다() {
        assertSame(clock, watcher.clock());
        assertThrows(NullPointerException.class, () -> new TimeLimitWatcher(null, clock));
        assertThrows(NullPointerException.class, () -> new TimeLimitWatcher(match, null));
    }
}
