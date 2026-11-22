package seoultech.se.tetris.ui.versus;

import java.util.Objects;
import java.util.function.LongSupplier;

import seoultech.se.tetris.ui.game.BoardEffects;

/**
 * 한 플레이어 보드의 줄 공격 애니메이션 상태. Swing/AWT에 의존하지 않는다.
 *
 * <ul>
 *   <li>공격을 보냄: 보드 위에 "ATTACK +N" 문구가 떠오르며 사라진다 ({@link #POPUP_MS})</li>
 *   <li>공격을 받음: 공격 대기열에 새로 붙은 줄이 깜빡인다 ({@link #QUEUE_FLASH_MS})</li>
 *   <li>대기 줄이 보드에 추가됨: 추가된 맨 아래 줄들이 밝게 빛났다가 원래 색으로 돌아온다 ({@link #INSERT_MS})</li>
 * </ul>
 * 애니메이션은 표시에만 쓰며 게임 진행을 멈추지 않는다. 같은 종류의 효과가 다시 생기면 처음부터 다시 보여 준다.
 *
 * <p>현재 시각은 생성할 때 받은 함수로 읽으므로, 테스트에서는 원하는 시각을 넣어 확인할 수 있다.
 */
public final class AttackEffects implements BoardEffects {

    static final long POPUP_MS = 900;
    static final long QUEUE_FLASH_MS = 720;
    /** 대기열 깜빡임에서 켜짐·꺼짐이 바뀌는 간격 */
    static final long QUEUE_BLINK_MS = 120;
    static final long INSERT_MS = 450;

    private static final long NONE = Long.MIN_VALUE;

    private final LongSupplier nowMillis;

    private long popupStart = NONE;
    private int sentLines;

    private long queueFlashStart = NONE;
    private int queueFlashFrom;
    private int queueFlashTo;

    private long insertStart = NONE;
    private int insertedLines;

    /** @param nowMillis 현재 시각(ms)을 돌려주는 함수. 값은 줄어들지 않아야 한다 */
    public AttackEffects(LongSupplier nowMillis) {
        this.nowMillis = Objects.requireNonNull(nowMillis, "nowMillis");
    }

    /** 실제 시간으로 진행하는 효과 */
    public static AttackEffects realTime() {
        return new AttackEffects(() -> System.nanoTime() / 1_000_000L);
    }

    // ------------------------------------------------------------------
    // 효과 시작
    // ------------------------------------------------------------------

    /** 이 플레이어가 상대에게 {@code lines}줄을 보냈다. */
    public void attackSent(int lines) {
        if (lines <= 0) {
            return;
        }
        sentLines = lines;
        popupStart = nowMillis.getAsLong();
    }

    /**
     * 이 플레이어의 공격 대기열이 {@code before}줄에서 {@code after}줄로 늘었다.
     * 새로 붙은 줄은 기존 대기 줄의 아래쪽이므로 위에서 {@code before}번째부터 {@code after - 1}번째 줄이다.
     */
    public void attackReceived(int before, int after) {
        if (after <= before || before < 0) {
            return;
        }
        queueFlashFrom = before;
        queueFlashTo = after;
        queueFlashStart = nowMillis.getAsLong();
    }

    /** 대기 줄 {@code lines}줄이 보드 아래쪽에 추가되었다. 대기열에서는 빠졌으므로 대기열 깜빡임은 끝낸다. */
    public void garbageInserted(int lines) {
        if (lines <= 0) {
            return;
        }
        insertedLines = lines;
        insertStart = nowMillis.getAsLong();
        queueFlashStart = NONE;
    }

    /** 모든 효과를 끝낸다. 대전이 끝나 화면을 더 이상 다시 그리지 않을 때 쓴다. */
    public void clear() {
        popupStart = NONE;
        queueFlashStart = NONE;
        insertStart = NONE;
    }

    // ------------------------------------------------------------------
    // BoardEffects
    // ------------------------------------------------------------------

    @Override
    public String popupText() {
        return isRunning(popupStart, POPUP_MS) ? "ATTACK +" + sentLines : null;
    }

    @Override
    public double popupProgress() {
        return progress(popupStart, POPUP_MS);
    }

    /** 새로 붙은 줄이고, 깜빡임이 켜진 순간이면 강조한다. 첫 순간은 켜짐이다. */
    @Override
    public boolean isQueueRowHighlighted(int row) {
        if (!isRunning(queueFlashStart, QUEUE_FLASH_MS) || row < queueFlashFrom || row >= queueFlashTo) {
            return false;
        }
        long elapsed = nowMillis.getAsLong() - queueFlashStart;
        return (elapsed / QUEUE_BLINK_MS) % 2 == 0;
    }

    @Override
    public int highlightedBottomRows() {
        return isRunning(insertStart, INSERT_MS) ? insertedLines : 0;
    }

    /** 추가된 순간 가장 밝고, 시간이 지날수록 일정하게 어두워진다. */
    @Override
    public double bottomRowsHighlightStrength() {
        return isRunning(insertStart, INSERT_MS) ? 1.0 - progress(insertStart, INSERT_MS) : 0.0;
    }

    @Override
    public boolean isActive() {
        return isRunning(popupStart, POPUP_MS)
                || isRunning(queueFlashStart, QUEUE_FLASH_MS)
                || isRunning(insertStart, INSERT_MS);
    }

    private boolean isRunning(long start, long duration) {
        return start != NONE && nowMillis.getAsLong() - start < duration;
    }

    /** 시작한 뒤 지난 시간의 비율. 시작하지 않았거나 끝났으면 1이다. */
    private double progress(long start, long duration) {
        if (start == NONE) {
            return 1.0;
        }
        long elapsed = nowMillis.getAsLong() - start;
        return Math.min(1.0, Math.max(0.0, (double) elapsed / duration));
    }
}
