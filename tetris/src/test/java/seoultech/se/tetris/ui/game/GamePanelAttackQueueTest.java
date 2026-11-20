package seoultech.se.tetris.ui.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import seoultech.se.tetris.blocks.CellCode;
import seoultech.se.tetris.component.model.Attack;
import seoultech.se.tetris.component.model.GarbageLine;
import seoultech.se.tetris.config.AppSettings.ScreenSize;
import seoultech.se.tetris.engine.Board;
import seoultech.se.tetris.engine.TetrisEngine;

class GamePanelAttackQueueTest {

    /** "X"는 공격 칸, "."은 빈 칸인 공격 줄 */
    private static GarbageLine line(String cells) {
        boolean[] filled = new boolean[cells.length()];
        for (int i = 0; i < cells.length(); i++) {
            filled[i] = cells.charAt(i) == 'X';
        }
        return new GarbageLine(filled);
    }

    private static Attack attack(String... rows) {
        List<GarbageLine> lines = new ArrayList<>();
        for (String row : rows) {
            lines.add(line(row));
        }
        return new Attack(lines);
    }

    private static BufferedImage render(GamePanel panel) {
        Dimension size = panel.getPreferredSize();
        panel.setSize(size);
        BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
        panel.paint(image.getGraphics());
        return image;
    }

    /** 대기열 (행, 열) 칸의 채움 영역 안쪽 픽셀 색. 칸 가운데의 문자를 피해 왼쪽 위 안쪽을 본다. */
    private static Color queuePixel(GamePanel panel, BufferedImage image, int row, int col) {
        int cell = panel.attackQueueCell();
        int x = panel.attackQueueLeft() + col * cell + 2;
        int y = panel.attackQueueTop() + row * cell + 2;
        return new Color(image.getRGB(x, y));
    }

    private static final Color GARBAGE = BlockPalette.GARBAGE;

    @Test
    void 기본으로는_공격_대기열을_표시하지_않는다() {
        GamePanel panel = new GamePanel(new TetrisEngine());

        assertFalse(panel.isAttackQueueVisible());
    }

    @Test
    void 공격_대기열을_켜고_끌_수_있다() {
        GamePanel panel = new GamePanel(new TetrisEngine());

        panel.setAttackQueueVisible(true);
        assertTrue(panel.isAttackQueueVisible());

        panel.setAttackQueueVisible(false);
        assertFalse(panel.isAttackQueueVisible());
    }

    @Test
    void 대기_줄을_넘어온_형태_그대로_위에서부터_표시한다() {
        TetrisEngine engine = new TetrisEngine();
        engine.receiveAttack(attack("XXX.XXXXXX", "X.XXXXXXXX"));
        GamePanel panel = new GamePanel(engine);
        panel.setAttackQueueVisible(true);

        BufferedImage image = render(panel);

        // 첫째 줄: 3번 열이 빈 칸
        assertEquals(GARBAGE, queuePixel(panel, image, 0, 0));
        assertEquals(Color.BLACK, queuePixel(panel, image, 0, 3));
        assertEquals(GARBAGE, queuePixel(panel, image, 0, 9));
        // 둘째 줄: 1번 열이 빈 칸
        assertEquals(Color.BLACK, queuePixel(panel, image, 1, 1));
        assertEquals(GARBAGE, queuePixel(panel, image, 1, 2));
        // 셋째 줄부터는 비어 있다
        assertEquals(Color.BLACK, queuePixel(panel, image, 2, 0));
    }

    @Test
    void 대기열을_끄면_대기_줄이_있어도_그리지_않는다() {
        TetrisEngine engine = new TetrisEngine();
        engine.receiveAttack(attack("XXX.XXXXXX", "X.XXXXXXXX"));
        GamePanel panel = new GamePanel(engine);

        BufferedImage image = render(panel);

        assertEquals(Color.BLACK, queuePixel(panel, image, 0, 0));
    }

    @Test
    void 최대_10줄까지_표시한다() {
        TetrisEngine engine = new TetrisEngine();
        String[] twelve = new String[12];
        for (int i = 0; i < twelve.length; i++) {
            twelve[i] = "XXXXXXXXX.";
        }
        engine.receiveAttack(attack(twelve));
        GamePanel panel = new GamePanel(engine);
        panel.setAttackQueueVisible(true);

        BufferedImage image = render(panel);

        assertEquals(GamePanel.ATTACK_QUEUE_ROWS, engine.getPendingGarbage().size());
        assertEquals(GARBAGE, queuePixel(panel, image, GamePanel.ATTACK_QUEUE_ROWS - 1, 0));
    }

    @Test
    void 색맹_모드에서는_색맹_모드의_공격_칸_색을_쓴다() {
        TetrisEngine engine = new TetrisEngine();
        engine.receiveAttack(attack("XXX.XXXXXX", "X.XXXXXXXX"));
        GamePanel panel = new GamePanel(engine, 1.0, true);
        panel.setAttackQueueVisible(true);

        BufferedImage image = render(panel);

        assertEquals(BlockPalette.colorOf(CellCode.GARBAGE, true), queuePixel(panel, image, 0, 0));
    }

    @Test
    void 모든_화면_크기에서_대기열이_보드_높이_안에_들어간다() {
        for (ScreenSize size : ScreenSize.values()) {
            GamePanel panel = new GamePanel(new TetrisEngine(), size.scale());
            panel.setAttackQueueVisible(true);
            int bottom = panel.attackQueueTop() + GamePanel.ATTACK_QUEUE_ROWS * panel.attackQueueCell();
            int right = panel.attackQueueLeft() + Board.WIDTH * panel.attackQueueCell();
            Dimension preferred = panel.getPreferredSize();

            assertTrue(bottom < preferred.height, size + ": " + bottom + " / " + preferred.height);
            assertTrue(right <= preferred.width, size + ": " + right + " / " + preferred.width);
        }
    }

    @Test
    void 대기_줄이_보드에_추가되면_대기열이_비워진다() {
        TetrisEngine engine = new TetrisEngine();
        engine.receiveAttack(attack("XXX.XXXXXX", "X.XXXXXXXX"));
        GamePanel panel = new GamePanel(engine);
        panel.setAttackQueueVisible(true);

        // 현재 블럭을 고정하면 다음 블럭이 생성되기 전에 대기 줄이 보드에 추가된다
        engine.dropToBottom();
        BufferedImage image = render(panel);

        assertTrue(engine.getPendingGarbage().isEmpty());
        assertEquals(Color.BLACK, queuePixel(panel, image, 0, 0));
    }
}
