package seoultech.se.tetris.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seoultech.se.tetris.component.model.GarbageLineFixtures.line;

import java.util.ArrayList;
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
import seoultech.se.tetris.component.controller.VersusController;
import seoultech.se.tetris.component.listener.GameEventListener;
import seoultech.se.tetris.component.model.Attack;
import seoultech.se.tetris.component.model.GarbageLine;

/** 대전 모드에서 엔진이 하는 일: 공격 만들기, 공격 받기, 제한 시간 종료, 같은 블럭 순서 */
@DisplayName("TetrisEngine (대전 모드)")
class TetrisEngineVersusTest {

    private static final int FILLED = BlockType.T.code();

    /** 세로 I. 4×4 격자의 2열을 쓴다. */
    private static final Block VERTICAL_I = Block.of(BlockType.I, 0, 0).rotated();

    // ------------------------------------------------------------------
    // 테스트 보조
    // ------------------------------------------------------------------

    /** 엔진이 보낸 이벤트를 기록하는 리스너 */
    private static final class RecordingListener implements GameEventListener {
        final List<Attack> attacks = new ArrayList<>();
        int gameOver;
        int boardUpdated;

        @Override public void onBoardUpdated() { boardUpdated++; }
        @Override public void onLinesCleared(int clearedCount, int gainedScore) { }
        @Override public void onSpeedIncreased(int currentInterval) { }
        @Override public void onGameOver(int finalScore) { gameOver++; }
        @Override public void onAttack(Attack attack) { attacks.add(attack); }
    }

    private final RecordingListener listener = new RecordingListener();

    private TetrisEngine engineWith(int[][] grid) {
        TetrisEngine engine = new TetrisEngine(new BlockFactory(new Random(1)), new Board(grid));
        engine.addListener(listener);
        return engine;
    }

    /** 한 줄을 문자열로 채운다. 'X'는 채운 칸, '.'은 빈 칸 */
    private static void setRow(int[][] grid, int row, String cells) {
        for (int c = 0; c < Board.WIDTH; c++) {
            grid[row][c] = cells.charAt(c) == '.' ? CellCode.EMPTY : FILLED;
        }
    }

    /** 보드의 한 줄을 문자열로 읽는다. 공격 칸은 'G', 그 외 채운 칸은 'X' */
    private static String rowOf(TetrisEngine engine, int row) {
        int[] cells = engine.getBoard()[row];
        StringBuilder text = new StringBuilder();
        for (int code : cells) {
            text.append(code == CellCode.EMPTY ? '.' : CellCode.isGarbage(code) ? 'G' : 'X');
        }
        return text.toString();
    }

    /** 블럭을 지정한 위치에 두고 한 칸 내려 고정한다. 아래가 막혀 있어야 한다. */
    private static void lockAt(TetrisEngine engine, Block block) {
        engine.replaceCurrentBlock(block);
        engine.moveDown();
    }

    private static Attack attack(String... lines) {
        List<GarbageLine> list = new ArrayList<>();
        for (String text : lines) {
            list.add(line(text));
        }
        return new Attack(list);
    }

    private static List<String> textOf(Attack attack) {
        List<String> result = new ArrayList<>();
        for (GarbageLine garbage : attack.lines()) {
            StringBuilder text = new StringBuilder();
            for (int c = 0; c < garbage.width(); c++) {
                text.append(garbage.isFilled(c) ? 'X' : '.');
            }
            result.add(text.toString());
        }
        return result;
    }

    @Test
    @DisplayName("엔진은 대전을 진행하는 쪽의 명령(VersusController)을 받는다")
    void isVersusController() {
        assertTrue(VersusController.class.isAssignableFrom(TetrisEngine.class));
    }

    // ------------------------------------------------------------------
    // 공격 만들기
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("공격 만들기")
    class MakingAttack {

        @Test
        @DisplayName("2줄을 지우면 줄을 지운 블럭의 칸을 비운 2줄이 공격이 된다")
        void twoLinesBecomeAttack() {
            int[][] grid = new int[Board.HEIGHT][Board.WIDTH];
            setRow(grid, 18, "XXXXXXXX..");
            setRow(grid, 19, "XXXXXXXX..");
            TetrisEngine engine = engineWith(grid);

            lockAt(engine, Block.of(BlockType.O, 18, 7));   // O가 18~19행 8·9열을 채운다
            engine.finishClearing();

            assertEquals(1, listener.attacks.size());
            assertEquals(List.of("XXXXXXXX..", "XXXXXXXX.."), textOf(listener.attacks.get(0)));
        }

        @Test
        @DisplayName("1줄만 지우면 공격하지 않는다")
        void oneLineIsNotAttack() {
            int[][] grid = new int[Board.HEIGHT][Board.WIDTH];
            setRow(grid, 19, "XXXXXXXX..");
            TetrisEngine engine = engineWith(grid);

            lockAt(engine, Block.of(BlockType.O, 18, 7));
            engine.finishClearing();

            assertEquals(1, engine.getLinesCleared());
            assertTrue(listener.attacks.isEmpty());
        }

        @Test
        @DisplayName("공격 줄은 보내는 쪽 보드에 있던 순서(위에서 아래)로 담긴다")
        void keepsOrderFromTopToBottom() {
            // S를 세운 모양은 줄마다 빈 칸 위치가 달라 순서를 확인할 수 있다
            Block verticalS = Block.of(BlockType.S, 0, 0).rotated();
            Block placed = verticalS.moved(17, 5);
            int[][] grid = new int[Board.HEIGHT][Board.WIDTH];
            List<String> expected = new ArrayList<>();
            for (int row = 17; row <= 19; row++) {
                StringBuilder text = new StringBuilder();
                for (int col = 0; col < Board.WIDTH; col++) {
                    text.append(occupies(placed, row, col) ? '.' : 'X');
                }
                setRow(grid, row, text.toString());
                expected.add(text.toString());
            }
            TetrisEngine engine = engineWith(grid);

            lockAt(engine, placed);
            engine.finishClearing();

            assertEquals(expected, textOf(listener.attacks.get(0)));
            assertEquals(3, expected.stream().distinct().count());   // 줄마다 모양이 달라야 순서 확인이 의미가 있다
        }

        @Test
        @DisplayName("공격은 애니메이션이 끝나 실제로 줄을 지울 때 보낸다")
        void sentAfterAnimation() {
            int[][] grid = new int[Board.HEIGHT][Board.WIDTH];
            setRow(grid, 18, "XXXXXXXX..");
            setRow(grid, 19, "XXXXXXXX..");
            TetrisEngine engine = engineWith(grid);

            lockAt(engine, Block.of(BlockType.O, 18, 7));
            assertTrue(engine.isClearing());
            assertTrue(listener.attacks.isEmpty());

            engine.finishClearing();
            assertEquals(1, listener.attacks.size());
        }

        @Test
        @DisplayName("줄 삭제 아이템(L)으로 지우는 꽉 차지 않은 줄은 보내지 않고, 꽉 찬 줄만 보낸다")
        void lineClearItemSendsOnlyFullRows() {
            // 세로 I(9열, 16~19행)의 맨 위 칸에 L. 16행은 꽉 차지 않는다
            Block withL = VERTICAL_I.withItem(ItemType.LINE_CLEAR, 0, 2).moved(16, 7);
            int[][] grid = new int[Board.HEIGHT][Board.WIDTH];
            setRow(grid, 16, "XXXX......");
            setRow(grid, 17, "XXXXXXXXX.");
            setRow(grid, 18, "XXXXXXXXX.");
            setRow(grid, 19, "XXXXXXXXX.");
            TetrisEngine engine = engineWith(grid);

            lockAt(engine, withL);
            engine.finishClearing();

            assertEquals(4, engine.getLinesCleared());   // 16행도 줄 삭제로 지워진다
            assertEquals(List.of("XXXXXXXXX.", "XXXXXXXXX.", "XXXXXXXXX."), textOf(listener.attacks.get(0)));
        }

        @Test
        @DisplayName("Crusher가 짓누른 뒤 줄이 채워지면, 내려간 Crusher의 칸을 비워서 보낸다")
        void crusherFollowsFinalPosition() {
            // Crusher(7~9열)가 15~17행에 놓여 그 줄들을 채우고, 아래 2줄을 짓눌러 17~19행으로 내려간다
            int[][] grid = new int[Board.HEIGHT][Board.WIDTH];
            setRow(grid, 15, "XXXXXXX...");
            setRow(grid, 16, "XXXXXXX...");
            setRow(grid, 17, "XXXXXXX...");
            setRow(grid, 18, "XXXXXXXX..");
            setRow(grid, 19, "XXXXXXXX..");
            TetrisEngine engine = engineWith(grid);

            lockAt(engine, Block.of(ItemType.CRUSHER, 15, 7));
            engine.finishClearing();   // 짓누르기
            assertTrue(listener.attacks.isEmpty());   // 짓누르기는 줄 삭제가 아니다
            engine.finishClearing();   // 이어지는 일반 줄 삭제

            assertEquals(List.of("XXXXXXX...", "XXXXXXX...", "XXXXXXX..."), textOf(listener.attacks.get(0)));
        }

        private boolean occupies(Block block, int row, int col) {
            for (int r = 0; r < BlockType.SIZE; r++) {
                for (int c = 0; c < BlockType.SIZE; c++) {
                    if (block.isFilled(r, c) && block.boardRow(r) == row && block.boardCol(c) == col) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    // ------------------------------------------------------------------
    // 공격 받기
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("공격 받기")
    class ReceivingAttack {

        @Test
        @DisplayName("받은 공격은 먼저 대기 줄에만 나타나고 보드는 바뀌지 않는다")
        void waitsInQueue() {
            TetrisEngine engine = engineWith(new int[Board.HEIGHT][Board.WIDTH]);
            Attack attack = attack("XXX.XXXXXX", "XXXX.XXXXX");
            int updatesBefore = listener.boardUpdated;

            engine.receiveAttack(attack);

            assertEquals(attack.lines(), engine.getPendingGarbage());
            assertEquals("..........", rowOf(engine, 19));
            assertTrue(listener.boardUpdated > updatesBefore);   // 대기 영역을 다시 그리도록 알린다
        }

        @Test
        @DisplayName("현재 블럭을 고정한 뒤 다음 블럭이 생성되기 전에 대기 줄 전체가 보드 아래쪽에 추가된다")
        void addedBeforeNextSpawn() {
            TetrisEngine engine = engineWith(new int[Board.HEIGHT][Board.WIDTH]);
            engine.receiveAttack(attack("XXX.XXXXXX", "XXXX.XXXXX"));

            lockAt(engine, Block.of(BlockType.O, 18, 7));

            assertTrue(engine.getPendingGarbage().isEmpty());
            assertEquals("........XX", rowOf(engine, 16));   // 고정한 O가 2줄 밀려 올라갔다
            assertEquals("........XX", rowOf(engine, 17));
            assertEquals("GGG.GGGGGG", rowOf(engine, 18));
            assertEquals("GGGG.GGGGG", rowOf(engine, 19));
        }

        @Test
        @DisplayName("자신의 줄 삭제를 먼저 마친 뒤 대기 줄이 추가된다")
        void ownClearComesFirst() {
            int[][] grid = new int[Board.HEIGHT][Board.WIDTH];
            setRow(grid, 19, "XXXXXXXX..");
            TetrisEngine engine = engineWith(grid);
            engine.receiveAttack(attack("XXX.XXXXXX", "XXXX.XXXXX"));

            lockAt(engine, Block.of(BlockType.O, 18, 7));   // 19행이 채워진다
            assertFalse(engine.getPendingGarbage().isEmpty());   // 애니메이션 중에는 아직 대기 중
            engine.finishClearing();

            assertEquals(1, engine.getLinesCleared());
            assertEquals("........XX", rowOf(engine, 17));   // O의 윗줄만 남아 2줄 올라갔다
            assertEquals("GGG.GGGGGG", rowOf(engine, 18));
            assertEquals("GGGG.GGGGG", rowOf(engine, 19));
        }

        @Test
        @DisplayName("공격 줄에 밀려 보드 위로 넘치는 칸이 생기면 즉시 게임 오버다")
        void overflowIsGameOver() {
            int[][] grid = new int[Board.HEIGHT][Board.WIDTH];
            grid[1][0] = FILLED;   // 2줄이 밀려 올라가면 보드 위로 넘친다
            TetrisEngine engine = engineWith(grid);
            engine.receiveAttack(attack("XXX.XXXXXX", "XXXX.XXXXX"));

            lockAt(engine, Block.of(BlockType.O, 18, 7));

            assertTrue(engine.isGameOver());
            assertEquals(1, listener.gameOver);
        }

        @Test
        @DisplayName("게임이 끝난 뒤에 받은 공격은 무시한다")
        void ignoredAfterGameOver() {
            TetrisEngine engine = engineWith(new int[Board.HEIGHT][Board.WIDTH]);
            engine.exitGame();

            engine.receiveAttack(attack("XXX.XXXXXX", "XXXX.XXXXX"));

            assertTrue(engine.getPendingGarbage().isEmpty());
        }

        @Test
        @DisplayName("공격 줄의 폭이 보드 폭과 다르면 예외를 던진다")
        void rejectsWrongWidth() {
            TetrisEngine engine = engineWith(new int[Board.HEIGHT][Board.WIDTH]);

            assertThrows(IllegalArgumentException.class,
                    () -> engine.receiveAttack(attack("XXX.XXXXXX", "XXX.XXXXX")));
        }

        @Test
        @DisplayName("다시 시작하면 대기 줄이 비워진다")
        void restartClearsQueue() {
            TetrisEngine engine = engineWith(new int[Board.HEIGHT][Board.WIDTH]);
            engine.receiveAttack(attack("XXX.XXXXXX", "XXXX.XXXXX"));

            engine.restartGame();

            assertTrue(engine.getPendingGarbage().isEmpty());
        }
    }

    // ------------------------------------------------------------------
    // 제한 시간 종료
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("제한 시간 종료")
    class TimeUp {

        @Test
        @DisplayName("진행 중이면 다음 블럭을 만들지 않고 끝내며, 게임 오버 알림은 보내지 않는다")
        void endsWithoutGameOverEvent() {
            TetrisEngine engine = engineWith(new int[Board.HEIGHT][Board.WIDTH]);
            int score = engine.getScore();

            engine.finishByTimeUp();

            assertTrue(engine.isGameOver());
            assertEquals(score, engine.getScore());
            assertEquals(0, listener.gameOver);
        }

        @Test
        @DisplayName("대기 중인 공격 줄은 보드에 추가하지 않는다")
        void pendingGarbageIsNotAdded() {
            TetrisEngine engine = engineWith(new int[Board.HEIGHT][Board.WIDTH]);
            engine.receiveAttack(attack("XXX.XXXXXX", "XXXX.XXXXX"));

            engine.finishByTimeUp();

            assertEquals("..........", rowOf(engine, 19));
        }

        @Test
        @DisplayName("줄 삭제 애니메이션 중이면 즉시 마무리해 점수에 반영하지만, 공격은 보내지 않는다")
        void finishesClearingWithoutAttack() {
            int[][] grid = new int[Board.HEIGHT][Board.WIDTH];
            setRow(grid, 18, "XXXXXXXX..");
            setRow(grid, 19, "XXXXXXXX..");
            TetrisEngine engine = engineWith(grid);
            lockAt(engine, Block.of(BlockType.O, 18, 7));
            int scoreBefore = engine.getScore();

            engine.finishByTimeUp();

            assertEquals(2, engine.getLinesCleared());
            assertTrue(engine.getScore() > scoreBefore);
            assertTrue(listener.attacks.isEmpty());
            assertTrue(engine.isGameOver());
            assertFalse(engine.isClearing());
        }

        @Test
        @DisplayName("아이템 효과 중이면 이어지는 일반 줄 삭제까지 한 번에 마무리한다")
        void finishesFollowingLineClear() {
            int[][] grid = new int[Board.HEIGHT][Board.WIDTH];
            setRow(grid, 15, "XXXXXXX...");
            setRow(grid, 16, "XXXXXXX...");
            setRow(grid, 17, "XXXXXXX...");
            setRow(grid, 18, "XXXXXXXX..");
            setRow(grid, 19, "XXXXXXXX..");
            TetrisEngine engine = engineWith(grid);
            lockAt(engine, Block.of(ItemType.CRUSHER, 15, 7));   // 짓누르기 애니메이션 중

            engine.finishByTimeUp();

            assertEquals(3, engine.getLinesCleared());
            assertTrue(listener.attacks.isEmpty());
        }

        @Test
        @DisplayName("이미 끝난 게임에서는 아무 일도 하지 않는다")
        void nothingAfterGameOver() {
            TetrisEngine engine = engineWith(new int[Board.HEIGHT][Board.WIDTH]);
            engine.exitGame();
            int updates = listener.boardUpdated;

            engine.finishByTimeUp();

            assertEquals(updates, listener.boardUpdated);
        }
    }

    // ------------------------------------------------------------------
    // 같은 블럭 순서
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("같은 블럭 순서")
    class Seeded {

        @Test
        @DisplayName("같은 시드로 만든 엔진은 블럭이 같은 순서로 나온다")
        void sameSeedSameOrder() {
            TetrisEngine first = TetrisEngine.seeded(7L, Difficulty.NORMAL, GameMode.ITEM);
            TetrisEngine second = TetrisEngine.seeded(7L, Difficulty.NORMAL, GameMode.ITEM);

            assertEquals(spawnOrder(first), spawnOrder(second));
        }

        @Test
        @DisplayName("다른 시드면 순서가 달라질 수 있다")
        void differentSeedDifferentOrder() {
            assertNotEquals(
                    spawnOrder(TetrisEngine.seeded(1L, Difficulty.NORMAL, GameMode.NORMAL)),
                    spawnOrder(TetrisEngine.seeded(2L, Difficulty.NORMAL, GameMode.NORMAL)));
        }

        @Test
        @DisplayName("지정한 난이도와 모드로 만들어진다")
        void keepsDifficultyAndMode() {
            TetrisEngine engine = TetrisEngine.seeded(3L, Difficulty.HARD, GameMode.ITEM);

            assertEquals(Difficulty.HARD, engine.getDifficulty());
            assertEquals(GameMode.ITEM, engine.getGameMode());
        }

        /** 블럭 다섯 개를 떨어뜨리며 나온 블럭 종류 */
        private List<String> spawnOrder(TetrisEngine engine) {
            List<String> order = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                order.add(engine.getCurrentBlock().shape().toString());
                engine.dropToBottom();
                engine.finishClearing();
            }
            return order;
        }
    }
}
