package seoultech.se.tetris.ui.versus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AttackEffectsTest {

    private long now = 5_000L;
    private final AttackEffects effects = new AttackEffects(() -> now);

    @Test
    void 처음에는_아무_효과도_없다() {
        assertFalse(effects.isActive());
        assertNull(effects.popupText());
        assertEquals(1.0, effects.popupProgress());
        assertFalse(effects.isQueueRowHighlighted(0));
        assertEquals(0, effects.highlightedBottomRows());
        assertEquals(0.0, effects.bottomRowsHighlightStrength());
    }

    // ------------------------------------------------------------------
    // 공격을 보냄
    // ------------------------------------------------------------------

    @Test
    void 공격을_보내면_보낸_줄_수를_문구로_보여준다() {
        effects.attackSent(3);

        assertTrue(effects.isActive());
        assertEquals("ATTACK +3", effects.popupText());
        assertEquals(0.0, effects.popupProgress());
    }

    @Test
    void 문구는_시간이_지나며_진행되다가_사라진다() {
        effects.attackSent(2);
        now += AttackEffects.POPUP_MS / 2;
        assertEquals(0.5, effects.popupProgress(), 1e-9);

        now += AttackEffects.POPUP_MS / 2;
        assertNull(effects.popupText());
        assertEquals(1.0, effects.popupProgress());
        assertFalse(effects.isActive());
    }

    @Test
    void 다시_공격하면_문구를_처음부터_새_줄_수로_보여준다() {
        effects.attackSent(2);
        now += 600;
        effects.attackSent(4);

        assertEquals("ATTACK +4", effects.popupText());
        assertEquals(0.0, effects.popupProgress());
    }

    @Test
    void 줄_수가_0_이하이면_문구를_보여주지_않는다() {
        effects.attackSent(0);

        assertNull(effects.popupText());
    }

    // ------------------------------------------------------------------
    // 공격을 받음
    // ------------------------------------------------------------------

    @Test
    void 공격을_받으면_새로_붙은_줄만_깜빡인다() {
        effects.attackReceived(2, 5);

        assertFalse(effects.isQueueRowHighlighted(1));
        assertTrue(effects.isQueueRowHighlighted(2));
        assertTrue(effects.isQueueRowHighlighted(4));
        assertFalse(effects.isQueueRowHighlighted(5));
    }

    @Test
    void 대기열은_켜짐과_꺼짐을_번갈아_보여주다가_끝난다() {
        effects.attackReceived(0, 2);
        assertTrue(effects.isQueueRowHighlighted(0));

        now += AttackEffects.QUEUE_BLINK_MS;
        assertFalse(effects.isQueueRowHighlighted(0));

        now += AttackEffects.QUEUE_BLINK_MS;
        assertTrue(effects.isQueueRowHighlighted(0));

        now = 5_000L + AttackEffects.QUEUE_FLASH_MS;
        assertFalse(effects.isQueueRowHighlighted(0));
        assertFalse(effects.isActive());
    }

    @Test
    void 대기_줄이_늘지_않았으면_깜빡이지_않는다() {
        effects.attackReceived(3, 3);
        effects.attackReceived(-1, 2);

        assertFalse(effects.isActive());
    }

    // ------------------------------------------------------------------
    // 대기 줄이 보드에 추가됨
    // ------------------------------------------------------------------

    @Test
    void 대기_줄이_보드에_추가되면_맨_아래_줄을_강조하고_점점_옅어진다() {
        effects.garbageInserted(3);

        assertEquals(3, effects.highlightedBottomRows());
        assertEquals(1.0, effects.bottomRowsHighlightStrength());

        now += AttackEffects.INSERT_MS / 2;
        assertEquals(0.5, effects.bottomRowsHighlightStrength(), 1e-9);

        now += AttackEffects.INSERT_MS / 2;
        assertEquals(0, effects.highlightedBottomRows());
        assertEquals(0.0, effects.bottomRowsHighlightStrength());
    }

    @Test
    void 대기_줄이_보드에_추가되면_대기열_깜빡임은_끝난다() {
        effects.attackReceived(0, 2);
        effects.garbageInserted(2);

        assertFalse(effects.isQueueRowHighlighted(0));
    }

    @Test
    void 추가된_줄이_0_이하이면_강조하지_않는다() {
        effects.garbageInserted(0);

        assertEquals(0, effects.highlightedBottomRows());
    }

    @Test
    void 모든_효과를_한_번에_끝낼_수_있다() {
        effects.attackSent(2);
        effects.attackReceived(0, 2);
        effects.garbageInserted(2);

        effects.clear();

        assertFalse(effects.isActive());
    }

    @Test
    void 일시정지_중에는_애니메이션이_진행되지_않고_재개하면_이어서_진행된다() {
        effects.attackSent(2);
        effects.attackReceived(0, 2);
        effects.garbageInserted(1);

        now += 200;
        effects.pause();
        double pausedProgress = effects.popupProgress();

        now += 5_000;
        assertTrue(effects.isActive());
        assertEquals(pausedProgress, effects.popupProgress(), 1e-9);

        effects.resume();
        assertTrue(effects.isActive());
        assertEquals(pausedProgress, effects.popupProgress(), 1e-9);

        now += AttackEffects.POPUP_MS;
        assertFalse(effects.isActive());
    }

    @Test
    void 실제_시간_효과도_처음에는_비어_있다() {
        assertFalse(AttackEffects.realTime().isActive());
        assertThrows(NullPointerException.class, () -> new AttackEffects(null));
    }
}
