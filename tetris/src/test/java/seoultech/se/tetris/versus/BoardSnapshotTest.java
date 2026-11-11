package seoultech.se.tetris.versus;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seoultech.se.tetris.SerializationTestSupport.roundTrip;
import static seoultech.se.tetris.component.model.GarbageLineFixtures.line;

import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import seoultech.se.tetris.blocks.Block;
import seoultech.se.tetris.blocks.BlockFactory;
import seoultech.se.tetris.blocks.BlockType;
import seoultech.se.tetris.blocks.CellCode;
import seoultech.se.tetris.blocks.ItemType;
import seoultech.se.tetris.component.model.GameState;
import seoultech.se.tetris.component.model.GarbageLine;
import seoultech.se.tetris.engine.Board;
import seoultech.se.tetris.engine.TetrisEngine;

@DisplayName("BoardSnapshot")
class BoardSnapshotTest {

    private static final List<GarbageLine> PENDING = List.of(line("XXX.XXXXXX"), line("XXX..XXXXX"));

    // ------------------------------------------------------------------
    // 상태 담기
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("상태 담기")
    class Capture {

        @Test
        @DisplayName("GameState가 알려주는 값을 그대로 담는다")
        void copiesEveryValue() {
            FakeState state = new FakeState();
            state.board[19][0] = CellCode.GARBAGE;
            state.current = Block.of(BlockType.T, 3, 4);
            state.next = Block.of(BlockType.I, 0, 0).withItem(ItemType.LINE_CLEAR, 1, 2);
            state.ghostY = 17;
            state.score = 1234;
            state.level = 3;
            state.lines = 12;
            state.gameOver = true;
            state.clearing = true;
            state.clearingCells[18][5] = true;
            state.pending = PENDING;

            BoardSnapshot snapshot = BoardSnapshot.of(state);

            assertArrayEquals(state.board, snapshot.board());
            assertEquals(state.current, snapshot.current());
            assertEquals(state.next, snapshot.next());
            assertEquals(17, snapshot.ghostY());
            assertEquals(1234, snapshot.score());
            assertEquals(3, snapshot.level());
            assertEquals(12, snapshot.linesCleared());
            assertTrue(snapshot.gameOver());
            assertTrue(snapshot.clearing());
            assertTrue(snapshot.clearingCells()[18][5]);
            assertEquals(PENDING, snapshot.pendingGarbage());
        }

        @Test
        @DisplayName("진행 중인 엔진의 상태를 그대로 담는다")
        void capturesEngine() {
            TetrisEngine engine = new TetrisEngine(new BlockFactory(new Random(7)));
            engine.moveLeft();
            engine.moveDown();

            BoardSnapshot snapshot = BoardSnapshot.of(engine);

            assertArrayEquals(engine.getBoard(), snapshot.board());
            assertEquals(engine.getCurrentBlock(), snapshot.current());
            assertEquals(engine.getNextBlock(), snapshot.next());
            assertEquals(engine.getGhostY(), snapshot.ghostY());
            assertEquals(engine.getScore(), snapshot.score());
            assertEquals(engine.getLevel(), snapshot.level());
            assertEquals(engine.getLinesCleared(), snapshot.linesCleared());
            assertFalse(snapshot.gameOver());
            assertFalse(snapshot.clearing());
            assertTrue(snapshot.pendingGarbage().isEmpty());
        }

        @Test
        @DisplayName("현재 블럭이나 다음 블럭이 없어도 담을 수 있다")
        void allowsMissingBlocks() {
            BoardSnapshot snapshot = BoardSnapshot.of(new FakeState());

            assertNull(snapshot.current());
            assertNull(snapshot.next());
        }
    }

    // ------------------------------------------------------------------
    // 크기 검사 (P2P로 받은 값도 같은 검사를 거친다)
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("크기 검사")
    class Validation {

        @Test
        @DisplayName("보드의 줄 수나 칸 수가 화면 보드와 다르면 예외를 던진다")
        void rejectsWrongBoardSize() {
            assertThrows(IllegalArgumentException.class,
                    () -> snapshot(new int[Board.VISIBLE_HEIGHT - 1][Board.WIDTH], emptyCells(), List.of()));
            assertThrows(IllegalArgumentException.class,
                    () -> snapshot(new int[Board.VISIBLE_HEIGHT][Board.WIDTH + 1], emptyCells(), List.of()));
        }

        @Test
        @DisplayName("지워질 칸의 크기가 화면 보드와 다르면 예외를 던진다")
        void rejectsWrongClearingCellsSize() {
            assertThrows(IllegalArgumentException.class,
                    () -> snapshot(emptyBoard(), new boolean[Board.VISIBLE_HEIGHT][Board.WIDTH - 1], List.of()));
        }

        @Test
        @DisplayName("공격 대기 줄의 폭이 보드 폭과 다르면 예외를 던진다")
        void rejectsWrongGarbageWidth() {
            assertThrows(IllegalArgumentException.class,
                    () -> snapshot(emptyBoard(), emptyCells(), List.of(line("XXX.XXXXX"))));
        }

        @Test
        @DisplayName("보드, 지워질 칸, 공격 대기 줄이 없으면 예외를 던진다")
        void rejectsNull() {
            assertThrows(NullPointerException.class, () -> snapshot(null, emptyCells(), List.of()));
            assertThrows(NullPointerException.class, () -> snapshot(emptyBoard(), null, List.of()));
            assertThrows(NullPointerException.class, () -> snapshot(emptyBoard(), emptyCells(), null));
        }
    }

    // ------------------------------------------------------------------
    // 복사 보호
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("복사 보호")
    class Copying {

        @Test
        @DisplayName("만들 때 넘긴 배열을 나중에 고쳐도 스냅샷은 바뀌지 않는다")
        void copiesGivenArrays() {
            int[][] board = emptyBoard();
            boolean[][] cells = emptyCells();
            BoardSnapshot snapshot = snapshot(board, cells, List.of());

            board[0][0] = CellCode.GARBAGE;
            cells[0][0] = true;

            assertEquals(CellCode.EMPTY, snapshot.board()[0][0]);
            assertFalse(snapshot.clearingCells()[0][0]);
        }

        @Test
        @DisplayName("돌려받은 배열을 고쳐도 스냅샷은 바뀌지 않는다")
        void returnsCopies() {
            BoardSnapshot snapshot = snapshot(emptyBoard(), emptyCells(), List.of());

            snapshot.board()[0][0] = CellCode.GARBAGE;
            snapshot.clearingCells()[0][0] = true;

            assertEquals(CellCode.EMPTY, snapshot.board()[0][0]);
            assertFalse(snapshot.clearingCells()[0][0]);
        }

        @Test
        @DisplayName("돌려받은 공격 대기 줄 목록은 고칠 수 없다")
        void pendingGarbageIsUnmodifiable() {
            BoardSnapshot snapshot = snapshot(emptyBoard(), emptyCells(), PENDING);

            assertThrows(UnsupportedOperationException.class, () -> snapshot.pendingGarbage().clear());
        }
    }

    // ------------------------------------------------------------------
    // 비교와 직렬화
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("비교와 직렬화")
    class EqualityAndSerialization {

        @Test
        @DisplayName("담긴 내용이 같으면 같은 스냅샷이고, 하나라도 다르면 다르다")
        void equalsByContent() {
            FakeState state = new FakeState();
            state.board[19][3] = BlockType.T.code();
            state.pending = PENDING;

            BoardSnapshot first = BoardSnapshot.of(state);
            BoardSnapshot second = BoardSnapshot.of(state);
            assertEquals(first, second);
            assertEquals(first.hashCode(), second.hashCode());

            state.score = 10;
            assertNotEquals(first, BoardSnapshot.of(state));
        }

        @Test
        @DisplayName("직렬화했다가 다시 읽어도 같은 스냅샷이다")
        void survivesSerialization() throws Exception {
            FakeState state = new FakeState();
            state.board[19][0] = CellCode.GARBAGE;
            state.current = Block.of(BlockType.I, 0, 0).withItem(ItemType.LINE_CLEAR, 1, 2);
            state.next = Block.of(ItemType.CRUSHER, 0, 3);
            state.clearingCells[19][0] = true;
            state.pending = PENDING;
            BoardSnapshot snapshot = BoardSnapshot.of(state);

            assertEquals(snapshot, roundTrip(snapshot));
        }
    }

    // ------------------------------------------------------------------
    // 도우미
    // ------------------------------------------------------------------

    private static BoardSnapshot snapshot(int[][] board, boolean[][] cells, List<GarbageLine> pending) {
        return new BoardSnapshot(board, null, null, 0, 0, 1, 0, false, false, cells, pending);
    }

    private static int[][] emptyBoard() {
        return new int[Board.VISIBLE_HEIGHT][Board.WIDTH];
    }

    private static boolean[][] emptyCells() {
        return new boolean[Board.VISIBLE_HEIGHT][Board.WIDTH];
    }

    /** 모든 값을 테스트에서 직접 정하는 GameState */
    private static final class FakeState implements GameState {
        int[][] board = emptyBoard();
        Block current;
        Block next;
        int ghostY;
        int score;
        int level = 1;
        int lines;
        boolean gameOver;
        boolean clearing;
        boolean[][] clearingCells = emptyCells();
        List<GarbageLine> pending = List.of();

        @Override public int[][] getBoard() { return board; }
        @Override public Block getCurrentBlock() { return current; }
        @Override public int getCurrentX() { return current == null ? 0 : current.col(); }
        @Override public int getCurrentY() { return current == null ? 0 : current.row(); }
        @Override public Block getNextBlock() { return next; }
        @Override public int getScore() { return score; }
        @Override public int getLevel() { return level; }
        @Override public int getLinesCleared() { return lines; }
        @Override public boolean isPaused() { return false; }
        @Override public boolean isGameOver() { return gameOver; }
        @Override public int getGhostY() { return ghostY; }
        @Override public boolean isClearing() { return clearing; }
        @Override public boolean[][] getClearingCells() { return clearingCells; }
        @Override public List<GarbageLine> getPendingGarbage() { return pending; }
    }
}
