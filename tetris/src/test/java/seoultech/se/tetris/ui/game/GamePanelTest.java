package seoultech.se.tetris.ui.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

import seoultech.se.tetris.blocks.Block;
import seoultech.se.tetris.blocks.BlockType;
import seoultech.se.tetris.component.model.GameState;
import seoultech.se.tetris.config.AppSettings.ScreenSize;
import seoultech.se.tetris.engine.Board;
import seoultech.se.tetris.engine.TetrisEngine;

class GamePanelTest {

    private static final Dimension BASE_SIZE = new Dimension(10 * 28 + 24 * 3 + 150, 20 * 28 + 24 * 2);

    @Test
    void 보통_크기는_기존_화면_크기와_같다() {
        GamePanel panel = new GamePanel(new TetrisEngine(), ScreenSize.MEDIUM.scale());

        assertEquals(BASE_SIZE, panel.getPreferredSize());
        assertEquals(BASE_SIZE, new GamePanel(new TetrisEngine()).getPreferredSize());
    }

    @Test
    void 화면_크기_설정은_작게_보통_크게_순서로_커진다() {
        Dimension small = new GamePanel(new TetrisEngine(), ScreenSize.SMALL.scale()).getPreferredSize();
        Dimension medium = new GamePanel(new TetrisEngine(), ScreenSize.MEDIUM.scale()).getPreferredSize();
        Dimension large = new GamePanel(new TetrisEngine(), ScreenSize.LARGE.scale()).getPreferredSize();

        assertTrue(small.width < medium.width && medium.width < large.width);
        assertTrue(small.height < medium.height && medium.height < large.height);
    }

    @Test
    void 배율을_바꾸면_선호_크기가_바뀐다() {
        GamePanel panel = new GamePanel(new TetrisEngine());

        panel.setScale(ScreenSize.LARGE.scale());

        assertEquals(ScreenSize.LARGE.scale(), panel.getScale());
        assertTrue(panel.getPreferredSize().width > BASE_SIZE.width);
    }

    @Test
    void 배율은_0보다_커야_한다() {
        GamePanel panel = new GamePanel(new TetrisEngine());

        assertThrows(IllegalArgumentException.class, () -> panel.setScale(0));
        assertThrows(IllegalArgumentException.class, () -> panel.setScale(-1));
        assertThrows(IllegalArgumentException.class, () -> panel.setScale(Double.NaN));
    }

    @Test
    void 모든_크기에서_화면을_그릴_수_있다() {
        for (ScreenSize size : ScreenSize.values()) {
            GamePanel panel = new GamePanel(new TetrisEngine(), size.scale());
            Dimension preferred = panel.getPreferredSize();
            panel.setSize(preferred);
            BufferedImage image = new BufferedImage(preferred.width, preferred.height, BufferedImage.TYPE_INT_RGB);

            panel.paint(image.createGraphics());
        }
    }

    @Test
    void 화면_크기_배율은_모두_양수이고_보통은_1이다() {
        for (ScreenSize size : ScreenSize.values()) {
            assertTrue(size.scale() > 0);
        }
        assertEquals(1.0, ScreenSize.MEDIUM.scale());
    }

    // ------------------------------------------------------------------
    // 줄 삭제 애니메이션
    // ------------------------------------------------------------------

    private static final int CELL = 28;
    private static final int MARGIN = 24;

    /**
     * 보이는 보드의 (행, 열) 칸의 왼쪽 위 모서리 근처 픽셀의 색.
     *
     * <p>칸 가운데는 문자가 그려지는 자리이고, 문자색은 바탕에 따라 흰색일 수 있으므로 피한다.
     */
    private static int pixelAt(BufferedImage image, int row, int col) {
        return image.getRGB(MARGIN + col * CELL + 2, MARGIN + row * CELL + 2) & 0xFFFFFF;
    }

    private static BufferedImage render(GameState state) {
        GamePanel panel = new GamePanel(state);
        Dimension size = panel.getPreferredSize();
        panel.setSize(size);
        BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
        panel.paint(image.createGraphics());
        return image;
    }

    /** 맨 아랫줄이 T 블럭으로 가득 찬 보드 */
    private static int[][] bottomRowFilled() {
        int[][] grid = new int[Board.HEIGHT][Board.WIDTH];
        java.util.Arrays.fill(grid[Board.HEIGHT - 1], BlockType.T.code());
        return grid;
    }

    @Test
    void 지워질_칸만_하얗게_그리고_나머지_칸은_원래_색이다() {
        int[][] grid = bottomRowFilled();
        grid[Board.HEIGHT - 2][0] = BlockType.T.code();
        boolean[][] clearing = new boolean[Board.VISIBLE_HEIGHT][Board.WIDTH];
        java.util.Arrays.fill(clearing[Board.VISIBLE_HEIGHT - 1], true);

        BufferedImage image = render(new StubState(grid, clearing, true, null));

        int white = GamePanel.FLASH_COLOR.getRGB() & 0xFFFFFF;
        assertEquals(white, pixelAt(image, Board.VISIBLE_HEIGHT - 1, 0));
        assertEquals(white, pixelAt(image, Board.VISIBLE_HEIGHT - 1, Board.WIDTH - 1));
        int original = BlockPalette.colorOf(BlockType.T).getRGB() & 0xFFFFFF;
        assertNotEquals(white, pixelAt(image, Board.VISIBLE_HEIGHT - 2, 0));
        assertEquals(original, image.getRGB(MARGIN + 2, MARGIN + (Board.VISIBLE_HEIGHT - 2) * CELL + 2) & 0xFFFFFF);
    }

    @Test
    void 애니메이션이_아니면_가득_찬_줄도_하얗게_그리지_않는다() {
        boolean[][] none = new boolean[Board.VISIBLE_HEIGHT][Board.WIDTH];

        BufferedImage image = render(new StubState(bottomRowFilled(), none, false, null));

        assertNotEquals(GamePanel.FLASH_COLOR.getRGB() & 0xFFFFFF, pixelAt(image, Board.VISIBLE_HEIGHT - 1, 0));
    }

    @Test
    void 하얗게_그린_칸에는_문자를_그리지_않는다() {
        boolean[][] clearing = new boolean[Board.VISIBLE_HEIGHT][Board.WIDTH];
        java.util.Arrays.fill(clearing[Board.VISIBLE_HEIGHT - 1], true);

        BufferedImage image = render(new StubState(bottomRowFilled(), clearing, true, null));

        int white = GamePanel.FLASH_COLOR.getRGB() & 0xFFFFFF;
        int y0 = MARGIN + (Board.VISIBLE_HEIGHT - 1) * CELL + 1;
        for (int dy = 0; dy < CELL - 2; dy++) {
            for (int dx = 0; dx < CELL - 2; dx++) {
                assertEquals(white, image.getRGB(MARGIN + 1 + dx, y0 + dy) & 0xFFFFFF);
            }
        }
    }

    @Test
    void 애니메이션_중에는_현재_블럭을_그리지_않는다() {
        Block block = Block.of(BlockType.O, 3, 3);
        int row = -1;
        int col = -1;
        for (int r = 0; r < BlockType.SIZE && row < 0; r++) {
            for (int c = 0; c < BlockType.SIZE; c++) {
                if (block.isFilled(r, c)) {
                    row = block.boardRow(r);
                    col = block.boardCol(c);
                    break;
                }
            }
        }
        boolean[][] none = new boolean[Board.VISIBLE_HEIGHT][Board.WIDTH];
        int[][] empty = new int[Board.HEIGHT][Board.WIDTH];
        int blockColor = BlockPalette.colorOf(BlockType.O).getRGB() & 0xFFFFFF;

        BufferedImage playing = render(new StubState(empty, none, false, block));
        BufferedImage clearing = render(new StubState(empty, none, true, block));

        assertEquals(blockColor, playing.getRGB(MARGIN + col * CELL + 2, MARGIN + row * CELL + 2) & 0xFFFFFF);
        assertNotEquals(blockColor, clearing.getRGB(MARGIN + col * CELL + 2, MARGIN + row * CELL + 2) & 0xFFFFFF);
    }

    @Test
    void 지워질_칸_판단은_범위_밖을_안전하게_처리한다() {
        boolean[][] clearing = new boolean[2][3];
        clearing[1][2] = true;

        assertTrue(GamePanel.isFlashing(clearing, 1, 2));
        assertFalse(GamePanel.isFlashing(clearing, 0, 2));
        assertFalse(GamePanel.isFlashing(clearing, -1, 0));
        assertFalse(GamePanel.isFlashing(clearing, 2, 0));
        assertFalse(GamePanel.isFlashing(clearing, 0, 3));
        assertFalse(GamePanel.isFlashing(clearing, 0, -1));
        assertEquals(Color.WHITE, GamePanel.FLASH_COLOR);
    }

    @Test
    void 애니메이션_길이는_양수이다() {
        assertTrue(ClearAnimationTimer.DURATION_MS > 0);
    }

    /** 화면 그리기 확인용 게임 상태 대역. */
    private static final class StubState implements GameState {
        private final int[][] board;
        private final boolean[][] clearingCells;
        private final boolean clearing;
        private final Block current;

        StubState(int[][] board, boolean[][] clearingCells, boolean clearing, Block current) {
            this.board = board;
            this.clearingCells = clearingCells;
            this.clearing = clearing;
            this.current = current;
        }

        @Override public int[][] getBoard() { return board; }
        @Override public Block getCurrentBlock() { return current; }
        @Override public int getCurrentX() { return 0; }
        @Override public int getCurrentY() { return 0; }
        @Override public Block getNextBlock() { return null; }
        @Override public int getScore() { return 0; }
        @Override public int getLevel() { return 1; }
        @Override public int getLinesCleared() { return 0; }
        @Override public boolean isPaused() { return false; }
        @Override public boolean isGameOver() { return false; }
        @Override public int getGhostY() { return current == null ? 0 : current.row(); }
        @Override public boolean isClearing() { return clearing; }
        @Override public boolean[][] getClearingCells() { return clearingCells; }
    }
}
