package seoultech.se.tetris.ui.game;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import seoultech.se.tetris.blocks.CellCode;
import seoultech.se.tetris.component.model.Attack;
import seoultech.se.tetris.component.model.GarbageLine;
import seoultech.se.tetris.engine.TetrisEngine;

/** 대전에서 받은 공격 줄(공격 칸)이 보드에 추가된 뒤에도 보드를 그릴 수 있는지 확인한다. */
class GamePanelGarbageTest {

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

    /** 공격 2줄을 받고 현재 블럭을 고정해, 다음 블럭이 생성되기 전에 공격 줄이 보드 아래쪽에 추가되게 한다. */
    private static TetrisEngine engineWithGarbageOnBoard() {
        TetrisEngine engine = new TetrisEngine();
        engine.receiveAttack(new Attack(List.of(line(), line())));
        engine.dropToBottom();
        return engine;
    }

    @Test
    void 보드_아래쪽에_공격_칸이_추가된다() {
        int[][] board = engineWithGarbageOnBoard().getBoard();

        assertTrue(CellCode.isGarbage(board[board.length - 1][0]));
        assertTrue(CellCode.isGarbage(board[board.length - 2][0]));
    }

    @Test
    void 공격_칸이_있는_보드를_예외_없이_그린다() {
        GamePanel panel = new GamePanel(engineWithGarbageOnBoard());

        assertDoesNotThrow(() -> render(panel));
    }

    @Test
    void 색맹_모드에서도_공격_칸이_있는_보드를_예외_없이_그린다() {
        GamePanel panel = new GamePanel(engineWithGarbageOnBoard(), 1.0, true);

        assertDoesNotThrow(() -> render(panel));
    }

    @Test
    void 공격_칸은_회색으로_그린다() {
        GamePanel panel = new GamePanel(engineWithGarbageOnBoard());
        BufferedImage image = render(panel);

        // 보드 맨 아래 줄 첫 칸의 채움 영역 안쪽 (여백 24px, 칸 28px, 가운데 문자를 피해 왼쪽 위를 본다)
        int x = 24 + 2;
        int y = 24 + 19 * 28 + 2;
        assertEquals(BlockPalette.GARBAGE.getRGB(), image.getRGB(x, y));
    }
}
