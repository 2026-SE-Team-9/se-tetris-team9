package seoultech.se.tetris.ui.versus;

import java.time.Duration;
import java.util.Objects;
import java.util.function.LongSupplier;

/**
 * 시간제한 모드의 남은 시간. 실제 시간을 기준으로 줄어들며, 멈췄다가 이어서 잴 수 있다. Swing/AWT에 의존하지 않는다.
 *
 * <p>현재 시각은 생성할 때 받은 함수로 읽으므로, 테스트에서는 원하는 시각을 넣어 확인할 수 있다.
 * 처음에는 멈춘 상태이며 {@link #start()}로 재기 시작한다.
 */
public final class CountdownClock {

    /** 남은 시간이 이 값 이하이면 화면에서 강조한다. */
    static final Duration URGENT = Duration.ofSeconds(10);

    private final long limitMillis;
    private final LongSupplier nowMillis;
    /** 지금까지 잰 시간 중 마지막으로 멈춘 시점까지의 합 */
    private long elapsedBeforeMillis;
    /** 재는 중이면 마지막으로 시작한 시각, 멈춘 상태이면 -1 */
    private long runningSinceMillis = -1;
    private boolean stopped;

    /**
     * @param limit     제한 시간. 0보다 커야 한다
     * @param nowMillis 현재 시각(ms)을 돌려주는 함수. 값은 줄어들지 않아야 한다
     */
    public CountdownClock(Duration limit, LongSupplier nowMillis) {
        Objects.requireNonNull(limit, "limit");
        if (limit.isZero() || limit.isNegative()) {
            throw new IllegalArgumentException("제한 시간은 0보다 커야 합니다: " + limit);
        }
        this.limitMillis = limit.toMillis();
        this.nowMillis = Objects.requireNonNull(nowMillis, "nowMillis");
    }

    /** 실제 시간으로 재는 시계 */
    public static CountdownClock realTime(Duration limit) {
        return new CountdownClock(limit, () -> System.nanoTime() / 1_000_000L);
    }

    /** 재기 시작하거나, 멈춘 곳부터 이어서 잰다. 이미 재는 중이거나 끝낸 시계는 그대로 둔다. */
    public void start() {
        if (stopped || isRunning()) {
            return;
        }
        runningSinceMillis = nowMillis.getAsLong();
    }

    /** 잠시 멈춘다. 다시 {@link #start()}하면 남은 시간부터 이어서 줄어든다. */
    public void pause() {
        if (!isRunning()) {
            return;
        }
        elapsedBeforeMillis += nowMillis.getAsLong() - runningSinceMillis;
        runningSinceMillis = -1;
    }

    /** 재기를 끝낸다. 남은 시간은 이 시점의 값으로 고정되고, 다시 시작할 수 없다. */
    public void stop() {
        pause();
        stopped = true;
    }

    public boolean isRunning() {
        return runningSinceMillis >= 0;
    }

    public boolean isStopped() {
        return stopped;
    }

    /** 남은 시간(ms). 0보다 작아지지 않는다. */
    public long remainingMillis() {
        long elapsed = elapsedBeforeMillis;
        if (isRunning()) {
            elapsed += nowMillis.getAsLong() - runningSinceMillis;
        }
        return Math.max(0, limitMillis - elapsed);
    }

    /** 제한 시간이 모두 지났는지 */
    public boolean isExpired() {
        return remainingMillis() == 0;
    }

    /** 남은 시간이 {@link #URGENT} 이하인지 */
    public boolean isUrgent() {
        return remainingMillis() <= URGENT.toMillis();
    }

    /** 화면에 표시할 남은 시간. 예: "03:00", "00:09" */
    public String text() {
        return format(remainingMillis());
    }

    /**
     * 남은 시간을 "분:초"로 나타낸다. 초 단위 미만은 올려서, 시작 직후에는 제한 시간이 그대로 보이고
     * 남은 시간이 정확히 0이 될 때 "00:00"이 된다.
     */
    static String format(long millis) {
        long seconds = (Math.max(0, millis) + 999) / 1000;
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }
}
