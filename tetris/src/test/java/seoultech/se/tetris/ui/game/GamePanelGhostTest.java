package seoultech.se.tetris.ui.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dimension;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

import seoultech.se.tetris.blocks.Block;
import seoultech.se.tetris.blocks.BlockType;
import seoultech.se.tetris.blocks.ItemType;
import seoultech.se.tetris.component.model.GameState;
import seoultech.se.tetris.engine.Board;

class GamePanelGhostTest {

    private static final int CELL = 28;
    private static final int MARGIN = 24;

    private static BufferedImage render(GameState state) {
        GamePanel panel = new GamePanel(state);
        Dimension size = panel.getPreferredSize();
        panel.setSize(size);
        BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
        panel.paint(image.createGraphics());
        return image;
    }

    private static int pixel(BufferedImage image, int row, int col, int dx, int dy) {
        return image.getRGB(MARGIN + col * CELL + dx, MARGIN + row * CELL + dy) & 0xFFFFFF;
    }

    private static int textPixels(BufferedImage image, int row, int col) {
        int count = 0;
        int gray = 0xC8C8C8;
        for (int y = 2; y < CELL - 2; y++) {
            for (int x = 2; x < CELL - 2; x++) {
                if (pixel(image, row, col, x, y) == gray) {
                    count++;
                }
            }
        }
        return count;
    }

    @Test
    void 고스트는_착지_위치에_흐린_칸과_샵_문자를_그린다() {
        // O형은 4칸이며, 현재 위치와 고스트 위치가 겹치지 않는다.
        Block current = Block.of(BlockType.O, 0, 3);
        BufferedImage image = render(new StubState(current, 10, false, false));

        assertNotEquals(0, pixel(image, 10, 4, 2, 2));
        assertTrue(textPixels(image, 10, 4) > 0);
        assertTrue(textPixels(image, 10, 5) > 0);
        assertTrue(textPixels(image, 11, 4) > 0);
        assertTrue(textPixels(image, 11, 5) > 0);
        assertEquals(0, pixel(image, 10, 3, 2, 2));
        assertEquals(BlockPalette.colorOf(BlockType.O).getRGB() & 0xFFFFFF,
                pixel(image, 0, 4, 2, 2));
    }

    @Test
    void 아이템_블럭도_자기_모양으로_착지_위치에_표시한다() {
        // 무게추의 첫 접촉 위치를 넘겨주면 위 2칸과 아래 4칸만 표시한다.
        Block weight = Block.of(ItemType.WEIGHT, 0, 2);
        BufferedImage image = render(new StubState(weight, 10, false, false));

        assertTrue(textPixels(image, 10, 3) > 0);
        assertTrue(textPixels(image, 10, 4) > 0);
        for (int col = 2; col <= 5; col++) {
            assertTrue(textPixels(image, 11, col) > 0);
        }
        assertEquals(0, pixel(image, 10, 2, 2, 2));

        // 포함형 아이템도 원래 테트로미노 모양을 사용하고 아이템 문자를 복제하지 않는다.
        Block embedded = Block.of(BlockType.O, 0, 3).withItem(ItemType.LINE_CLEAR, 0, 1);
        BufferedImage embeddedImage = render(new StubState(embedded, 10, false, false));
        assertTrue(textPixels(embeddedImage, 10, 4) > 0);
        assertNotEquals(BlockPalette.colorOf(ItemType.LINE_CLEAR.code()).getRGB() & 0xFFFFFF,
                pixel(embeddedImage, 10, 4, 2, 2));
    }

    @Test
    void 줄_삭제와_게임_오버_중에는_고스트를_숨긴다() {
        Block current = Block.of(BlockType.O, 0, 3);
        BufferedImage clearing = render(new StubState(current, 10, true, false));
        BufferedImage gameOver = render(new StubState(current, 10, false, true));

        assertEquals(0, textPixels(clearing, 10, 4));
        assertEquals(0, textPixels(gameOver, 10, 4));
    }

    @Test
    void 이미_바닥에_닿은_블럭은_고스트로_덧그리지_않는다() {
        Block current = Block.of(BlockType.O, 18, 3);
        BufferedImage image = render(new StubState(current, 18, false, false));

        assertEquals(BlockPalette.colorOf(BlockType.O).getRGB() & 0xFFFFFF,
                pixel(image, 18, 4, 2, 2));
        assertEquals(0, textPixels(image, 18, 4));
    }

    private record StubState(Block current, int ghostY, boolean clearing, boolean gameOver) implements GameState {
        @Override public int[][] getBoard() { return new int[Board.HEIGHT][Board.WIDTH]; }
        @Override public Block getCurrentBlock() { return current; }
        @Override public int getCurrentX() { return current.col(); }
        @Override public int getCurrentY() { return current.row(); }
        @Override public Block getNextBlock() { return null; }
        @Override public int getScore() { return 0; }
        @Override public int getLevel() { return 1; }
        @Override public int getLinesCleared() { return 0; }
        @Override public boolean isPaused() { return false; }
        @Override public boolean isGameOver() { return gameOver; }
        @Override public int getGhostY() { return ghostY; }
        @Override public boolean isClearing() { return clearing; }
    }
}
