package seoultech.se.tetris.engine;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import seoultech.se.tetris.blocks.Block;
import seoultech.se.tetris.blocks.BlockFactory;
import seoultech.se.tetris.blocks.BlockType;
import seoultech.se.tetris.blocks.ItemFactory;
import seoultech.se.tetris.component.controller.GameController;
import seoultech.se.tetris.component.listener.GameEventListener;
import seoultech.se.tetris.component.model.GameState;

/**
 * 게임 진행을 담당한다.
 *
 * <p>{@link Board}, {@link BlockFactory}, {@link LevelPolicy}, {@link ScoreCalculator}를
 * 조립해 실제 게임을 굴린다. 계산은 각각의 component가 담당하고, 이 클래스는 순서를 정하고 상태를 보관한다.
 *
 * <p>하나의 객체가 {@link GameState}와 {@link GameController}를 모두 구현한다.
 * 같은 데이터를 보는 두 가지 창구이며, 화면 쪽에는 읽기용({@code GameState})으로,
 * 조작 쪽에는 명령용({@code GameController})으로 넘기면 각자에게 필요한 면만 보인다.
 *
 * <p><b>타이머를 갖지 않는다.</b> {@link #moveDown()}이 호출되면 반응할 뿐이며,
 * 주기적으로 부르는 것은 조작 쪽의 일이다. 덕분에 실제 시간을 기다리지 않고
 * {@code moveDown()}을 반복 호출하는 방식으로 게임 전체를 테스트할 수 있다.
 */
public class TetrisEngine implements GameState, GameController {

    /** 내부에서 미리 뽑아 두는 다음 블럭 수. 화면에 표시하는 숫자와는 무관하다. */
    public static final int NEXT_QUEUE_SIZE = 5;

    /** 블럭이 생성되는 열. 4칸 격자를 보드 가운데에 맞춘다. */
    public static final int SPAWN_COL = (Board.WIDTH - BlockType.SIZE) / 2;

    /** 아이템이 생성되는 줄 간격. 10줄마다 하나씩 생성된다. */
    public static final int ITEM_LINE_INTERVAL = 10;

    /* 각 component들의 객체 */
    private final Board board;
    private final BlockFactory blockFactory;
    private final LevelPolicy levelPolicy;
    private final ScoreCalculator scoreCalculator;
    private final ItemFactory itemFactory;

    /** 설정된 게임 난이도 */
    private final Difficulty difficulty;

    /** 설정된 게임 모드 */
    private final GameMode mode;
    /** 게임 이벤트를 받을 리스너 목록 */
    private final List<GameEventListener> listeners = new ArrayList<>();

    /** 다음 블럭을 저장하는 큐. 아이템도 담아야 하므로 Block 타입을 사용한다. */
    private final Deque<Block> nextQueue = new ArrayDeque<>(NEXT_QUEUE_SIZE);

    /* 현재 게임 상태 변수 */
    private Block current;
    private int score;
    private int linesCleared;
    private int blocksSpawned;
    private int level;
    private boolean lastClearWasBackToBackEligible;
    private Status status;

    /** 애니메이션이 끝나면 적용할 계획. 애니메이션 중이 아니면 {@code null}. */
    private ClearPlan pendingPlan;

    /**
     * 게임의 상태. 진행중, 일시정지, 줄 삭제 애니메이션, 종료 상태를 나타낸다.
     *
     * <p>애니메이션 표시 상태(CLEARING)는 사용자의 일시정지 상태(PAUSED)와 구분한다.
     * 같은 상태를 쓰면 애니메이션 도중 일시정지 메뉴가 뜨거나, 일시정지 해제로 애니메이션을 건너뛰게 된다.
     */
    private enum Status {
        PLAYING, PAUSED, CLEARING, OVER
    }

    /** 실제 게임용. 기본 난이도, 블럭의 등장 순서를 랜덤으로 설정한다. */
    public TetrisEngine() {
        this(Difficulty.DEFAULT);
    }

    /** 실제 게임용. 일반 모드로, 지정한 난이도와 블럭 등장 순서를 랜덤으로 설정한다. */
    public TetrisEngine(Difficulty difficulty) {
        this(difficulty, GameMode.NORMAL);
    }

    /** 실제 게임용. 지정한 난이도와 모드, 블럭 등장 순서를 랜덤으로 설정한다. */
    public TetrisEngine(Difficulty difficulty, GameMode mode) {
        this(new BlockFactory(new Random(), difficulty), new ItemFactory(new Random()), mode);
    }

    /**
     * 블럭 생성기를 지정해 만든다.
     *
     * <p>난이도는 블럭 생성기의 난이도를 따른다. 블럭 확률과 속도 규칙 난이도를 통일한다.
     *
     * <p>시드를 고정한 {@link Random}을 넘기면 같은 순서가 재현되므로 테스트에 쓸 수 있다.
     */
    public TetrisEngine(BlockFactory blockFactory) {
        this(blockFactory, new Board());
    }

    /**
     * 블럭 생성기, 아이템 생성기, 모드를 모두 지정해 만든다.
     *
     * <p>두 생성기에 시드를 고정한 {@link Random}을 넘기면 아이템 모드의 진행도 재현된다.
     */
    public TetrisEngine(BlockFactory blockFactory, ItemFactory itemFactory, GameMode mode) {
        this(blockFactory, itemFactory, mode, new Board());
    }

    /**
     * 테스트용. 미리 채운 보드로 시작한다.
     *
     * <p>줄 삭제·백투백·레벨 상승처럼 무작위 블럭으로는 만들기 어려운 상황을 재현하기 위한 것으로,
     * 같은 패키지의 테스트에서만 사용한다.
     */
    TetrisEngine(BlockFactory blockFactory, Board board) {
        this(blockFactory, new ItemFactory(new Random()), GameMode.NORMAL, board);
    }

    /** 테스트용. 지정된 블럭 생성기, 아이템 생성기, 모드, 그리고 보드로 시작한다. */
    TetrisEngine(BlockFactory blockFactory, ItemFactory itemFactory, GameMode mode, Board board) {
        this.board = Objects.requireNonNull(board, "Board는 null일 수 없습니다.");
        this.blockFactory = Objects.requireNonNull(blockFactory, "BlockFactory는 null일 수 없습니다.");
        this.itemFactory = Objects.requireNonNull(itemFactory, "ItemFactory는 null일 수 없습니다.");
        this.mode = Objects.requireNonNull(mode, "GameMode는 null일 수 없습니다.");
        this.difficulty = blockFactory.difficulty();
        this.levelPolicy = new LevelPolicy(difficulty);
        this.scoreCalculator = new ScoreCalculator();
        start();
    }

    // ------------------------------------------------------------------
    // 리스너
    // ------------------------------------------------------------------

    /**
     * 게임 이벤트를 받을 대상을 등록한다.
     *
     * <p>여러 개를 등록할 수 있다. 화면 갱신은 시각화 쪽이,
     * 낙하 간격 변경({@link GameEventListener#onSpeedIncreased(int)})은 타이머를 가진
     * 조작 쪽이 받아야 하므로 한 번에 여러 개의 리스너가 필요하다.
     */
    public void addListener(GameEventListener listener) {
        listeners.add(Objects.requireNonNull(listener, "리스너는 null일 수 없습니다."));
    }

    public void removeListener(GameEventListener listener) {
        listeners.remove(listener);
    }

    private void fireBoardUpdated() {
        for (GameEventListener listener : listeners) {
            listener.onBoardUpdated();
        }
    }

    private void fireLinesCleared(int count, int gainedScore) {
        for (GameEventListener listener : listeners) {
            listener.onLinesCleared(count, gainedScore);
        }
    }

    private void fireSpeedIncreased(int intervalMs) {
        for (GameEventListener listener : listeners) {
            listener.onSpeedIncreased(intervalMs);
        }
    }

    private void fireGameOver(int finalScore) {
        for (GameEventListener listener : listeners) {
            listener.onGameOver(finalScore);
        }
    }

    private void fireClearingStarted() {
        for (GameEventListener listener : listeners) {
            listener.onClearingStarted();
        }
    }

    // ------------------------------------------------------------------
    // 진행
    // ------------------------------------------------------------------

    /**
     * 게임 상태를 초기화하고 첫 블럭을 생성한다.
     *
     * <p>보드는 비우지 않는다. 처음 만들 때는 보드가 이미 비어 있고(테스트에서는 미리 채운 보드),
     * 재시작할 때는 {@link #restartGame()}이 먼저 비운다.
     */
    private void start() {
        nextQueue.clear();
        score = 0;
        linesCleared = 0;
        blocksSpawned = 0;
        level = LevelPolicy.INITIAL_LEVEL;
        lastClearWasBackToBackEligible = false;
        pendingPlan = null;
        status = Status.PLAYING;

        fillQueue();
        spawnNext();
    }

    private void fillQueue() {
        while (nextQueue.size() < NEXT_QUEUE_SIZE) {
            nextQueue.addLast(Block.of(blockFactory.next(), 0, 0));
        }
    }

    /** 큐에서 다음 블럭을 꺼내 생성한다. 놓을 자리가 없으면 게임 종료 */
    private void spawnNext() {
        Block next = nextQueue.removeFirst();
        fillQueue();

        Block spawned = next.moved(Board.SPAWN_ROW, SPAWN_COL);
        blocksSpawned++;

        if (!board.canPlace(spawned)) {
            current = spawned;
            status = Status.OVER;
            return;
        }

        current = spawned;
        updateLevel();
    }

    /** 레벨이 올랐으면 낙하 간격 변경을 알린다. */
    private void updateLevel() {
        int newLevel = levelPolicy.level(linesCleared, blocksSpawned);
        if (newLevel != level) {
            level = newLevel;
            fireSpeedIncreased(levelPolicy.dropIntervalMs(level));
        }
    }

    /**
     * 현재 블럭을 고정한다.
     *
     * <p>블럭의 아이템 효과(아이템이 없으면 일반 블럭의 동작)가 무엇을 지울지 계획을 세우고,
     * 지울 칸이 있으면 애니메이션을 거쳐 {@link #finishClearing()}에서, 없으면 바로 계획을 적용한다.
     * 줄 삭제와 점수 처리, 다음 블럭 생성은 계획을 적용한 뒤에 한다.
     */
    private void lockCurrentBlock() {
        LockContext context = new LockContext(board, current);
        ItemEffects.of(current.item()).onLock(context);
        runPlan(context.plan());
    }

    /** 지울 칸이 있으면 애니메이션을 시작하고, 없으면 바로 적용하고 이어간다. */
    private void runPlan(ClearPlan plan) {
        if (plan.hasRemoval()) {
            pendingPlan = plan;
            status = Status.CLEARING;
            fireBoardUpdated();
            fireClearingStarted();
            return;
        }
        applyPlan(plan);
        continueAfter(plan);
    }

    /**
     * {@inheritDoc}
     *
     * <p>보관한 계획을 적용한 뒤, 아이템 효과로 줄이 채워졌으면 일반 줄 삭제를 이어서 하고 아니면 다음 블럭을 생성한다.
     * 애니메이션 중이 아니면(중단·재시작으로 이미 끝난 경우 포함) 아무 일도 하지 않는다.
     */
    @Override
    public void finishClearing() {
        if (status != Status.CLEARING) {
            return;
        }
        ClearPlan plan = pendingPlan;
        pendingPlan = null;
        applyPlan(plan);
        status = Status.PLAYING;
        continueAfter(plan);
    }

    /** 계획대로 보드를 바꾸고 점수를 반영한다. */
    private void applyPlan(ClearPlan plan) {
        if (plan.isLineClear()) {
            applyLineClear(plan);
        }
        for (int[] cell : plan.removedCells()) {
            board.clearCell(cell[0], cell[1]);
        }
        for (int[] column : plan.dropColumns()) {
            board.dropAbove(column[0], column[1]);
        }
        board.removeRows(plan.crushedRows());
        if (plan.placeAfterRemoval() != null) {
            board.place(plan.placeAfterRemoval());
        }
        score += scoreCalculator.itemScore(plan.itemScoreCells(), level);
    }

    /**
     * 줄 삭제를 적용하고 점수·지운 줄 수·백투백·아이템 등장을 반영한다.
     *
     * <p>점수는 지운 줄 수와 레벨로 계산한 뒤, 꽉 찬 줄에 Double or Nothing이 있으면 배수를 곱한다.
     * 줄 삭제 아이템(L)으로 지운 줄도 지운 줄 수와 10줄 카운트에 포함한다.
     *
     * <p>백투백은 직전 줄 삭제와 이번 줄 삭제가 모두 백투백 조건(4줄 동시 삭제)을 만족할 때 적용되며,
     * 연속으로 만족하는 동안 계속 이어진다. 조건 미만의 줄을 지우면 끊기고, 줄을 지우지 않는 동작은 영향이 없다.
     * 무게추·Tall Boy·Crusher의 효과는 줄 삭제가 아니므로 백투백에 영향을 주지 않는다.
     */
    private void applyLineClear(ClearPlan plan) {
        int cleared = plan.lineRows().size();
        board.removeRows(plan.lineRows());

        boolean backToBack = lastClearWasBackToBackEligible
                && scoreCalculator.isBackToBackEligible(cleared);
        int gained = scoreCalculator.lineClearScore(cleared, level, backToBack)
                * plan.lineScoreMultiplier();

        int linesBefore = linesCleared;
        score += gained;
        linesCleared += cleared;
        lastClearWasBackToBackEligible = scoreCalculator.isBackToBackEligible(cleared);
        if (reachedItemLine(linesBefore, linesCleared)) {
            scheduleItemBlock();
        }
        fireLinesCleared(cleared, gained);
    }

    /** 줄 삭제였으면 다음 블럭, 아이템 효과였으면 꽉 찬 줄을 다시 확인해 일반 줄 삭제로 처리한다. */
    private void continueAfter(ClearPlan plan) {
        if (!plan.isLineClear()) {
            List<Integer> fullRows = board.fullRows();
            if (!fullRows.isEmpty()) {
                ClearPlan lineClear = new ClearPlan();
                lineClear.setLineClear(board, fullRows);
                runPlan(lineClear);
                return;
            }
        }
        spawnNextAndNotify();
    }

    /**
     * 이번 줄 삭제로 아이템 등장 기준(10줄 단위)을 새로 넘겼는지 여부. 아이템 모드에서만 참이 될 수 있다.
     *
     * <p>여러 줄을 지워 단위를 건너뛴 경우(예: 9줄 → 12줄)도 넘긴 것으로 본다.
     * 한 번에 지울 수 있는 줄은 최대 4줄이므로 두 단위를 동시에 넘기는 일은 없다.
     */
    private boolean reachedItemLine(int before, int after) {
        return mode == GameMode.ITEM
                && before / ITEM_LINE_INTERVAL != after / ITEM_LINE_INTERVAL;
    }

    /**
     * 다다음 블럭(큐의 두 번째)을 아이템 블럭으로 바꾼다.
     *
     * <p>다음 블럭 영역에 이미 보이고 있는 블럭(큐의 첫 번째)은 바꾸지 않는다. (2차 팀 결정)
     * 그 블럭이 생성되고 나면 다음 블럭 영역에 아이템 블럭이 나타난다.
     * 포함형 아이템은 원래 다다음 블럭이던 테트로미노에 붙는다.
     *
     * <p>아이템은 이 메서드만 두 번째 자리에 넣는다. 이 메서드를 부르는 {@link #applyLineClear}는
     * 블럭 하나를 고정할 때 최대 한 번 실행되고 그 뒤에 반드시 블럭이 하나 생성되므로,
     * 다음 호출 때 두 번째 자리는 항상 테트라미노다. {@code blockFactory.next()}는 실행되지 않는 대비용이며,
     * 이후 줄 수를 세는 경로가 늘어 생성 전에 두 번 호출되면 예약된 아이템이 바뀔 수 있다.
     */
    private void scheduleItemBlock() {
        Block shown = nextQueue.removeFirst();
        Block afterNext = nextQueue.removeFirst();
        BlockType base = afterNext.shape() instanceof BlockType type ? type : blockFactory.next();
        nextQueue.addFirst(itemFactory.create(base));
        nextQueue.addFirst(shown);
    }

    /**
     * 다음 블럭을 생성하고 화면 갱신을 알린다. 생성할 자리가 없으면 게임 오버를 알린다.
     *
     * <p>애니메이션 도중 게임 종료 조건이 생겨도 생성은 애니메이션이 끝난 뒤에 하므로,
     * 게임 오버는 항상 애니메이션을 마친 뒤에 알린다.
     */
    private void spawnNextAndNotify() {
        spawnNext();
        fireBoardUpdated();
        if (status == Status.OVER) {
            fireGameOver(score);
        }
    }

    /** 조작을 받을 수 있는 상태인지 여부. */
    private boolean isPlayable() {
        return status == Status.PLAYING;
    }

    /** 현재 블럭을 지정한 위치로 옮긴다. 놓을 수 없으면 아무 일도 하지 않는다. */
    private boolean tryMoveTo(Block candidate) {
        if (!board.canPlace(candidate)) {
            return false;
        }
        current = candidate;
        fireBoardUpdated();
        return true;
    }

    // ------------------------------------------------------------------
    // GameController
    // ------------------------------------------------------------------

    @Override
    public void moveLeft() {
        if (isPlayable() && canSlide()) {
            tryMoveTo(current.moved(0, -1));
        }
    }

    @Override
    public void moveRight() {
        if (isPlayable() && canSlide()) {
            tryMoveTo(current.moved(0, 1));
        }
    }

    /** 좌우로 움직일 수 있는지. 무게추처럼 닿은 뒤 좌우 이동을 막는 아이템은 바닥이나 블럭에 닿으면 막힌다. */
    private boolean canSlide() {
        return ItemEffects.of(current.item()).canSlideWhenLanded()
                || board.canPlace(current.moved(1, 0));
    }

    /**
     * 한 칸 아래로 내린다. 더 내려갈 수 없으면 현재 블럭을 고정한다.
     *
     * <p>자동 낙하와 수동 조작 모두 이 메서드를 쓴다. 요구사항상 점수는 둘을 구분하지 않는다.
     */
    @Override
    public void moveDown() {
        if (!isPlayable()) {
            return;
        }

        Block below = current.moved(1, 0);
        if (board.canPlace(below)) {
            current = below;
            score += scoreCalculator.dropScore(1, level);
            fireBoardUpdated();          // 위치와 점수를 모두 바꾼 뒤에 알린다
        } else {
            lockCurrentBlock();
        }
    }

    /** 더 내려갈 수 없을 때까지 내린 뒤 즉시 고정한다. */
    @Override
    public void dropToBottom() {
        if (!isPlayable()) {
            return;
        }

        int dropped = 0;
        while (board.canPlace(current.moved(1, 0))) {
            current = current.moved(1, 0);
            dropped++;
        }

        score += scoreCalculator.dropScore(dropped, level);
        lockCurrentBlock();
    }

    /**
     * 시계방향으로 90도 회전한다.
     *
     * <p>회전 결과가 벽이나 기존 블럭과 겹치면 좌우로 한 칸씩 밀어 다시 시도하고,
     * 모두 실패하면 회전을 취소한다.
     */
    @Override
    public void rotate() {
        if (!isPlayable()) {
            return;
        }

        Block rotated = current.rotated();
        for (int kick : WALL_KICKS) {
            if (tryMoveTo(rotated.moved(0, kick))) {
                return;
            }
        }
    }

    /**
     * 반시계방향으로 90도 회전한다.
     *
     * <p>회전 결과가 벽이나 기존 블럭과 겹치면 좌우로 한 칸씩 밀어 다시 시도하고,
     * 모두 실패하면 회전을 취소한다.
     */
    @Override
    public void rotateCounterclockwise() {
        if (!isPlayable()) {
            return;
        }

        Block rotated = current.rotatedCounterClockwise();
        for (int kick : WALL_KICKS) {
            if (tryMoveTo(rotated.moved(0, kick))) {
                return;
            }
        }
    }

    /** 회전이 막혔을 때 좌우로 밀어볼 거리. 0은 제자리 회전이다. */
    private static final int[] WALL_KICKS = {0, -1, 1};

    @Override
    public void togglePause() {
        if (status == Status.PLAYING) {
            pauseGame();
        } else if (status == Status.PAUSED) {
            resumeGame();
        }
    }

    @Override
    public void pauseGame() {
        if (status == Status.PLAYING) {
            status = Status.PAUSED;
            fireBoardUpdated();
        }
    }

    @Override
    public void resumeGame() {
        if (status == Status.PAUSED) {
            status = Status.PLAYING;
            fireBoardUpdated();
        }
    }

    @Override
    public void restartGame() {
        board.reset();
        start();
        fireSpeedIncreased(levelPolicy.dropIntervalMs(level));
        fireBoardUpdated();
    }

    /**
     * 게임을 중간에 중단한다.
     *
     * <p>게임 진행만 멈춘다. 화면 전환은 호출하는 쪽이
     * {@code ScreenNavigator}를 통해 처리한다.
     */
    @Override
    public void exitGame() {
        status = Status.OVER;
        fireBoardUpdated();
    }

    // ------------------------------------------------------------------
    // GameState
    // ------------------------------------------------------------------

    /**
     * 고정된 블럭들의 배치.
     *
     * <p>현재 떨어지고 있는 블럭은 포함되지 않는다.
     * 화면에 함께 그리려면 {@link #getCurrentBlock()}과 좌표를 사용한다.
     */
    @Override
    public int[][] getBoard() {
        return board.snapshot();
    }

    @Override
    public Block getCurrentBlock() {
        return current;
    }

    @Override
    public Block getNextBlock() {
        return nextQueue.peekFirst();
    }

    @Override
    public int getCurrentX() {
        return current.col();
    }

    @Override
    public int getCurrentY() {
        return current.row();
    }

    /** 지금 즉시 낙하시켰을 때 현재 블럭이 멈출 행. */
    @Override
    public int getGhostY() {
        Block ghost = current;
        while (board.canPlace(ghost.moved(1, 0))) {
            ghost = ghost.moved(1, 0);
        }
        return ghost.row();
    }

    @Override
    public int getScore() {
        return score;
    }

    @Override
    public int getLinesCleared() {
        return linesCleared;
    }

    /** 현재 레벨. {@link LevelPolicy#INITIAL_LEVEL}부터 시작 */
    @Override
    public int getLevel() {
        return level;
    }

    @Override
    public boolean isPaused() {
        return status == Status.PAUSED;
    }

    @Override
    public boolean isGameOver() {
        return status == Status.OVER;
    }

    @Override
    public boolean isClearing() {
        return status == Status.CLEARING;
    }

    @Override
    public boolean[][] getClearingCells() {
        boolean[][] cells = new boolean[Board.VISIBLE_HEIGHT][Board.WIDTH];
        if (status != Status.CLEARING) {
            return cells;
        }
        boolean[][] planned = pendingPlan.cellsToClear();
        for (int r = 0; r < Board.VISIBLE_HEIGHT; r++) {
            System.arraycopy(planned[r + Board.BUFFER_HEIGHT], 0, cells[r], 0, Board.WIDTH);
        }
        return cells;
    }

    /**
     * 테스트용. 현재 블럭을 지정한 블럭으로 바꾼다.
     *
     * <p>아이템 효과는 아이템 블럭이 현재 블럭일 때만 확인할 수 있는데, 무작위 등장을 기다리지 않고
     * 원하는 위치에서 바로 시험하기 위한 것이다. 같은 패키지의 테스트에서만 사용한다.
     */
    void replaceCurrentBlock(Block block) {
        current = Objects.requireNonNull(block, "블럭은 null일 수 없습니다.");
    }

    /** 지금까지 생성된 블럭 수. 레벨 계산과 테스트에 쓴다. */
    public int getBlocksSpawned() {
        return blocksSpawned;
    }

    /** 이 게임의 모드. 점수 기록에 남길 모드로 쓴다. */
    public GameMode getGameMode() {
        return mode;
    }

    /** 현재 게임 난이도. 점수 기록에도 동일한 난이도를 기록한다. */
    public Difficulty getDifficulty() {
        return difficulty;
    }
}
