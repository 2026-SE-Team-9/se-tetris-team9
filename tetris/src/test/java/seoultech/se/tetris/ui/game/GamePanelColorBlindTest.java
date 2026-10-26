package seoultech.se.tetris.ui.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dimension;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

import seoultech.se.tetris.blocks.Block;
import seoultech.se.tetris.blocks.BlockType;
import seoultech.se.tetris.blocks.ItemType;
import seoultech.se.tetris.component.model.GameState;
import seoultech.se.tetris.engine.Board;

/** 색맹 모드에서 게임 화면이 색맹용 색표와 아이템 테두리로 그려지는지 확인한다. */
class GamePanelColorBlindTest {

    private static final int CELL = 28;
    private static final int MARGIN = 24;

    /** 맨 아래 줄 0열에 T, 1열에 무게추, 2열에 줄 삭제 칸이 있는 보드. */
    private static int[][] board() {
        int[][] grid = new int[Board.HEIGHT][Board.WIDTH];
        grid[Board.HEIGHT - 1][0] = BlockType.T.code();
        grid[Board.HEIGHT - 1][1] = ItemType.WEIGHT.code();
        grid[Board.HEIGHT - 1][2] = ItemType.LINE_CLEAR.code();
        return grid;
    }

    private static BufferedImage render(GamePanel panel) {
        Dimension size = panel.getPreferredSize();
        panel.setSize(size);
        BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
        panel.paint(image.createGraphics());
        return image;
    }

    /** 맨 아래 줄 칸의 (dx, dy) 위치 픽셀. */
    private static int pixel(BufferedImage image, int col, int dx, int dy) {
        return image.getRGB(MARGIN + col * CELL + dx, MARGIN + (Board.VISIBLE_HEIGHT - 1) * CELL + dy) & 0xFFFFFF;
    }

    private static int rgb(java.awt.Color color) {
        return color.getRGB() & 0xFFFFFF;
    }

    @Test
    void 색맹_모드면_색맹용_색으로_그린다() {
        BufferedImage image = render(new GamePanel(new BoardOnly(board()), 1.0, true));

        assertEquals(rgb(BlockPalette.colorOf(BlockType.T.code(), true)), pixel(image, 0, 2, 2));
        assertEquals(rgb(BlockPalette.colorOf(ItemType.LINE_CLEAR.code(), true)), pixel(image, 2, 2, 2));
    }

    @Test
    void 색맹_모드의_아이템_칸에는_안쪽_테두리를_그린다() {
        BufferedImage image = render(new GamePanel(new BoardOnly(board()), 1.0, true));

        assertEquals(rgb(BlockPalette.borderColorOf(ItemType.WEIGHT.code(), true)), pixel(image, 1, 3, 10));
        assertEquals(rgb(BlockPalette.colorOf(ItemType.WEIGHT.code(), true)), pixel(image, 1, 2, 10));
        assertEquals(rgb(BlockPalette.colorOf(BlockType.T.code(), true)), pixel(image, 0, 3, 10));   // 블럭 칸은 테두리 없음
    }

    @Test
    void 기본_모드에서는_기본_색으로_그리고_테두리가_없다() {
        BufferedImage image = render(new GamePanel(new BoardOnly(board()), 1.0));

        assertEquals(rgb(BlockPalette.colorOf(BlockType.T.code())), pixel(image, 0, 2, 2));
        assertEquals(rgb(BlockPalette.colorOf(ItemType.WEIGHT.code())), pixel(image, 1, 3, 10));
    }

    @Test
    void 게임_중에_색맹_모드를_바꾸면_다음_그리기부터_반영된다() {
        GamePanel panel = new GamePanel(new BoardOnly(board()), 1.0);
        assertFalse(panel.isColorBlind());

        panel.setColorBlind(true);

        assertTrue(panel.isColorBlind());
        assertEquals(rgb(BlockPalette.colorOf(BlockType.T.code(), true)), pixel(render(panel), 0, 2, 2));
    }

    /** 고정된 보드만 있는 상태. 현재 블럭과 다음 블럭은 없다. */
    private static final class BoardOnly implements GameState {
        private final int[][] grid;

        BoardOnly(int[][] grid) {
            this.grid = grid;
        }

        @Override public int[][] getBoard() { return grid; }
        @Override public Block getCurrentBlock() { return null; }
        @Override public int getCurrentX() { return 0; }
        @Override public int getCurrentY() { return 0; }
        @Override public Block getNextBlock() { return null; }
        @Override public int getScore() { return 0; }
        @Override public int getLevel() { return 1; }
        @Override public int getLinesCleared() { return 0; }
        @Override public boolean isPaused() { return false; }
        @Override public boolean isGameOver() { return false; }
        @Override public int getGhostY() { return 0; }
    }
}
