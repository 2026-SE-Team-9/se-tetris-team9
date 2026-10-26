package seoultech.se.tetris.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import seoultech.se.tetris.blocks.Block;
import seoultech.se.tetris.blocks.BlockType;
import seoultech.se.tetris.blocks.BlockFactory;
import seoultech.se.tetris.blocks.ItemFactory;
import seoultech.se.tetris.blocks.ItemType;
import seoultech.se.tetris.component.listener.GameEventListener;

@DisplayName("TetrisEngine")
class TetrisEngineTest {

    /** 시드를 고정해 블럭 순서를 재현 가능하게 만든다. */
    private static final int SEED = 42;

    private TetrisEngine engine;
    private RecordingListener listener;

    @BeforeEach
    void setUp() {
        engine = new TetrisEngine(new BlockFactory(new Random(SEED)));
        listener = new RecordingListener();
        engine.addListener(listener);
    }

    // ------------------------------------------------------------------
    // 테스트 보조
    // ------------------------------------------------------------------

    /** 어떤 이벤트가 몇 번 왔는지 기록하는 리스너. */
    private static class RecordingListener implements GameEventListener {
        int boardUpdated;
        int speedIncreased;
        int gameOver;
        int clearingStarted;
        int lastFinalScore;
        final List<int[]> linesCleared = new ArrayList<>();   // {줄 수, 얻은 점수}
        final List<Integer> intervals = new ArrayList<>();

        @Override
        public void onBoardUpdated() {
            boardUpdated++;
        }

        @Override
        public void onLinesCleared(int clearedCount, int gainedScore) {
            linesCleared.add(new int[] {clearedCount, gainedScore});
        }

        @Override
        public void onSpeedIncreased(int currentInterval) {
            speedIncreased++;
            intervals.add(currentInterval);
        }

        @Override
        public void onGameOver(int finalScore) {
            gameOver++;
            lastFinalScore = finalScore;
        }

        @Override
        public void onClearingStarted() {
            clearingStarted++;
        }
    }

    /** 현재 블럭이 고정될 때까지 아래로 내린다. */
    private void dropCurrentBlock() {
        engine.dropToBottom();
    }

    /** 블럭을 지정한 수만큼 고정한다. */
    private void dropBlocks(int count) {
        for (int i = 0; i < count; i++) {
            engine.dropToBottom();
        }
    }

    // ------------------------------------------------------------------
    // 초기 상태
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("초기 상태")
    class InitialState {

        @Test
        @DisplayName("게임이 진행 중이다")
        void startsPlaying() {
            assertFalse(engine.isPaused());
            assertFalse(engine.isGameOver());
        }

        @Test
        @DisplayName("점수와 삭제 줄 수가 0이다")
        void startsWithZeroScore() {
            assertEquals(0, engine.getScore());
            assertEquals(0, engine.getLinesCleared());
        }

        @Test
        @DisplayName("레벨 1에서 시작한다")
        void startsAtInitialLevel() {
            assertEquals(LevelPolicy.INITIAL_LEVEL, engine.getLevel());
        }

        @Test
        @DisplayName("현재 블럭과 다음 블럭이 준비되어 있다")
        void hasCurrentAndNextBlock() {
            assertNotNull(engine.getCurrentBlock());
            assertNotNull(engine.getNextBlock());
        }

        @Test
        @DisplayName("보드가 비어 있다")
        void startsWithEmptyBoard() {
            for (int[] row : engine.getBoard()) {
                for (int cell : row) {
                    assertEquals(0, cell);
                }
            }
        }

        @Test
        @DisplayName("블럭이 보드 가운데에서 생성된다")
        void spawnsAtCenter() {
            assertEquals(TetrisEngine.SPAWN_COL, engine.getCurrentX());
            assertEquals(Board.SPAWN_ROW, engine.getCurrentY());
        }
    }

    // ------------------------------------------------------------------
    // 이동
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("이동")
    class Movement {

        @Test
        @DisplayName("좌우로 한 칸씩 움직인다")
        void movesHorizontally() {
            int startX = engine.getCurrentX();

            engine.moveLeft();
            assertEquals(startX - 1, engine.getCurrentX());

            engine.moveRight();
            assertEquals(startX, engine.getCurrentX());
        }

        @Test
        @DisplayName("왼쪽 벽에 닿으면 더 움직이지 않는다")
        void stopsAtLeftWall() {
            for (int i = 0; i < Board.WIDTH * 2; i++) {
                engine.moveLeft();
            }

            // 블럭이 실제로 차지한 칸이 왼쪽 벽(0열)에 닿아 있어야 한다
            assertEquals(0, leftmostCol(engine.getCurrentBlock()));
            // 더 눌러도 위치가 변하지 않는다
            int stuck = engine.getCurrentX();
            engine.moveLeft();
            assertEquals(stuck, engine.getCurrentX());
        }

        @Test
        @DisplayName("오른쪽 벽에 닿으면 더 움직이지 않는다")
        void stopsAtRightWall() {
            for (int i = 0; i < Board.WIDTH * 2; i++) {
                engine.moveRight();
            }

            // 블럭이 실제로 차지한 칸이 오른쪽 벽(마지막 열)에 닿아 있어야 한다
            assertEquals(Board.WIDTH - 1, rightmostCol(engine.getCurrentBlock()));
            // 더 눌러도 위치가 변하지 않는다
            int stuck = engine.getCurrentX();
            engine.moveRight();
            assertEquals(stuck, engine.getCurrentX());
        }

        /** 블럭이 실제로 차지한 칸 중 가장 왼쪽 열. 모양 격자의 빈 칸은 무시한다. */
        private int leftmostCol(Block block) {
            int min = Integer.MAX_VALUE;
            for (int r = 0; r < BlockType.SIZE; r++) {
                for (int c = 0; c < BlockType.SIZE; c++) {
                    if (block.isFilled(r, c)) {
                        min = Math.min(min, block.boardCol(c));
                    }
                }
            }
            return min;
        }

        /** 블럭이 실제로 차지한 칸 중 가장 오른쪽 열. 모양 격자의 빈 칸은 무시한다. */
        private int rightmostCol(Block block) {
            int max = Integer.MIN_VALUE;
            for (int r = 0; r < BlockType.SIZE; r++) {
                for (int c = 0; c < BlockType.SIZE; c++) {
                    if (block.isFilled(r, c)) {
                        max = Math.max(max, block.boardCol(c));
                    }
                }
            }
            return max;
        }

        @Test
        @DisplayName("아래로 내리면 행이 늘어난다")
        void movesDown() {
            int startY = engine.getCurrentY();

            engine.moveDown();

            assertEquals(startY + 1, engine.getCurrentY());
        }

        @Test
        @DisplayName("아래로 내릴 때마다 점수를 얻는다")
        void earnsScoreOnDrop() {
            engine.moveDown();

            assertEquals(LevelPolicy.INITIAL_LEVEL, engine.getScore());
        }

        @Test
        @DisplayName("이동이 성공하면 화면 갱신을 알린다")
        void notifiesOnSuccessfulMove() {
            int before = listener.boardUpdated;

            engine.moveLeft();

            assertEquals(before + 1, listener.boardUpdated);
        }

        @Test
        @DisplayName("반시계 회전은 시계 회전의 반대 방향이다")
        void rotatesCounterClockwise() {
            int before = engine.getCurrentBlock().rotation();

            engine.rotateCounterclockwise();

            assertEquals(BlockType.normalize(before - 1),
                engine.getCurrentBlock().rotation());
        }
    }

    // ------------------------------------------------------------------
    // 하드 드롭과 고정
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("하드 드롭")
    class HardDrop {

        @Test
        @DisplayName("바닥까지 내려간 뒤 새 블럭이 생성된다")
        void locksAndSpawnsNext() {
            Block before = engine.getCurrentBlock();

            engine.dropToBottom();

            assertEquals(Board.SPAWN_ROW, engine.getCurrentY());
            assertEquals(2, engine.getBlocksSpawned());
            // 새 블럭이 생성되었으므로 이전 블럭과 같은 객체가 아니다
            assertFalse(before == engine.getCurrentBlock());
        }

        @Test
        @DisplayName("보드에 블럭이 고정된다")
        void placesBlockOnBoard() {
            engine.dropToBottom();

            boolean anyFilled = false;
            for (int[] row : engine.getBoard()) {
                for (int cell : row) {
                    if (cell != 0) {
                        anyFilled = true;
                    }
                }
            }
            assertTrue(anyFilled, "고정된 블럭이 보드에 없습니다.");
        }

        @Test
        @DisplayName("떨어진 칸 수만큼 점수를 얻는다")
        void earnsScoreForDistance() {
            int ghostY = engine.getGhostY();
            int startY = engine.getCurrentY();
            int expected = (ghostY - startY) * LevelPolicy.INITIAL_LEVEL;

            engine.dropToBottom();

            assertEquals(expected, engine.getScore());
        }

        @Test
        @DisplayName("고스트 위치는 실제로 멈추는 위치와 같다")
        void ghostMatchesLandingPosition() {
            int ghostY = engine.getGhostY();
            int spawned = engine.getBlocksSpawned();

            // 한 칸씩 끝까지 내려본다
            int y = engine.getCurrentY();
            while (true) {
                engine.moveDown();
                if (engine.getBlocksSpawned() != spawned) {
                    break;          // 고정되어 새 블럭이 생성됨
                }
                y = engine.getCurrentY();
            }

            assertEquals(ghostY, y);
        }

        @Test
        @DisplayName("화면 갱신 알림 시점에 점수가 이미 반영되어 있다")
        void scoreIsUpdatedBeforeNotification() {
            List<Integer> seen = new ArrayList<>();
            engine.addListener(new GameEventListener() {
                @Override public void onBoardUpdated() { seen.add(engine.getScore()); }
                @Override public void onLinesCleared(int clearedCount, int gainedScore) { }
                @Override public void onSpeedIncreased(int currentInterval) { }
                @Override public void onGameOver(int finalScore) { }
            });

            engine.moveDown();

            // 알림은 한 번만 오고, 그 시점에 점수가 이미 반영되어 있어야 한다
            assertEquals(List.of(engine.getScore()), seen);
        }
    }

    // ------------------------------------------------------------------
    // 일시정지
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("일시정지")
    class Pause {

        @Test
        @DisplayName("일시정지와 재개를 토글한다")
        void togglesPause() {
            engine.togglePause();
            assertTrue(engine.isPaused());

            engine.togglePause();
            assertFalse(engine.isPaused());
        }

        @Test
        @DisplayName("일시정지 중에는 조작이 먹히지 않는다")
        void ignoresInputWhilePaused() {
            engine.pauseGame();
            int x = engine.getCurrentX();
            int y = engine.getCurrentY();
            int score = engine.getScore();

            engine.moveLeft();
            engine.moveRight();
            engine.moveDown();
            engine.rotate();
            engine.dropToBottom();

            assertEquals(x, engine.getCurrentX());
            assertEquals(y, engine.getCurrentY());
            assertEquals(score, engine.getScore());
        }

        @Test
        @DisplayName("재개하면 다시 조작할 수 있다")
        void resumesInput() {
            engine.pauseGame();
            engine.resumeGame();
            int x = engine.getCurrentX();

            engine.moveLeft();

            assertEquals(x - 1, engine.getCurrentX());
        }
    }

    // ------------------------------------------------------------------
    // 종료와 재시작
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("종료와 재시작")
    class EndAndRestart {

        @Test
        @DisplayName("중단하면 진행이 멈춘다")
        void exitStopsGame() {
            engine.exitGame();

            assertTrue(engine.isGameOver());
        }

        @Test
        @DisplayName("중단은 게임오버 이벤트를 발생시키지 않는다")
        void exitDoesNotFireGameOver() {
            // 사용자가 중단한 것은 스코어보드 기록 대상이 아니다
            engine.exitGame();

            assertEquals(0, listener.gameOver);
        }

        @Test
        @DisplayName("재시작하면 모든 상태가 초기화된다")
        void restartResetsEverything() {
            dropBlocks(3);
            engine.restartGame();

            assertEquals(0, engine.getScore());
            assertEquals(0, engine.getLinesCleared());
            assertEquals(LevelPolicy.INITIAL_LEVEL, engine.getLevel());
            assertEquals(1, engine.getBlocksSpawned());
            assertFalse(engine.isGameOver());

            for (int[] row : engine.getBoard()) {
                for (int cell : row) {
                    assertEquals(0, cell);
                }
            }
        }

        @Test
        @DisplayName("보드가 가득 차면 게임이 끝난다")
        void gameOverWhenBoardFills() {
            // 같은 자리에 계속 쌓으면 결국 생성 위치가 막힌다
            for (int i = 0; i < 200 && !engine.isGameOver(); i++) {
                engine.dropToBottom();
            }

            assertTrue(engine.isGameOver(), "블럭을 계속 쌓았는데 게임이 끝나지 않았습니다.");
            assertEquals(1, listener.gameOver);
            assertEquals(engine.getScore(), listener.lastFinalScore);
        }

        @Test
        @DisplayName("게임이 끝나면 조작이 먹히지 않는다")
        void ignoresInputAfterGameOver() {
            engine.exitGame();
            int x = engine.getCurrentX();

            engine.moveLeft();

            assertEquals(x, engine.getCurrentX());
        }
    }

    // ------------------------------------------------------------------
    // 리스너
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("리스너")
    class Listeners {

        @Test
        @DisplayName("여러 개를 등록하면 모두에게 알린다")
        void notifiesAllListeners() {
            RecordingListener second = new RecordingListener();
            engine.addListener(second);

            engine.moveLeft();

            assertEquals(1, second.boardUpdated);
            assertTrue(listener.boardUpdated >= 1);
        }

        @Test
        @DisplayName("해제하면 더 이상 알리지 않는다")
        void stopsNotifyingAfterRemoval() {
            engine.removeListener(listener);
            int before = listener.boardUpdated;

            engine.moveLeft();

            assertEquals(before, listener.boardUpdated);
        }
    }

    // ------------------------------------------------------------------
    // 줄 삭제·백투백·레벨
    // ------------------------------------------------------------------

    /**
     * 미리 채운 보드와 I 블럭만 주는 생성기로 줄 삭제 상황을 정확히 재현한다.
     *
     * <p>I 블럭은 생성 위치({@link TetrisEngine#SPAWN_COL} = 3)에서
     * 가로(회전 0)일 때 보드 3~6열, 세로(회전 1)일 때 보드 5열을 차지한다.
     */
    @Nested
    @DisplayName("줄 삭제·백투백·레벨")
    class LineClearAndLevel {

        /** 보드를 채울 때 쓰는 블럭 번호. 어떤 블럭이든 0이 아니면 된다. */
        private static final int FILLED = BlockType.O.code();

        /** 세로 I 블럭이 들어가는 열 */
        private static final int VERTICAL_I_COL = 5;

        private RecordingListener events;

        /** 항상 같은 종류의 블럭만 주는 생성기. 중간 난이도. */
        private BlockFactory always(BlockType type) {
            return always(type, Difficulty.NORMAL);
        }

        /** 항상 같은 종류의 블럭만 주는 생성기. 난이도를 지정한다. */
        private BlockFactory always(BlockType type, Difficulty difficulty) {
            return new BlockFactory(new Random(0), difficulty) {
                @Override
                public BlockType next() {
                    return type;
                }
            };
        }

        /** fromRow~toRow 행을 채우되 emptyCols 열은 비워 둔다. */
        private void fillRows(int[][] grid, int fromRow, int toRow, int... emptyCols) {
            for (int r = fromRow; r <= toRow; r++) {
                for (int c = 0; c < Board.WIDTH; c++) {
                    grid[r][c] = FILLED;
                }
                for (int c : emptyCols) {
                    grid[r][c] = 0;
                }
            }
        }

        /** 미리 채운 보드와 I 블럭만으로 엔진을 만들고 이벤트 기록을 시작한다. 중간 난이도. */
        private TetrisEngine engineWith(int[][] grid) {
            return engineWith(grid, Difficulty.NORMAL);
        }

        /** 미리 채운 보드와 I 블럭만으로 엔진을 만들고 이벤트 기록을 시작한다. 난이도를 지정한다. */
        private TetrisEngine engineWith(int[][] grid, Difficulty difficulty) {
            TetrisEngine created = new TetrisEngine(always(BlockType.I, difficulty), new Board(grid));
            events = new RecordingListener();
            created.addListener(events);
            return created;
        }

        /**
         * 바닥까지 떨어뜨리고, 줄 삭제 애니메이션이 시작되면 바로 끝낸다.
         *
         * <p>실제 게임에서는 고정 길이 타이머가 {@code finishClearing()}을 부르지만,
         * 엔진은 타이머를 갖지 않으므로 테스트에서 직접 부른다. 애니메이션이 없으면 아무 일도 하지 않는다.
         */
        private void dropAndClear(TetrisEngine target) {
            target.dropToBottom();
            target.finishClearing();
        }

        /** 세로로 세운 I 블럭을 바닥까지 떨어뜨리고 줄 삭제를 마친다. */
        private void dropVerticalI(TetrisEngine target) {
            target.rotate();
            dropAndClear(target);
        }

        /** 19행을 가로 I 자리(3~6열)만 비워 둔 보드. 가로 I를 떨어뜨리면 한 줄을 지운다. */
        private TetrisEngine engineWithSingleLineReady() {
            int[][] grid = new int[Board.HEIGHT][Board.WIDTH];
            fillRows(grid, 19, 19, 3, 4, 5, 6);
            return engineWith(grid);
        }

        /** onLinesCleared로 받은 (줄 수, 얻은 점수) 목록. */
        private List<List<Integer>> clearEvents() {
            return events.linesCleared.stream()
                    .map(e -> List.of(e[0], e[1]))
                    .toList();
        }

        @Test
        @DisplayName("한 줄을 지우면 줄 삭제 점수를 얻고 이벤트를 알린다")
        void clearsSingleLine() {
            int[][] grid = new int[Board.HEIGHT][Board.WIDTH];
            fillRows(grid, 19, 19, 3, 4, 5, 6);
            TetrisEngine target = engineWith(grid);

            dropAndClear(target);           // 가로 I가 18칸 떨어져 19행을 채운다

            assertEquals(1, target.getLinesCleared());
            assertEquals(List.of(List.of(1, 100)), clearEvents());
            assertEquals(18 + 100, target.getScore());
        }

        @Test
        @DisplayName("네 줄을 동시에 지우면 1000점을 얻는다")
        void clearsFourLines() {
            int[][] grid = new int[Board.HEIGHT][Board.WIDTH];
            fillRows(grid, 16, 19, VERTICAL_I_COL);
            TetrisEngine target = engineWith(grid);

            dropVerticalI(target);          // 세로 I가 16칸 떨어져 16~19행을 채운다

            assertEquals(4, target.getLinesCleared());
            assertEquals(List.of(List.of(4, 1000)), clearEvents());
            assertEquals(16 + 1000, target.getScore());
        }

        @Test
        @DisplayName("네 줄 삭제가 연속되면 두 번째는 백투백으로 2배가 된다")
        void appliesBackToBack() {
            int[][] grid = new int[Board.HEIGHT][Board.WIDTH];
            fillRows(grid, 12, 19, VERTICAL_I_COL);
            TetrisEngine target = engineWith(grid);

            dropVerticalI(target);
            dropVerticalI(target);

            assertEquals(List.of(List.of(4, 1000), List.of(4, 2000)), clearEvents());
            assertEquals(16 + 1000 + 16 + 2000, target.getScore());
        }

        @Test
        @DisplayName("사이에 1~3줄 삭제가 끼면 백투백이 끊긴다")
        void singleClearBreaksBackToBack() {
            int[][] grid = new int[Board.HEIGHT][Board.WIDTH];
            fillRows(grid, 16, 19, VERTICAL_I_COL);   // 첫 번째 네 줄
            fillRows(grid, 12, 15, VERTICAL_I_COL);   // 세 번째 네 줄
            fillRows(grid, 11, 11, 3, 4, 5, 6);       // 두 번째 한 줄 (가로 I)
            TetrisEngine target = engineWith(grid);

            dropVerticalI(target);          // 16~19행 삭제 → 위 줄들이 4칸 내려온다
            dropAndClear(target);           // 가로 I로 한 줄 삭제 → 연속이 끊긴다
            dropVerticalI(target);          // 다시 네 줄 삭제지만 백투백이 아니다

            assertEquals(List.of(List.of(4, 1000), List.of(1, 100), List.of(4, 1000)),
                    clearEvents());
        }

        @Test
        @DisplayName("지운 줄이 10줄을 넘으면 레벨이 오르고 낙하 간격 변경을 알린다")
        void levelRisesByLines() {
            int[][] grid = new int[Board.HEIGHT][Board.WIDTH];
            fillRows(grid, 8, 19, VERTICAL_I_COL);
            TetrisEngine target = engineWith(grid);

            dropVerticalI(target);          // 4줄
            dropVerticalI(target);          // 8줄
            assertEquals(LevelPolicy.INITIAL_LEVEL, target.getLevel());
            assertTrue(events.intervals.isEmpty());

            dropVerticalI(target);          // 12줄 → 레벨 2

            assertEquals(12, target.getLinesCleared());
            assertEquals(2, target.getLevel());
            assertEquals(List.of(new LevelPolicy().dropIntervalMs(2)), events.intervals);
        }

        @Test
        @DisplayName("재시작하면 레벨 1의 낙하 간격을 다시 알리고 보드를 비운다")
        void restartResetsDropInterval() {
            int[][] grid = new int[Board.HEIGHT][Board.WIDTH];
            fillRows(grid, 8, 19, VERTICAL_I_COL);
            TetrisEngine target = engineWith(grid);
            dropVerticalI(target);
            dropVerticalI(target);
            dropVerticalI(target);          // 레벨 2

            target.restartGame();

            assertEquals(LevelPolicy.INITIAL_LEVEL, target.getLevel());
            assertEquals(LevelPolicy.INITIAL_INTERVAL_MS,
                    events.intervals.get(events.intervals.size() - 1));
            for (int[] row : target.getBoard()) {
                for (int cell : row) {
                    assertEquals(0, cell);
                }
            }
        }

        @Test
        @DisplayName("레벨이 오를 때 알리는 낙하 간격이 난이도에 따라 다르다")
        void dropIntervalDependsOnDifficulty() {
            for (Difficulty difficulty : Difficulty.values()) {
                int[][] grid = new int[Board.HEIGHT][Board.WIDTH];
                fillRows(grid, 8, 19, VERTICAL_I_COL);
                TetrisEngine target = engineWith(grid, difficulty);

                dropVerticalI(target);
                dropVerticalI(target);
                dropVerticalI(target);      // 12줄 → 레벨 2

                assertEquals(2, target.getLevel(), difficulty.name());
                assertEquals(List.of(specLevel2IntervalMs(difficulty)), events.intervals,
                        difficulty.name());
            }
        }

        // --------------------------------------------------------------
        // 아이템 등장 (아이템 모드)
        // --------------------------------------------------------------

        /** 항상 같은 아이템을 고르는 생성기. 포함형이면 붙는 칸은 실제 코드대로 고른다. */
        private ItemFactory alwaysItem(ItemType item) {
            return new ItemFactory(new Random(0)) {
                @Override
                public ItemType nextItem() {
                    return item;
                }
            };
        }

        /** 8~19행을 세로 I 자리(5열)만 비운 보드. 세로 I 하나마다 4줄씩, 세 개로 12줄을 지운다. */
        private int[][] twelveLinesReady() {
            int[][] grid = new int[Board.HEIGHT][Board.WIDTH];
            fillRows(grid, 8, 19, VERTICAL_I_COL);
            return grid;
        }

        /** I 블럭만 나오는 아이템 모드 엔진. */
        private TetrisEngine itemModeEngine(int[][] grid, ItemType item) {
            TetrisEngine created = new TetrisEngine(always(BlockType.I), alwaysItem(item),
                    GameMode.ITEM, new Board(grid));
            events = new RecordingListener();
            created.addListener(events);
            return created;
        }

        @Test
        @DisplayName("아이템 모드에서 10줄을 넘기면 다다음 블럭이 아이템 블럭이 된다")
        void itemAppearsAfterTenLines() {
            TetrisEngine target = itemModeEngine(twelveLinesReady(), ItemType.CRUSHER);

            dropVerticalI(target);          // 4줄
            dropVerticalI(target);          // 8줄
            assertNull(target.getNextBlock().item());

            dropVerticalI(target);          // 12줄 → 10줄 단위를 넘김

            assertEquals(12, target.getLinesCleared());
            assertNull(target.getCurrentBlock().item());
            assertSame(ItemType.CRUSHER, target.getNextBlock().item());
        }

        @Test
        @DisplayName("다음 블럭 영역에 이미 보이던 블럭은 바꾸지 않는다")
        void keepsBlockAlreadyShown() {
            TetrisEngine target = itemModeEngine(twelveLinesReady(), ItemType.WEIGHT);
            dropVerticalI(target);
            dropVerticalI(target);
            Block shownBefore = target.getNextBlock();

            dropVerticalI(target);

            assertEquals(shownBefore.moved(Board.SPAWN_ROW, TetrisEngine.SPAWN_COL), target.getCurrentBlock());
        }

        @Test
        @DisplayName("포함형 아이템은 원래 다다음 블럭이던 테트로미노에 붙는다")
        void embeddedItemAttachesToOriginalBlock() {
            TetrisEngine target = itemModeEngine(twelveLinesReady(), ItemType.LINE_CLEAR);

            dropVerticalI(target);
            dropVerticalI(target);
            dropVerticalI(target);

            Block next = target.getNextBlock();
            assertSame(BlockType.I, next.shape());
            assertSame(ItemType.LINE_CLEAR, next.item());
        }

        @Test
        @DisplayName("아이템 블럭도 생성 위치에서 등장한다")
        void itemBlockSpawnsAtSpawnPosition() {
            TetrisEngine target = itemModeEngine(twelveLinesReady(), ItemType.TALL_BOY);
            dropVerticalI(target);
            dropVerticalI(target);
            dropVerticalI(target);          // 12줄을 모두 지워 보드가 빈다

            target.dropToBottom();          // 보이던 블럭을 내려놓으면 아이템 블럭이 생성된다

            Block current = target.getCurrentBlock();
            assertSame(ItemType.TALL_BOY, current.item());
            assertEquals(Board.SPAWN_ROW, current.row());
            assertEquals(TetrisEngine.SPAWN_COL, current.col());
        }

        @Test
        @DisplayName("일반 모드에서는 10줄을 넘겨도 아이템이 나오지 않는다")
        void normalModeHasNoItems() {
            TetrisEngine target = engineWith(twelveLinesReady());

            dropVerticalI(target);
            dropVerticalI(target);
            dropVerticalI(target);

            assertEquals(12, target.getLinesCleared());
            assertNull(target.getCurrentBlock().item());
            assertNull(target.getNextBlock().item());
        }

        // --------------------------------------------------------------
        // 아이템 효과
        // --------------------------------------------------------------

        /** 미리 채운 보드에서 현재 블럭을 아이템 블럭으로 바꿔 시작한다. 블럭은 생성 위치에 놓는다. */
        private TetrisEngine engineWithCurrent(int[][] grid, Block itemBlock) {
            TetrisEngine created = engineWith(grid);
            created.replaceCurrentBlock(itemBlock.moved(Board.SPAWN_ROW, TetrisEngine.SPAWN_COL));
            return created;
        }

        /** 한 줄을 문자열로 채운다. 'X'는 채운 칸, 'D'는 Double or Nothing 칸, '.'은 빈 칸이다. */
        private void setRow(int[][] grid, int row, String cells) {
            for (int c = 0; c < Board.WIDTH; c++) {
                char ch = cells.charAt(c);
                grid[row][c] = ch == '.' ? 0 : ch == 'D' ? ItemType.DOUBLE_OR_NOTHING.code() : FILLED;
            }
        }

        @Test
        @DisplayName("줄 삭제 아이템: 꽉 차지 않은 줄도 지우고 줄 삭제로 점수와 줄 수를 반영한다")
        void lineClearItemClearsRow() {
            int[][] grid = new int[Board.HEIGHT][Board.WIDTH];
            setRow(grid, 19, "X.........");
            TetrisEngine target = engineWithCurrent(grid,
                    Block.of(BlockType.I, 0, 0).withItem(ItemType.LINE_CLEAR, 1, 0));

            target.dropToBottom();
            assertTrue(target.isClearing());
            target.finishClearing();

            assertEquals(1, target.getLinesCleared());
            assertEquals(List.of(List.of(1, 100)), clearEvents());
            assertEquals(18 + 100, target.getScore());
            assertEquals(BlockType.EMPTY_CODE, target.getBoard()[19][0]);
        }

        @Test
        @DisplayName("Double or Nothing: D가 있는 줄을 꽉 채워 지우면 줄 삭제 점수가 2배다")
        void doubleOrNothingDoublesScore() {
            int[][] grid = new int[Board.HEIGHT][Board.WIDTH];
            setRow(grid, 19, "DXX....XXX");
            TetrisEngine target = engineWith(grid);

            dropAndClear(target);

            assertEquals(List.of(List.of(1, 200)), clearEvents());
            assertEquals(18 + 200, target.getScore());
        }

        @Test
        @DisplayName("무게추: 바닥이나 블럭에 닿기 전에는 좌우로 움직이고, 닿은 뒤에는 움직이지 않는다")
        void weightCannotSlideAfterLanding() {
            TetrisEngine target = engineWithCurrent(new int[Board.HEIGHT][Board.WIDTH],
                    Block.of(ItemType.WEIGHT, 0, 0));

            target.moveLeft();
            assertEquals(TetrisEngine.SPAWN_COL - 1, target.getCurrentX());

            for (int i = 0; i < 18; i++) {
                target.moveDown();                       // 아랫줄이 19행에 닿을 때까지
            }
            target.moveLeft();
            assertEquals(TetrisEngine.SPAWN_COL - 1, target.getCurrentX());
            target.moveRight();
            assertEquals(TetrisEngine.SPAWN_COL - 1, target.getCurrentX());
        }

        @Test
        @DisplayName("무게추: 아래 칸을 지워 칸당 10점을 얻고, 바닥에 내려앉아 줄이 차면 일반 줄 삭제로 이어진다")
        void weightRemovesCellsThenClearsFilledLine() {
            int[][] grid = new int[Board.HEIGHT][Board.WIDTH];
            setRow(grid, 18, "...XXXX...");
            setRow(grid, 19, "XXX....XXX");
            TetrisEngine target = engineWithCurrent(grid, Block.of(ItemType.WEIGHT, 0, 0));

            target.dropToBottom();                       // 아랫줄이 17행에 멈춘다 (16칸 낙하)
            assertTrue(target.isClearing());
            assertTrue(target.getClearingCells()[18][3]);
            assertFalse(target.getClearingCells()[19][0]);

            target.finishClearing();                     // 18행 4칸 제거 → 바닥에 내려앉아 19행이 찬다
            assertTrue(target.isClearing());
            assertEquals(0, target.getLinesCleared());   // 무게추가 지운 칸은 줄 삭제가 아니다
            assertEquals(16 + 40, target.getScore());

            target.finishClearing();

            assertEquals(1, target.getLinesCleared());
            assertEquals(List.of(List.of(1, 100)), clearEvents());
            assertEquals(16 + 40 + 100, target.getScore());
        }

        @Test
        @DisplayName("Tall Boy: 파고든 자리 중심 5×5를 지우고 자기 칸을 포함해 칸당 10점을 얻는다")
        void tallBoyExplodes() {
            int[][] grid = new int[Board.HEIGHT][Board.WIDTH];
            fillRows(grid, 15, 19, 9);
            TetrisEngine target = engineWithCurrent(grid, Block.of(ItemType.TALL_BOY, 0, 0));

            target.dropToBottom();                       // 4열 14행에 멈춘다 (14칸 낙하)
            target.finishClearing();

            // 중심 (16, 4), 14~18행 × 2~6열 중 블럭이 있던 20칸 (중심 칸은 자기 칸으로 한 번)
            assertEquals(14 + 200, target.getScore());
            assertEquals(0, target.getLinesCleared());
            assertEquals(BlockType.EMPTY_CODE, target.getBoard()[16][4]);
            assertTrue(target.getBoard()[19][4] != BlockType.EMPTY_CODE);   // 범위 밖은 그대로
            assertTrue(target.getBoard()[15][1] != BlockType.EMPTY_CODE);
        }

        @Test
        @DisplayName("Tall Boy: 폭발 범위 위쪽의 블럭은 떠 있지 않고 아래가 막힐 때까지 내려온다")
        void tallBoyDropsBlocksAbove() {
            int[][] grid = new int[Board.HEIGHT][Board.WIDTH];
            for (int r = 11; r <= 14; r++) {
                setRow(grid, r, "..XX......");       // 2~3열의 탑. 아래가 모두 폭발 범위 안이다
            }
            setRow(grid, 15, "XXXX.XXXX.");
            fillRows(grid, 16, 19, 9);
            TetrisEngine target = engineWithCurrent(grid, Block.of(ItemType.TALL_BOY, 0, 0));

            target.dropToBottom();                       // 4열 15행에 멈춘 뒤 17행으로 파고든다
            target.finishClearing();                     // 15~19행 × 2~6열 폭발

            int[][] board = target.getBoard();
            for (int r = 11; r <= 15; r++) {
                assertEquals(BlockType.EMPTY_CODE, board[r][2], "떠 있는 칸이 남았습니다: " + r + "행");
            }
            for (int r = 16; r <= 19; r++) {
                assertTrue(board[r][2] != BlockType.EMPTY_CODE && board[r][3] != BlockType.EMPTY_CODE,
                        "탑이 바닥까지 내려와야 합니다: " + r + "행");
            }
            assertTrue(board[19][1] != BlockType.EMPTY_CODE);   // 범위 밖 열은 그대로
        }

        @Test
        @DisplayName("Crusher: 아래 줄을 짓누르고, 내려앉아 줄이 차면 일반 줄 삭제로 이어진다")
        void crusherCrushesThenClearsFilledLine() {
            int[][] grid = new int[Board.HEIGHT][Board.WIDTH];
            setRow(grid, 18, "XXX...XXXX");
            setRow(grid, 19, "...XXX...X");
            TetrisEngine target = engineWithCurrent(grid, Block.of(ItemType.CRUSHER, 0, 0));

            target.dropToBottom();                       // 16~18행에 멈춘다 (16칸 낙하)
            target.finishClearing();                     // 19행을 짓누르면 18행이 내려와 꽉 찬다
            assertTrue(target.isClearing());
            assertEquals(0, target.getLinesCleared());   // 짓누른 줄은 줄 삭제가 아니다
            assertEquals(16, target.getScore());

            target.finishClearing();

            assertEquals(1, target.getLinesCleared());
            assertEquals(16 + 100, target.getScore());
            assertEquals(ItemType.CRUSHER.code(), target.getBoard()[19][3]);   // 남은 Crusher 두 줄이 내려온다
        }

        // --------------------------------------------------------------
        // 줄 삭제 애니메이션
        // --------------------------------------------------------------

        @Test
        @DisplayName("줄이 가득 차면 바로 지우지 않고 애니메이션을 시작한다")
        void startsClearingInsteadOfRemovingAtOnce() {
            TetrisEngine target = engineWithSingleLineReady();

            target.dropToBottom();

            assertTrue(target.isClearing());
            assertFalse(target.isPaused());
            for (int cell : target.getBoard()[19]) {
                assertTrue(cell != BlockType.EMPTY_CODE, "지울 줄이 아직 보드에 남아 있어야 한다");
            }
            assertEquals(0, target.getLinesCleared());
            assertTrue(clearEvents().isEmpty());
            assertEquals(18, target.getScore());         // 하드 드롭 점수만 반영
            assertEquals(1, target.getBlocksSpawned());  // 다음 블럭은 아직 생성하지 않는다
            assertEquals(1, events.clearingStarted);
        }

        @Test
        @DisplayName("애니메이션 동안 지워질 칸을 알려준다")
        void reportsClearingCells() {
            TetrisEngine target = engineWithSingleLineReady();
            assertFalse(target.getClearingCells()[19][0]);

            target.dropToBottom();

            boolean[][] cells = target.getClearingCells();
            assertEquals(Board.VISIBLE_HEIGHT, cells.length);
            for (int c = 0; c < Board.WIDTH; c++) {
                assertTrue(cells[19][c]);
                assertFalse(cells[18][c]);
            }
        }

        @Test
        @DisplayName("애니메이션 동안에는 일시정지를 포함한 모든 조작을 무시한다")
        void ignoresInputWhileClearing() {
            TetrisEngine target = engineWithSingleLineReady();
            target.dropToBottom();
            int[][] before = target.getBoard();
            Block blockBefore = target.getCurrentBlock();

            target.moveLeft();
            target.moveRight();
            target.moveDown();
            target.rotate();
            target.rotateCounterclockwise();
            target.dropToBottom();
            target.pauseGame();
            target.togglePause();

            assertTrue(target.isClearing());
            assertFalse(target.isPaused());
            assertSame(blockBefore, target.getCurrentBlock());
            assertEquals(18, target.getScore());
            for (int r = 0; r < Board.VISIBLE_HEIGHT; r++) {
                for (int c = 0; c < Board.WIDTH; c++) {
                    assertEquals(before[r][c], target.getBoard()[r][c]);
                }
            }
        }

        @Test
        @DisplayName("애니메이션이 끝나면 줄을 지우고 점수를 반영한 뒤 다음 블럭을 생성한다")
        void finishClearingRemovesLinesAndSpawnsNext() {
            TetrisEngine target = engineWithSingleLineReady();
            target.dropToBottom();

            target.finishClearing();

            assertFalse(target.isClearing());
            for (int cell : target.getBoard()[19]) {
                assertEquals(BlockType.EMPTY_CODE, cell);
            }
            assertEquals(1, target.getLinesCleared());
            assertEquals(List.of(List.of(1, 100)), clearEvents());
            assertEquals(18 + 100, target.getScore());
            assertEquals(2, target.getBlocksSpawned());
            assertFalse(target.getClearingCells()[19][0]);
        }

        @Test
        @DisplayName("애니메이션 중이 아닐 때 끝내기를 호출하면 아무 일도 하지 않는다")
        void finishClearingIsIgnoredWhenNotClearing() {
            TetrisEngine target = engineWithSingleLineReady();
            int updatesBefore = events.boardUpdated;

            target.finishClearing();

            assertEquals(1, target.getBlocksSpawned());
            assertEquals(0, target.getScore());
            assertEquals(updatesBefore, events.boardUpdated);
        }

        @Test
        @DisplayName("애니메이션 도중 중단하면 끝내기를 호출해도 줄을 지우지 않는다")
        void exitDuringClearingCancelsIt() {
            TetrisEngine target = engineWithSingleLineReady();
            target.dropToBottom();

            target.exitGame();
            target.finishClearing();

            assertTrue(target.isGameOver());
            assertFalse(target.isClearing());
            assertEquals(0, target.getLinesCleared());
            assertEquals(0, events.gameOver);
        }

        @Test
        @DisplayName("애니메이션 도중 재시작하면 애니메이션을 취소하고 새로 시작한다")
        void restartDuringClearingCancelsIt() {
            TetrisEngine target = engineWithSingleLineReady();
            target.dropToBottom();

            target.restartGame();
            target.finishClearing();

            assertFalse(target.isClearing());
            assertEquals(0, target.getLinesCleared());
            assertEquals(0, target.getScore());
            assertEquals(1, target.getBlocksSpawned());
        }

        @Test
        @DisplayName("애니메이션 도중 게임 종료 조건이 생기면 애니메이션을 마친 뒤 종료한다")
        void gameOverComesAfterAnimation() {
            int[][] grid = new int[Board.HEIGHT][Board.WIDTH];
            fillRows(grid, 2, 2, 3, 4, 5, 6);       // 가로 I가 채울 줄
            for (int c = 3; c <= 6; c++) {
                grid[0][c] = FILLED;                 // 2행을 지우면 1행으로 내려와 생성 자리를 막는다
                grid[3][c] = FILLED;                 // 가로 I가 2행에서 멈추도록 받친다
            }
            TetrisEngine target = engineWith(grid);

            target.dropToBottom();
            assertTrue(target.isClearing());
            assertFalse(target.isGameOver());
            assertEquals(0, events.gameOver);

            target.finishClearing();

            assertTrue(target.isGameOver());
            assertEquals(1, events.gameOver);
            assertEquals(1, target.getLinesCleared());
        }

        /**
         * 요구사항 문서의 레벨 2 낙하 간격(ms).
         *
         * <p>{@link LevelPolicy}로 계산하면 같은 식을 두 번 쓰는 셈이라 식이 틀려도 통과하므로,
         * 문서의 값을 그대로 적는다.
         */
        private int specLevel2IntervalMs(Difficulty difficulty) {
            return switch (difficulty) {
                case EASY -> 840;
                case NORMAL -> 800;
                case HARD -> 760;
            };
        }
    }

    // ------------------------------------------------------------------
    // 난이도
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("난이도")
    class DifficultySetting {

        @Test
        @DisplayName("난이도를 지정하지 않으면 기본 난이도로 시작한다")
        void usesDefaultDifficultyWhenNotGiven() {
            assertSame(Difficulty.DEFAULT, new TetrisEngine().getDifficulty());
        }

        @Test
        @DisplayName("지정한 난이도로 시작한다")
        void usesGivenDifficulty() {
            for (Difficulty difficulty : Difficulty.values()) {
                assertSame(difficulty, new TetrisEngine(difficulty).getDifficulty());
            }
        }

        @Test
        @DisplayName("블럭 생성기를 넘기면 생성기의 난이도를 따른다")
        void followsBlockFactoryDifficulty() {
            for (Difficulty difficulty : Difficulty.values()) {
                TetrisEngine created = new TetrisEngine(new BlockFactory(new Random(SEED), difficulty));

                assertSame(difficulty, created.getDifficulty());
            }
        }

        @Test
        @DisplayName("재시작해도 난이도는 바뀌지 않는다")
        void restartKeepsDifficulty() {
            TetrisEngine hard = new TetrisEngine(new BlockFactory(new Random(SEED), Difficulty.HARD));
            hard.dropToBottom();

            hard.restartGame();

            assertSame(Difficulty.HARD, hard.getDifficulty());
        }

        @Test
        @DisplayName("난이도가 null이면 예외가 발생한다")
        void rejectsNullDifficulty() {
            // 생성자가 둘 다 인자 하나라 null만 넘기면 어느 쪽인지 정할 수 없으므로 형을 지정한다
            assertThrows(NullPointerException.class, () -> new TetrisEngine((Difficulty) null));
        }
    }

    // ------------------------------------------------------------------
    // 모드
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("모드")
    class Mode {

        @Test
        @DisplayName("모드를 지정하지 않으면 일반 모드로 시작한다")
        void defaultsToNormalMode() {
            assertSame(GameMode.NORMAL, new TetrisEngine().getGameMode());
            assertSame(GameMode.NORMAL, new TetrisEngine(Difficulty.HARD).getGameMode());
            assertSame(GameMode.NORMAL, engine.getGameMode());
        }

        @Test
        @DisplayName("지정한 난이도와 모드로 시작한다")
        void usesGivenDifficultyAndMode() {
            TetrisEngine created = new TetrisEngine(Difficulty.HARD, GameMode.ITEM);

            assertSame(Difficulty.HARD, created.getDifficulty());
            assertSame(GameMode.ITEM, created.getGameMode());
        }

        @Test
        @DisplayName("재시작해도 모드는 바뀌지 않는다")
        void restartKeepsMode() {
            TetrisEngine created = new TetrisEngine(new BlockFactory(new Random(SEED)),
                    new ItemFactory(new Random(SEED)), GameMode.ITEM);

            created.restartGame();

            assertSame(GameMode.ITEM, created.getGameMode());
        }

        @Test
        @DisplayName("모드나 아이템 생성기가 null이면 예외가 발생한다")
        void rejectsNull() {
            BlockFactory blocks = new BlockFactory(new Random(SEED));
            ItemFactory items = new ItemFactory(new Random(SEED));

            assertThrows(NullPointerException.class, () -> new TetrisEngine(blocks, items, null));
            assertThrows(NullPointerException.class, () -> new TetrisEngine(blocks, null, GameMode.ITEM));
        }
    }

    // ------------------------------------------------------------------
    // 재현성
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("재현성")
    class Reproducibility {

        @Test
        @DisplayName("같은 시드면 같은 블럭 순서가 나온다")
        void sameSeedProducesSameSequence() {
            TetrisEngine first = new TetrisEngine(new BlockFactory(new Random(SEED)));
            TetrisEngine second = new TetrisEngine(new BlockFactory(new Random(SEED)));

            for (int i = 0; i < 20; i++) {
                assertSame(first.getCurrentBlock().type(), second.getCurrentBlock().type());
                first.dropToBottom();
                second.dropToBottom();
            }

            assertEquals(first.getScore(), second.getScore());
        }
    }
}
