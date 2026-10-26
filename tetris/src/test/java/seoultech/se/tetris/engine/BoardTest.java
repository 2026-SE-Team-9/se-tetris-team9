package seoultech.se.tetris.engine;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import seoultech.se.tetris.blocks.Block;
import seoultech.se.tetris.blocks.BlockType;
import seoultech.se.tetris.blocks.ItemType;

@DisplayName("Board")
class BoardTest {

    // ------------------------------------------------------------------
    // 테스트 보조
    // ------------------------------------------------------------------

    /**
     * 보드 아래쪽부터 채워진 상태를 만든다.
     *
     * <p>'.'은 빈 칸, 그 외 문자는 채워진 칸이다. 지정하지 않은 위쪽 줄은 모두 비어 있다.
     * 각 줄은 {@link Board#WIDTH}칸이어야 한다.
     */
    private static Board boardWithBottomRows(String... bottomRows) {
        int[][] grid = new int[Board.HEIGHT][Board.WIDTH];
        int offset = Board.HEIGHT - bottomRows.length;

        for (int r = 0; r < bottomRows.length; r++) {
            String row = bottomRows[r];
            if (row.length() != Board.WIDTH) {
                throw new IllegalArgumentException("각 줄은 " + Board.WIDTH + "칸이어야 합니다: " + row);
            }
            for (int c = 0; c < Board.WIDTH; c++) {
                grid[offset + r][c] = row.charAt(c) == '.' ? BlockType.EMPTY_CODE : BlockType.T.code();
            }
        }
        return new Board(grid);
    }

    /** 보드의 한 줄을 문자열로 읽는다. 비교 결과를 눈으로 확인하기 위한 것이다. */
    private static String rowOf(Board board, int row) {
        StringBuilder sb = new StringBuilder();
        for (int c = 0; c < Board.WIDTH; c++) {
            sb.append(board.isEmpty(row, c) ? '.' : 'X');
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // canPlace
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("canPlace()")
    class CanPlace {

        @Test
        @DisplayName("빈 보드의 가운데에는 놓을 수 있다")
        void emptyBoardAcceptsBlock() {
            Board board = new Board();
            Block block = Block.of(BlockType.T, 5, 3);

            assertTrue(board.canPlace(block));
        }

        @Test
        @DisplayName("왼쪽 벽을 넘으면 놓을 수 없다")
        void rejectsBeyondLeftWall() {
            Board board = new Board();
            // T 상태0은 0열부터 차 있으므로 col = -1이면 벽을 넘는다
            Block block = Block.of(BlockType.T, 5, -1);

            assertFalse(board.canPlace(block));
        }

        @Test
        @DisplayName("오른쪽 벽을 넘으면 놓을 수 없다")
        void rejectsBeyondRightWall() {
            Board board = new Board();
            // T 상태0은 2열까지 차 있으므로 col = WIDTH - 2면 오른쪽을 넘는다
            Block block = Block.of(BlockType.T, 5, Board.WIDTH - 2);

            assertFalse(board.canPlace(block));
        }

        @Test
        @DisplayName("바닥 아래로 넘으면 놓을 수 없다")
        void rejectsBelowFloor() {
            Board board = new Board();
            Block block = Block.of(BlockType.T, Board.HEIGHT - 1, 3);

            assertFalse(board.canPlace(block));
        }

        @Test
        @DisplayName("보드 위로 넘으면 놓을 수 없다")
        void rejectsAboveCeiling() {
            Board board = new Board();
            // T 상태0은 0행에 차 있으므로 row = -1이면 보드 위로 넘는다
            Block block = Block.of(BlockType.T, -1, 3);

            assertFalse(board.canPlace(block));
        }

        @Test
        @DisplayName("이미 블럭이 있는 칸과 겹치면 놓을 수 없다")
        void rejectsOverlap() {
            Board board = boardWithBottomRows("XXXXXXXXXX");
            Block block = Block.of(BlockType.T, Board.HEIGHT - 2, 3);

            assertFalse(board.canPlace(block));
        }

        @Test
        @DisplayName("모양의 빈 칸은 보드 밖에 있어도 상관없다")
        void ignoresEmptyCellsOutsideBoard() {
            Board board = new Board();
            // I 상태1은 2열에만 차 있고 나머지 3열은 비어 있다.
            // col = -2면 격자의 0, 1열이 보드 밖으로 나가지만 그 칸들은 비어 있다.
            Block block = new Block(BlockType.I, 1, 5, -2);

            assertTrue(board.canPlace(block));
        }
    }

    // ------------------------------------------------------------------
    // place
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("place()")
    class Place {

        @Test
        @DisplayName("블럭이 차지하는 칸에 종류 번호가 기록된다")
        void writesBlockCode() {
            Board board = new Board();
            Block block = Block.of(BlockType.T, 5, 3);

            board.place(block);

            // T 상태0:  .T..  /  TTT.
            assertEquals(BlockType.T.code(), board.codeAt(5, 4));
            assertEquals(BlockType.T.code(), board.codeAt(6, 3));
            assertEquals(BlockType.T.code(), board.codeAt(6, 4));
            assertEquals(BlockType.T.code(), board.codeAt(6, 5));
        }

        @Test
        @DisplayName("아이템이 붙은 칸에는 아이템 번호가 기록된다")
        void writesItemCode() {
            Board board = new Board();
            // T 상태0의 윗칸(격자 0행 1열)에 D를 붙인다
            Block block = Block.of(BlockType.T, 5, 3).withItem(ItemType.DOUBLE_OR_NOTHING, 0, 1);

            board.place(block);

            assertEquals(ItemType.DOUBLE_OR_NOTHING.code(), board.codeAt(5, 4));
            assertEquals(BlockType.T.code(), board.codeAt(6, 4));
        }

        @Test
        @DisplayName("독립형 아이템은 모든 칸에 아이템 번호가 기록된다")
        void writesStandaloneItemCode() {
            Board board = new Board();

            board.place(Block.of(ItemType.CRUSHER, 0, 0));

            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    assertEquals(ItemType.CRUSHER.code(), board.codeAt(r, c));
                }
            }
            assertTrue(board.isEmpty(0, 3));
        }

        @Test
        @DisplayName("블럭이 차지하지 않는 칸은 그대로 비어 있다")
        void leavesOtherCellsEmpty() {
            Board board = new Board();
            board.place(Block.of(BlockType.T, 5, 3));

            assertTrue(board.isEmpty(5, 3));
            assertTrue(board.isEmpty(5, 5));
            assertTrue(board.isEmpty(7, 4));
        }

        @Test
        @DisplayName("놓을 수 없는 위치면 예외를 던진다")
        void throwsWhenCannotPlace() {
            Board board = new Board();
            Block block = Block.of(BlockType.T, 5, -1);

            assertThrows(IllegalArgumentException.class, () -> board.place(block));
        }
    }

    // ------------------------------------------------------------------
    // clearFullLines
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("clearFullLines()")
    class ClearFullLines {

        @Test
        @DisplayName("가득 찬 줄이 없으면 0을 반환한다")
        void returnsZeroWhenNoFullLine() {
            Board board = boardWithBottomRows("XXXXXXXXX.");

            assertEquals(0, board.clearFullLines());
        }

        @Test
        @DisplayName("가득 찬 한 줄을 삭제한다")
        void clearsSingleLine() {
            Board board = boardWithBottomRows("XXXXXXXXXX");

            assertEquals(1, board.clearFullLines());
            assertEquals("..........", rowOf(board, Board.HEIGHT - 1));
        }

        @Test
        @DisplayName("삭제한 줄 위의 블럭이 아래로 내려온다")
        void shiftsBlocksAboveDown() {
            Board board = boardWithBottomRows(
                    "X.........",
                    "XXXXXXXXXX"
            );

            assertEquals(1, board.clearFullLines());
            assertEquals("X.........", rowOf(board, Board.HEIGHT - 1));
            assertEquals("..........", rowOf(board, Board.HEIGHT - 2));
        }

        @Test
        @DisplayName("연속된 두 줄을 한 번에 삭제한다")
        void clearsTwoAdjacentLines() {
            Board board = boardWithBottomRows(
                    "X.........",
                    "XXXXXXXXXX",
                    "XXXXXXXXXX"
            );

            assertEquals(2, board.clearFullLines());
            assertEquals("X.........", rowOf(board, Board.HEIGHT - 1));
            assertEquals("..........", rowOf(board, Board.HEIGHT - 2));
        }

        @Test
        @DisplayName("떨어져 있는 두 줄도 모두 삭제한다")
        void clearsTwoSeparatedLines() {
            Board board = boardWithBottomRows(
                    "XXXXXXXXXX",
                    "X.........",
                    "XXXXXXXXXX"
            );

            assertEquals(2, board.clearFullLines());
            assertEquals("X.........", rowOf(board, Board.HEIGHT - 1));
            assertEquals("..........", rowOf(board, Board.HEIGHT - 2));
        }

        @Test
        @DisplayName("네 줄을 동시에 삭제한다")
        void clearsFourLines() {
            Board board = boardWithBottomRows(
                    "XXXXXXXXXX",
                    "XXXXXXXXXX",
                    "XXXXXXXXXX",
                    "XXXXXXXXXX"
            );

            assertEquals(4, board.clearFullLines());
            assertEquals("..........", rowOf(board, Board.HEIGHT - 1));
        }
    }

    // ------------------------------------------------------------------
    // clearCell
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("clearCell()")
    class ClearCell {

        @Test
        @DisplayName("지정한 칸만 비우고 위의 칸은 내려오지 않는다")
        void clearsOnlyThatCell() {
            Board board = boardWithBottomRows(
                    "X.........",
                    "XX........"
            );

            board.clearCell(Board.HEIGHT - 1, 0);

            assertEquals(".X........", rowOf(board, Board.HEIGHT - 1));
            assertEquals("X.........", rowOf(board, Board.HEIGHT - 2));
        }

        @Test
        @DisplayName("보드 범위를 벗어난 칸이면 예외를 던진다")
        void rejectsCellOutOfRange() {
            Board board = new Board();

            assertThrows(IllegalArgumentException.class, () -> board.clearCell(Board.HEIGHT, 0));
            assertThrows(IllegalArgumentException.class, () -> board.clearCell(0, -1));
        }
    }

    @Nested
    @DisplayName("dropAbove()")
    class DropAbove {

        /** 0열의 14~19행을 위에서부터 채운 보드. 'X'는 채운 칸, '.'은 빈 칸이다. */
        private Board boardWithColumn(String topToBottom) {
            String[] rows = new String[topToBottom.length()];
            for (int i = 0; i < rows.length; i++) {
                rows[i] = topToBottom.charAt(i) + ".........";
            }
            return boardWithBottomRows(rows);
        }

        /** 지정한 열을 위에서 아래로 읽은 문자열 (14~19행). */
        private String column(Board board, int col) {
            StringBuilder sb = new StringBuilder();
            for (int r = Board.HEIGHT - 6; r < Board.HEIGHT; r++) {
                sb.append(board.isEmpty(r, col) ? '.' : 'X');
            }
            return sb.toString();
        }

        @Test
        @DisplayName("위쪽 칸들을 아래가 막힐 때까지 한 덩어리로 내린다")
        void dropsUntilSupported() {
            Board board = boardWithColumn("XX...X");

            board.dropAbove(0, 16);

            assertEquals("...XXX", column(board, 0));
        }

        @Test
        @DisplayName("덩어리 안의 빈 칸은 그대로 유지한다")
        void keepsHolesInside() {
            Board board = boardWithColumn("X.X...");

            board.dropAbove(0, 17);

            assertEquals("...X.X", column(board, 0));
        }

        @Test
        @DisplayName("바로 아래가 막혀 있거나 위에 칸이 없으면 바뀌지 않는다")
        void noChangeWhenSupportedOrEmpty() {
            Board supported = boardWithColumn("XX....");
            Board empty = boardWithColumn(".....X");

            supported.dropAbove(0, 15);
            empty.dropAbove(0, 16);

            assertEquals("XX....", column(supported, 0));
            assertEquals(".....X", column(empty, 0));
        }

        @Test
        @DisplayName("범위를 벗어나면 예외를 던진다")
        void rejectsOutOfRange() {
            Board board = new Board();

            assertThrows(IllegalArgumentException.class, () -> board.dropAbove(Board.WIDTH, 0));
            assertThrows(IllegalArgumentException.class, () -> board.dropAbove(0, Board.HEIGHT + 1));
        }
    }

    // ------------------------------------------------------------------
    // fullRows / removeRows
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("fullRows()")
    class FullRows {

        @Test
        @DisplayName("가득 찬 줄이 없으면 빈 목록을 반환한다")
        void returnsEmptyWhenNoFullLine() {
            Board board = boardWithBottomRows("XXXXXXXXX.");

            assertEquals(List.of(), board.fullRows());
        }

        @Test
        @DisplayName("가득 찬 줄을 아래쪽부터 반환한다")
        void returnsFullRowsFromBottom() {
            Board board = boardWithBottomRows(
                    "XXXXXXXXXX",
                    "X.........",
                    "XXXXXXXXXX"
            );

            assertEquals(List.of(Board.HEIGHT - 1, Board.HEIGHT - 3), board.fullRows());
        }

        @Test
        @DisplayName("찾기만 하고 보드는 바꾸지 않는다")
        void doesNotModifyBoard() {
            Board board = boardWithBottomRows("XXXXXXXXXX");

            board.fullRows();

            assertEquals("XXXXXXXXXX", rowOf(board, Board.HEIGHT - 1));
        }
    }

    @Nested
    @DisplayName("removeRows()")
    class RemoveRows {

        @Test
        @DisplayName("가득 차지 않은 줄도 지정하면 삭제한다")
        void removesRowEvenIfNotFull() {
            Board board = boardWithBottomRows(
                    "X.........",
                    ".X........"
            );

            board.removeRows(List.of(Board.HEIGHT - 1));

            assertEquals("X.........", rowOf(board, Board.HEIGHT - 1));
            assertEquals("..........", rowOf(board, Board.HEIGHT - 2));
        }

        @Test
        @DisplayName("떨어져 있는 여러 줄을 지우면 남은 줄이 순서를 유지한 채 내려온다")
        void removesSeparatedRowsKeepingOrder() {
            Board board = boardWithBottomRows(
                    "X.........",   // 16행: 남음
                    ".X........",   // 17행: 삭제
                    "..X.......",   // 18행: 남음
                    "...X......"    // 19행: 삭제
            );

            board.removeRows(List.of(Board.HEIGHT - 3, Board.HEIGHT - 1));

            assertEquals("..X.......", rowOf(board, Board.HEIGHT - 1));
            assertEquals("X.........", rowOf(board, Board.HEIGHT - 2));
            assertEquals("..........", rowOf(board, Board.HEIGHT - 3));
        }

        @Test
        @DisplayName("같은 줄이 여러 번 들어 있어도 한 번만 삭제한다")
        void removesDuplicatedRowOnce() {
            Board board = boardWithBottomRows(
                    "X.........",
                    ".X........"
            );

            board.removeRows(List.of(Board.HEIGHT - 1, Board.HEIGHT - 1));

            assertEquals("X.........", rowOf(board, Board.HEIGHT - 1));
        }

        @Test
        @DisplayName("빈 목록이면 보드를 바꾸지 않는다")
        void doesNothingForEmptyList() {
            Board board = boardWithBottomRows(".X........");

            board.removeRows(List.of());

            assertEquals(".X........", rowOf(board, Board.HEIGHT - 1));
        }

        @Test
        @DisplayName("보드 범위를 벗어난 행이면 예외가 발생한다")
        void rejectsRowOutOfRange() {
            Board board = new Board();

            assertThrows(IllegalArgumentException.class, () -> board.removeRows(List.of(Board.HEIGHT)));
            assertThrows(IllegalArgumentException.class, () -> board.removeRows(List.of(-1)));
        }
    }

    // ------------------------------------------------------------------
    // 그 외
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("snapshot()")
    class Snapshot {

        @Test
        @DisplayName("화면에 보이는 줄 수만큼 반환한다")
        void returnsVisibleHeight() {
            int[][] snapshot = new Board().snapshot();

            assertEquals(Board.VISIBLE_HEIGHT, snapshot.length);
            assertEquals(Board.WIDTH, snapshot[0].length);
        }

        @Test
        @DisplayName("반환값을 수정해도 보드는 바뀌지 않는다")
        void returnsDefensiveCopy() {
            Board board = new Board();
            int[][] snapshot = board.snapshot();

            snapshot[0][0] = BlockType.I.code();

            assertTrue(board.isEmpty(0, 0));
        }

        @Test
        @DisplayName("고정된 블럭이 그대로 담긴다")
        void containsPlacedBlocks() {
            Board board = new Board();
            board.place(Block.of(BlockType.T, 5, 3));

            int[][] snapshot = board.snapshot();

            assertEquals(BlockType.T.code(), snapshot[6][4]);
        }
    }

    @Nested
    @DisplayName("isEmpty()")
    class IsEmpty {

        @Test
        @DisplayName("빈 칸이면 true")
        void trueForEmptyCell() {
            assertTrue(new Board().isEmpty(0, 0));
        }

        @Test
        @DisplayName("보드 밖은 비어 있지 않은 것으로 본다")
        void falseOutsideBoard() {
            Board board = new Board();

            assertFalse(board.isEmpty(-1, 0));
            assertFalse(board.isEmpty(Board.HEIGHT, 0));
            assertFalse(board.isEmpty(0, -1));
            assertFalse(board.isEmpty(0, Board.WIDTH));
        }
    }

    @Nested
    @DisplayName("reset()")
    class Reset {

        @Test
        @DisplayName("모든 칸을 비운다")
        void clearsEverything() {
            Board board = boardWithBottomRows(
                    "XXXXXXXXXX",
                    "XXXXXXXXXX"
            );

            board.reset();

            assertArrayEquals(new int[Board.VISIBLE_HEIGHT][Board.WIDTH], board.snapshot());
        }
    }
}
