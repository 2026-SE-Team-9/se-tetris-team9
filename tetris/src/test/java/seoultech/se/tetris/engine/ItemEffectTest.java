package seoultech.se.tetris.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import seoultech.se.tetris.blocks.Block;
import seoultech.se.tetris.blocks.BlockType;
import seoultech.se.tetris.blocks.ItemType;

/**
 * 아이템 동작을 보드와 게임 문맥 객체만으로 확인한다.
 *
 * <p>엔진을 거치지 않으므로 애니메이션, 점수 반영, 다음 블럭 생성은 여기서 다루지 않는다.
 * 이 테스트는 "무엇을 지우기로 계획했는가"만 본다.
 */
@DisplayName("ItemEffect")
class ItemEffectTest {

    /** 보드에 채울 번호. 어떤 블럭이든 0이 아니면 된다. */
    private static final int FILLED = BlockType.O.code();

    /** 아래쪽부터 채운 보드. 'X'는 채운 칸, 'D'는 Double or Nothing 칸, '.'은 빈 칸이다. */
    private static Board boardWithBottomRows(String... bottomRows) {
        int[][] grid = new int[Board.HEIGHT][Board.WIDTH];
        int offset = Board.HEIGHT - bottomRows.length;
        for (int i = 0; i < bottomRows.length; i++) {
            for (int c = 0; c < Board.WIDTH; c++) {
                char ch = bottomRows[i].charAt(c);
                grid[offset + i][c] = switch (ch) {
                    case '.' -> 0;
                    case 'D' -> ItemType.DOUBLE_OR_NOTHING.code();
                    default -> FILLED;
                };
            }
        }
        return new Board(grid);
    }

    /** 블럭을 바닥이나 다른 블럭에 닿을 때까지 내린다. */
    private static Block landed(Board board, Block block) {
        Block result = block;
        while (board.canPlace(result.moved(1, 0))) {
            result = result.moved(1, 0);
        }
        return result;
    }

    private static ClearPlan lock(Board board, Block block) {
        LockContext context = new LockContext(board, block);
        ItemEffects.of(block.item()).onLock(context);
        return context.plan();
    }

    private static List<String> cells(ClearPlan plan) {
        List<String> result = new ArrayList<>();
        for (int[] cell : plan.removedCells()) {
            result.add(cell[0] + "," + cell[1]);
        }
        return result;
    }

    private static List<String> dropColumns(ClearPlan plan) {
        List<String> result = new ArrayList<>();
        for (int[] column : plan.dropColumns()) {
            result.add(column[0] + "," + column[1]);
        }
        return result;
    }

    @Test
    @DisplayName("아이템마다 동작이 연결되어 있고, 아이템이 없으면 일반 블럭 동작이다")
    void everyItemHasEffect() {
        assertSame(ItemEffects.NONE, ItemEffects.of(null));
        for (ItemType item : ItemType.values()) {
            assertTrue(ItemEffects.of(item) != ItemEffects.NONE, item.toString());
        }
    }

    @Nested
    @DisplayName("일반 블럭")
    class Normal {

        @Test
        @DisplayName("놓은 뒤 가득 찬 줄만 줄 삭제로 계획한다")
        void plansFullLines() {
            Board board = boardWithBottomRows("XXX....XXX");
            Block block = landed(board, Block.of(BlockType.I, 0, 3));   // 가로 I가 19행 3~6열을 채운다

            ClearPlan plan = lock(board, block);

            assertEquals(List.of(19), plan.lineRows());
            assertEquals(1, plan.lineScoreMultiplier());
            assertEquals(BlockType.I.code(), board.codeAt(19, 3));
        }

        @Test
        @DisplayName("가득 찬 줄이 없으면 지울 것이 없다")
        void nothingToClear() {
            Board board = new Board();

            ClearPlan plan = lock(board, landed(board, Block.of(BlockType.T, 0, 3)));

            assertFalse(plan.hasRemoval());
        }
    }

    @Nested
    @DisplayName("줄 삭제 (L)")
    class LineClear {

        private Block withL(BlockType type, int localRow, int localCol) {
            return Block.of(type, 0, 3).withItem(ItemType.LINE_CLEAR, localRow, localCol);
        }

        @Test
        @DisplayName("꽉 차지 않아도 L이 있는 줄을 지운다")
        void clearsRowWithL() {
            Board board = new Board();
            // T 상태0: 윗줄 1칸, 아랫줄 3칸. 윗칸(0, 1)에 L
            Block block = landed(board, withL(BlockType.T, 0, 1));

            ClearPlan plan = lock(board, block);

            assertEquals(List.of(18), plan.lineRows());
        }

        @Test
        @DisplayName("L 줄이 꽉 찬 줄이기도 하면 한 번만 지운다")
        void doesNotCountTwice() {
            Board board = boardWithBottomRows("XXX....XXX");
            Block block = landed(board, withL(BlockType.I, 1, 0));

            ClearPlan plan = lock(board, block);

            assertEquals(List.of(19), plan.lineRows());
        }

        @Test
        @DisplayName("L 줄과 다른 꽉 찬 줄을 합쳐서 지운다")
        void combinesWithFullLines() {
            Board board = boardWithBottomRows(
                    "XXX...XXX.",
                    "XXXX.XXXXX"
            );
            // 뒤집힌 T(상태2)를 3열에 두면 윗줄이 18행 3~5열, 아래 칸이 19행 4열이다.
            // 19행은 꽉 차고, L을 붙인 18행은 9열이 비어 꽉 차지 않는다
            Block upsideDown = Block.of(BlockType.T, 0, 3).rotated().rotated();
            Block block = landed(board, upsideDown.withItem(ItemType.LINE_CLEAR, 1, 0));

            ClearPlan plan = lock(board, block);

            assertEquals(List.of(19, 18), plan.lineRows());
        }
    }

    @Nested
    @DisplayName("Double or Nothing (D)")
    class DoubleOrNothing {

        @Test
        @DisplayName("D가 있는 줄이 꽉 찬 줄로 지워지면 배수가 2다")
        void doublesWhenFullLineHasD() {
            Board board = boardWithBottomRows("XXD....XXX");

            ClearPlan plan = lock(board, landed(board, Block.of(BlockType.I, 0, 3)));

            assertEquals(2, plan.lineScoreMultiplier());
        }

        @Test
        @DisplayName("D가 여러 개 있어도 배수는 2다")
        void doublesOnce() {
            Board board = boardWithBottomRows(
                    "XXDXXXXXX.",
                    "XXDXXXXXX."
            );
            // 세로 I를 9열에 떨어뜨려 D가 있는 두 줄을 함께 채운다
            Block vertical = Block.of(BlockType.I, 0, 3).rotated().moved(0, 4);

            ClearPlan plan = lock(board, landed(board, vertical));

            assertEquals(List.of(19, 18), plan.lineRows());
            assertEquals(2, plan.lineScoreMultiplier());
        }

        @Test
        @DisplayName("꽉 차지 않은 줄이 L로 지워지면 D는 배수를 주지 않는다")
        void noBonusWhenRemovedByLineClearItem() {
            Board board = boardWithBottomRows("D.........");
            Block block = landed(board, Block.of(BlockType.I, 0, 3).withItem(ItemType.LINE_CLEAR, 1, 0));

            ClearPlan plan = lock(board, block);

            assertEquals(List.of(19), plan.lineRows());
            assertEquals(1, plan.lineScoreMultiplier());
        }

        @Test
        @DisplayName("고정은 일반 블럭과 같고 D 칸이 보드에 남는다")
        void staysOnBoard() {
            Board board = new Board();
            Block block = landed(board, Block.of(BlockType.I, 0, 3).withItem(ItemType.DOUBLE_OR_NOTHING, 1, 2));

            ClearPlan plan = lock(board, block);

            assertFalse(plan.hasRemoval());
            assertEquals(ItemType.DOUBLE_OR_NOTHING.code(), board.codeAt(19, 5));
        }
    }

    @Nested
    @DisplayName("무게추 (A)")
    class Weight {

        @Test
        @DisplayName("닿은 뒤에는 좌우로 움직일 수 없다")
        void cannotSlide() {
            assertFalse(ItemEffects.of(ItemType.WEIGHT).canSlideWhenLanded());
            assertTrue(ItemEffects.NONE.canSlideWhenLanded());
        }

        @Test
        @DisplayName("아래 칸을 바닥까지 지우고 그 수만큼 점수를 계획한 뒤 바닥에 놓는다")
        void removesCellsBelowAndLandsOnFloor() {
            Board board = boardWithBottomRows(
                    "...XX.....",
                    "...X.X....",
                    "XXXXXXXXX."
            );
            // 무게추 아랫줄(3~6열)이 17행 3·4열 블럭에 닿아 16행에 멈춘다
            Block weight = landed(board, Block.of(ItemType.WEIGHT, 0, 3));

            ClearPlan plan = lock(board, weight);

            assertEquals(List.of("17,3", "18,3", "19,3", "17,4", "19,4", "18,5", "19,5", "19,6"), cells(plan));
            assertEquals(8, plan.itemScoreCells());
            assertEquals(19, plan.placeAfterRemoval().boardRow(1));
            assertTrue(plan.lineRows().isEmpty());
        }

        @Test
        @DisplayName("빈 보드에서는 지울 칸 없이 바닥에 놓는다")
        void landsOnEmptyBoard() {
            Board board = new Board();
            Block weight = landed(board, Block.of(ItemType.WEIGHT, 0, 3));

            ClearPlan plan = lock(board, weight);

            assertFalse(plan.hasRemoval());
            assertEquals(0, plan.itemScoreCells());
            assertEquals(weight, plan.placeAfterRemoval());
        }
    }

    @Nested
    @DisplayName("Tall Boy (B)")
    class TallBoy {

        @Test
        @DisplayName("2칸 파고든 뒤 5×5 범위의 블럭을 지우고 자기 칸을 포함해 점수를 계획한다")
        void digsAndExplodes() {
            Board board = boardWithBottomRows(
                    "XXXXXXXXX.",
                    "XXXXXXXXX.",
                    "XXXXXXXXX.",
                    "XXXXXXXXX.",
                    "XXXXXXXXX."
            );
            // B는 격자 (0, 1)이므로 3열에 두면 보드 4열. 15행 위에 멈춘 뒤 17행으로 파고든다
            Block tallBoy = landed(board, Block.of(ItemType.TALL_BOY, 0, 3));
            assertEquals(14, tallBoy.boardRow(0));

            ClearPlan plan = lock(board, tallBoy);

            // 중심 (16, 4), 범위 14~18행 × 2~6열. 14행은 비어 있어 20칸이 지워진다
            assertEquals(20, plan.removedCells().size());
            assertEquals(20, plan.itemScoreCells());   // 중심 칸은 자기 칸으로 한 번만 센다
            assertNull(plan.placeAfterRemoval());
            assertEquals(0, plan.crushedRows().size());
            // 폭발한 2~6열마다 범위 맨 위(14행)보다 위의 칸을 내린다
            assertEquals(List.of("2,14", "3,14", "4,14", "5,14", "6,14"), dropColumns(plan));
        }

        @Test
        @DisplayName("바닥까지 2칸이 안 되면 갈 수 있는 만큼만 파고든다")
        void digsOnlyToFloor() {
            Board board = new Board();
            Block tallBoy = landed(board, Block.of(ItemType.TALL_BOY, 0, 3));   // 19행에 닿아 있다

            ClearPlan plan = lock(board, tallBoy);

            assertFalse(plan.hasRemoval());
            assertEquals(1, plan.itemScoreCells());    // 지울 블럭이 없어도 자기 칸 10점
        }

        @Test
        @DisplayName("벽에 붙어 있으면 보드 안쪽만 지운다")
        void staysInsideBoard() {
            Board board = boardWithBottomRows(
                    "XXXXXXXXX.",
                    "XXXXXXXXX."
            );
            Block tallBoy = landed(board, Block.of(ItemType.TALL_BOY, 0, -1));   // 보드 0열

            ClearPlan plan = lock(board, tallBoy);

            // 17행에 멈춘 뒤 19행으로 파고든다. 중심 (19, 0), 범위는 17~19행 × 0~2열만 보드 안쪽이다
            for (int[] cell : plan.removedCells()) {
                assertTrue(cell[1] >= 0 && cell[1] <= 2, "보드 밖 칸: " + cell[1]);
            }
            assertEquals(6, plan.removedCells().size());
            assertEquals(List.of("0,17", "1,17", "2,17"), dropColumns(plan));
        }
    }

    @Nested
    @DisplayName("Crusher (C)")
    class Crusher {

        @Test
        @DisplayName("바닥에 닿아 있으면 자기만 놓고 짓누를 줄이 없다")
        void nothingToCrushOnFloor() {
            Board board = boardWithBottomRows(
                    "X.........",
                    "X.........",
                    "X.........",
                    "X.........",
                    "X........."
            );
            Block crusher = landed(board, Block.of(ItemType.CRUSHER, 0, 3));   // 바닥까지 내려와 17~19행

            ClearPlan plan = lock(board, crusher);

            assertFalse(plan.hasRemoval());                 // 바닥에 닿아 있으면 짓누를 줄이 없다
            assertEquals(ItemType.CRUSHER.code(), board.codeAt(19, 3));
        }

        @Test
        @DisplayName("아래 줄이 3줄 이상이면 3줄, 적으면 있는 만큼 짓누른다")
        void crushesRowsBelow() {
            Board many = boardWithBottomRows("...X......", "...X......", "...X......", "...X......");
            Board few = boardWithBottomRows("...X......");

            ClearPlan manyPlan = lock(many, landed(many, Block.of(ItemType.CRUSHER, 0, 3)));
            ClearPlan fewPlan = lock(few, landed(few, Block.of(ItemType.CRUSHER, 0, 3)));

            assertEquals(List.of(19, 18, 17), manyPlan.crushedRows());
            assertEquals(List.of(19), fewPlan.crushedRows());
            assertTrue(manyPlan.lineRows().isEmpty());
            assertEquals(0, manyPlan.itemScoreCells());
        }
    }
}
