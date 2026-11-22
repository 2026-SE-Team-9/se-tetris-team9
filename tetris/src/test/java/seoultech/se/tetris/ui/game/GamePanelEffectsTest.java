package seoultech.se.tetris.ui.game;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import seoultech.se.tetris.component.model.Attack;
import seoultech.se.tetris.component.model.GarbageLine;
import seoultech.se.tetris.engine.TetrisEngine;

/** 보드 위에 덧그리는 효과(줄 공격 애니메이션)를 그리는지 확인한다. */
class GamePanelEffectsTest {

    /** 정해진 값을 그대로 돌려주는 효과 */
    private static final class FixedEffects implements BoardEffects {
        String popup;
        double popupProgress;
        int highlightedQueueRow = -1;
        int bottomRows;
        double strength;

        @Override public String popupText() { return popup; }
        @Override public double popupProgress() { return popupProgress; }
        @Override public boolean isQueueRowHighlighted(int row) { return row == highlightedQueueRow; }
        @Override public int highlightedBottomRows() { return bottomRows; }
        @Override public double bottomRowsHighlightStrength() { return strength; }
        @Override public boolean isActive() { return true; }
    }

    /** 마지막 열만 빈 공격 줄 */
    private static GarbageLine line() {
        boolean[] filled = new boolean[10];
        Arrays.fill(filled, 0, 9, true);
        return new GarbageLine(filled);
    }

    private static BufferedImage render(GamePanel panel) {
        Dimension size = panel.getPreferredSize();
        panel.setSize(size);
        BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
        panel.paint(image.getGraphics());
        return image;
    }

    /** 보드 영역(여백 24px, 칸 28px, 10x20)에 주어진 색의 픽셀이 있는지 */
    private static boolean boardHas(BufferedImage image, Color color) {
        for (int x = 24; x < 24 + 280; x++) {
            for (int y = 24; y < 24 + 560; y++) {
                if (image.getRGB(x, y) == color.getRGB()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Test
    void 기본으로는_효과가_없다() {
        GamePanel panel = new GamePanel(new TetrisEngine());

        assertSame(BoardEffects.NONE, panel.getEffects());
        assertFalse(BoardEffects.NONE.isActive());
        assertEquals(0, BoardEffects.NONE.highlightedBottomRows());
        assertEquals(0.0, BoardEffects.NONE.bottomRowsHighlightStrength());
        assertEquals(1.0, BoardEffects.NONE.popupProgress());
        assertFalse(BoardEffects.NONE.isQueueRowHighlighted(0));
    }

    @Test
    void 효과를_null로_정하면_효과가_없는_것으로_본다() {
        GamePanel panel = new GamePanel(new TetrisEngine());
        panel.setEffects(new FixedEffects());

        panel.setEffects(null);

        assertSame(BoardEffects.NONE, panel.getEffects());
    }

    @Test
    void 공격을_보낸_문구를_보드_위에_그린다() {
        GamePanel panel = new GamePanel(new TetrisEngine());
        assertFalse(boardHas(render(panel), GamePanel.ATTACK_POPUP_COLOR));

        FixedEffects effects = new FixedEffects();
        effects.popup = "ATTACK +2";
        effects.popupProgress = 0.1;
        panel.setEffects(effects);

        assertTrue(boardHas(render(panel), GamePanel.ATTACK_POPUP_COLOR));
    }

    @Test
    void 공격_대기열에_새로_붙은_줄을_강조해_그린다() {
        TetrisEngine engine = new TetrisEngine();
        engine.receiveAttack(new Attack(List.of(line(), line())));
        GamePanel panel = new GamePanel(engine);
        panel.setAttackQueueVisible(true);
        FixedEffects effects = new FixedEffects();
        effects.highlightedQueueRow = 1;
        panel.setEffects(effects);

        BufferedImage image = render(panel);
        int cell = panel.attackQueueCell();
        int x = panel.attackQueueLeft() + 2;

        assertEquals(BlockPalette.GARBAGE.getRGB(), image.getRGB(x, panel.attackQueueTop() + 2));
        assertEquals(GamePanel.QUEUE_HIGHLIGHT_COLOR.getRGB(), image.getRGB(x, panel.attackQueueTop() + cell + 2));
    }

    @Test
    void 보드에_추가된_맨_아래_줄을_밝게_덮는다() {
        GamePanel panel = new GamePanel(new TetrisEngine());
        int x = 24 + 2;
        int bottomY = 24 + 19 * 28 + 2;
        int aboveY = 24 + 17 * 28 + 2;
        Color before = new Color(render(panel).getRGB(x, bottomY));

        FixedEffects effects = new FixedEffects();
        effects.bottomRows = 2;
        effects.strength = 1.0;
        panel.setEffects(effects);
        BufferedImage image = render(panel);

        assertTrue(new Color(image.getRGB(x, bottomY)).getRed() > before.getRed() + 100);
        assertEquals(before.getRGB(), image.getRGB(x, aboveY));
    }

    @Test
    void 효과_값이_범위를_벗어나도_그릴_수_있다() {
        GamePanel panel = new GamePanel(new TetrisEngine(), 1.5, true);
        FixedEffects effects = new FixedEffects();
        effects.popup = "ATTACK +4";
        effects.popupProgress = 3.0;
        effects.bottomRows = 99;
        effects.strength = -1.0;
        panel.setEffects(effects);

        assertDoesNotThrow(() -> render(panel));
    }
}
