package seoultech.se.tetris.ui.versus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import seoultech.se.tetris.component.model.Attack;
import seoultech.se.tetris.component.model.GarbageLine;
import seoultech.se.tetris.engine.TetrisEngine;

/** 실제 엔진의 공격 대기열 변화로 줄 공격 애니메이션이 시작되는지 확인한다. */
class AttackEffectWatcherTest {

    private final long now = 0L;
    private final AttackEffects effects = new AttackEffects(() -> now);
    private final TetrisEngine engine = new TetrisEngine();
    private final AttackEffectWatcher watcher = new AttackEffectWatcher(engine, effects);

    AttackEffectWatcherTest() {
        engine.addListener(watcher);
    }

    /** 마지막 열만 빈 공격 줄 {@code count}개 */
    private static Attack attack(int count) {
        boolean[] filled = new boolean[10];
        Arrays.fill(filled, 0, 9, true);
        GarbageLine line = new GarbageLine(filled);
        return new Attack(Collections.nCopies(count, line));
    }

    @Test
    void 공격을_보내면_보낸_줄_수를_표시한다() {
        watcher.onAttack(attack(3));

        assertEquals("ATTACK +3", effects.popupText());
    }

    @Test
    void 공격을_받으면_대기열에_새로_붙은_줄을_깜빡인다() {
        engine.receiveAttack(attack(2));
        assertTrue(effects.isQueueRowHighlighted(0));
        assertTrue(effects.isQueueRowHighlighted(1));

        engine.receiveAttack(attack(3));
        assertFalse(effects.isQueueRowHighlighted(1));
        assertTrue(effects.isQueueRowHighlighted(2));
        assertTrue(effects.isQueueRowHighlighted(4));
        assertFalse(effects.isQueueRowHighlighted(5));
    }

    @Test
    void 대기_줄이_보드에_추가되면_추가된_줄_수만큼_강조한다() {
        engine.receiveAttack(attack(2));

        // 현재 블럭을 고정하면 다음 블럭을 만들기 직전에 대기 줄이 보드에 추가된다
        engine.dropToBottom();

        assertEquals(0, engine.getPendingGarbage().size());
        assertEquals(2, effects.highlightedBottomRows());
    }

    @Test
    void 대기_줄이_바뀌지_않는_보드_변화에는_효과를_시작하지_않는다() {
        engine.moveLeft();
        engine.rotate();

        assertFalse(effects.isActive());
    }

    @Test
    void 나머지_게임_이벤트는_효과와_관계없다() {
        watcher.onLinesCleared(2, 300);
        watcher.onSpeedIncreased(500);
        watcher.onGameOver(1000);

        assertFalse(effects.isActive());
        assertEquals(List.of(), engine.getPendingGarbage());
    }

    @Test
    void 게임_상태와_효과가_있어야_만들_수_있다() {
        assertThrows(NullPointerException.class, () -> new AttackEffectWatcher(null, effects));
        assertThrows(NullPointerException.class, () -> new AttackEffectWatcher(engine, null));
    }
}
