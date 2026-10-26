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

/** 아이템이 보드, 현재 블럭, 다음 블럭 영역에 올바른 색과 모양으로 그려지는지 확인한다. */
class GamePanelItemTest {

    private static final int CELL = 28;
    private static final int MARGIN = 24;
    private static final int SIDE_X = MARGIN + Board.WIDTH * CELL + MARGIN;
    private static final int MINI = 22;
    private static final int NEXT_Y = MARGIN + 30;

    private static BufferedImage render(GameState state, double scale) {
        GamePanel panel = new GamePanel(state, scale);
        Dimension size = panel.getPreferredSize();
        panel.setSize(size);
        BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
        panel.paint(image.createGraphics());
        return image;
    }

    private static int rgb(BufferedImage image, int x, int y) {
        return image.getRGB(x, y) & 0xFFFFFF;
    }

    private static int colorOf(ItemType item) {
        return BlockPalette.colorOf(item.code()).getRGB() & 0xFFFFFF;
    }

    /** 보드 칸(행, 열)의 왼쪽 위 모서리 근처 픽셀. 문자가 그려지지 않는 자리다. */
    private static int boardPixel(BufferedImage image, int row, int col) {
        return rgb(image, MARGIN + col * CELL + 2, MARGIN + row * CELL + 2);
    }

    private static int[][] emptyBoard() {
        return new int[Board.HEIGHT][Board.WIDTH];
    }

    @Test
    void 보드에_고정된_아이템_칸은_아이템_색으로_그린다() {
        int[][] grid = emptyBoard();
        int bottom = Board.HEIGHT - 1;
        int col = 0;
        for (ItemType item : ItemType.values()) {
            grid[bottom][col++] = item.code();
        }
        grid[bottom - 1][0] = BlockType.T.code();

        BufferedImage image = render(new StubState(grid, null, null), 1.0);

        col = 0;
        for (ItemType item : ItemType.values()) {
            assertEquals(colorOf(item), boardPixel(image, Board.VISIBLE_HEIGHT - 1, col++), item.name());
        }
        assertEquals(BlockPalette.colorOf(BlockType.T).getRGB() & 0xFFFFFF,
                boardPixel(image, Board.VISIBLE_HEIGHT - 2, 0));
    }

    @Test
    void 독립형_아이템은_정해진_모양으로_그린다() {
        // 무게추: 위 2칸(1~2열), 아래 4칸
        Block weight = Block.of(ItemType.WEIGHT, 2, 3);
        BufferedImage image = render(new StubState(emptyBoard(), weight, null), 1.0);
        int color = colorOf(ItemType.WEIGHT);
        int background = rgb(image, MARGIN + 2, MARGIN + 10 * CELL + 2);

        assertEquals(color, boardPixel(image, 2, 4));
        assertEquals(color, boardPixel(image, 2, 5));
        assertNotEquals(color, boardPixel(image, 2, 3));
        assertNotEquals(color, boardPixel(image, 2, 6));
        for (int c = 3; c <= 6; c++) {
            assertEquals(color, boardPixel(image, 3, c));
        }
        assertEquals(background, boardPixel(image, 4, 4));
    }

    @Test
    void 크러셔는_3x3으로_그리고_톨보이는_1칸으로_그린다() {
        BufferedImage crusher = render(new StubState(emptyBoard(), Block.of(ItemType.CRUSHER, 2, 3), null), 1.0);
        int color = colorOf(ItemType.CRUSHER);
        int filled = 0;
        for (int r = 0; r < 6; r++) {
            for (int c = 0; c < Board.WIDTH; c++) {
                if (boardPixel(crusher, r, c) == color) {
                    filled++;
                }
            }
        }
        assertEquals(9, filled);

        BufferedImage tall = render(new StubState(emptyBoard(), Block.of(ItemType.TALL_BOY, 2, 3), null), 1.0);
        int tallColor = colorOf(ItemType.TALL_BOY);
        int single = 0;
        for (int r = 0; r < 6; r++) {
            for (int c = 0; c < Board.WIDTH; c++) {
                if (boardPixel(tall, r, c) == tallColor) {
                    single++;
                }
            }
        }
        assertEquals(1, single);
    }

    @Test
    void 포함형_아이템은_붙은_칸만_아이템_색이고_나머지는_블럭_색이다() {
        Block block = Block.of(BlockType.I, 0, 3).withItem(ItemType.LINE_CLEAR, 1, 2);

        BufferedImage image = render(new StubState(emptyBoard(), block, null), 1.0);

        int host = BlockPalette.colorOf(BlockType.I).getRGB() & 0xFFFFFF;
        int item = colorOf(ItemType.LINE_CLEAR);
        int itemCount = 0;
        int hostCount = 0;
        for (int r = 0; r < BlockType.SIZE; r++) {
            for (int c = 0; c < BlockType.SIZE; c++) {
                if (!block.isFilled(r, c)) {
                    continue;
                }
                int color = boardPixel(image, block.boardRow(r), block.boardCol(c));
                if (color == item) {
                    itemCount++;
                } else if (color == host) {
                    hostCount++;
                }
            }
        }
        assertEquals(1, itemCount);
        assertEquals(3, hostCount);
    }

    @Test
    void 다음_블럭_영역에도_아이템을_그린다() {
        Block next = Block.of(ItemType.WEIGHT, 0, 0);

        BufferedImage image = render(new StubState(emptyBoard(), null, next), 1.0);

        // 무게추 위 2칸 중 첫 칸은 1열, 아래 4칸의 첫 칸은 0열
        assertEquals(colorOf(ItemType.WEIGHT), rgb(image, SIDE_X + MINI + 2, NEXT_Y + 2));
        assertEquals(colorOf(ItemType.WEIGHT), rgb(image, SIDE_X + 2, NEXT_Y + MINI + 2));
        assertNotEquals(colorOf(ItemType.WEIGHT), rgb(image, SIDE_X + 2, NEXT_Y + 2));
    }

    @Test
    void 포함형_아이템이_붙은_다음_블럭도_그린다() {
        Block next = Block.of(BlockType.O, 0, 0).withItem(ItemType.DOUBLE_OR_NOTHING, 0, 1);

        BufferedImage image = render(new StubState(emptyBoard(), null, next), 1.0);

        assertEquals(colorOf(ItemType.DOUBLE_OR_NOTHING), rgb(image, SIDE_X + MINI + 2, NEXT_Y + 2));
        // O는 1~2열을 채우므로 붙은 칸(0행 1열)의 옆 칸(0행 2열)은 블럭 색이다.
        assertEquals(BlockPalette.colorOf(BlockType.O).getRGB() & 0xFFFFFF,
                rgb(image, SIDE_X + 2 * MINI + 2, NEXT_Y + 2));
    }

    @Test
    void 모든_화면_크기에서_아이템이_있어도_오류_없이_그려진다() {
        int[][] grid = emptyBoard();
        int col = 0;
        for (ItemType item : ItemType.values()) {
            grid[Board.HEIGHT - 1][col++] = item.code();
        }
        Block current = Block.of(ItemType.CRUSHER, 3, 3);
        Block next = Block.of(BlockType.T, 0, 0).withItem(ItemType.LINE_CLEAR, 1, 1);

        for (double scale : new double[] {0.8, 1.0, 1.3}) {
            BufferedImage image = render(new StubState(grid, current, next), scale);

            assertTrue(image.getWidth() > 0);
        }
    }

    /** 화면 그리기 확인용 게임 상태 대역. */
    private static final class StubState implements GameState {
        private final int[][] board;
        private final Block current;
        private final Block next;

        StubState(int[][] board, Block current, Block next) {
            this.board = board;
            this.current = current;
            this.next = next;
        }

        @Override public int[][] getBoard() { return board; }
        @Override public Block getCurrentBlock() { return current; }
        @Override public int getCurrentX() { return 0; }
        @Override public int getCurrentY() { return 0; }
        @Override public Block getNextBlock() { return next; }
        @Override public int getScore() { return 0; }
        @Override public int getLevel() { return 1; }
        @Override public int getLinesCleared() { return 0; }
        @Override public boolean isPaused() { return false; }
        @Override public boolean isGameOver() { return false; }
        @Override public int getGhostY() { return 0; }
    }
}
