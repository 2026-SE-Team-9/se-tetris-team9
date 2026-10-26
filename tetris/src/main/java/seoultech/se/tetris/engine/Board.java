package seoultech.se.tetris.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import seoultech.se.tetris.blocks.Block;
import seoultech.se.tetris.blocks.BlockType;

/**
 * 블럭이 쌓이는 20줄 × 10칸의 보드
 *
 * <p>고정된 블럭들만 보관한다. 현재 떨어지고 있는 블럭은 이 안에 없으며,
 * 바닥이나 다른 블럭에 닿아 더 내려갈 수 없을 때 {@link #place(Block)}로 들어온다.
 *
 * <p>각 칸에는 칸 번호가 저장된다. 테트로미노는 {@link BlockType#code()}, 아이템은 {@link seoultech.se.tetris.blocks.ItemType#code()}이며
 * 빈 칸은 {@link seoultech.se.tetris.blocks.CellCode#EMPTY}다. 번호와 종류의 대응은 {@link seoultech.se.tetris.blocks.CellCode}에서 확인한다.
 * <p>계층 규칙에 따라 색을 저장하지 않으므로 색맹 모드 전환은 이 클래스에 영향을 주지 않는다.
 * <p>(계층 규칙 2 — 모델은 색을 모른다)
 */

public class Board {
    /** 화면에 보이는 줄 수. 20줄 */
    public static final int VISIBLE_HEIGHT = 20;

    /**
     * 보드 위쪽의 숨은 줄 수.
     *
     * <p>블럭은 내부 보드의 0행에서 생성되며 보드 위로 넘어갈 수 없다.
     * <p>표준 테트리스처럼 숨은 생성 영역이 필요해지면 이 값만 늘리고
     * {@link #snapshot()}이 보이는 영역만 잘라 반환하도록 하면 된다.
     */
    public static final int BUFFER_HEIGHT = 0;

    /** 보드 전체 줄 수. 내부 보드의 격자의 크기 */
    public static final int HEIGHT = VISIBLE_HEIGHT + BUFFER_HEIGHT;

    /** 보드의 칸 수 */
    public static final int WIDTH = 10;

    /** 새 블럭이 생성되는 행. 숨은 영역이 있으면 그 위쪽에서 시작한다. */
    public static final int SPAWN_ROW = 0;

    /** [행][열] 순서. 값은 칸 번호({@link seoultech.se.tetris.blocks.CellCode})이며 0은 빈 칸이다. */
    private final int[][] grid;

    public Board() {
        this.grid = new int[HEIGHT][WIDTH];
    }


    /**
     * * 테스트용 코드 *
     *
     * <p>지정한 상태로 보드를 만든다.
     * <p>전달받은 배열을 복사하므로 이후 바깥에서 수정해도 보드는 바뀌지 않는다.
     */
    public Board(int[][] initial) {
        if (initial.length != HEIGHT) {
            throw new IllegalArgumentException("보드는 " + HEIGHT + "줄이어야 합니다.");
        }
        this.grid = new int[HEIGHT][WIDTH];
        for (int r = 0; r < HEIGHT; r++) {
            if (initial[r].length != WIDTH) {
                throw new IllegalArgumentException("각 줄은 " + WIDTH + "칸이어야 합니다.");
            }
            System.arraycopy(initial[r], 0, grid[r], 0, WIDTH);
        }
    }

    /**
     * 블럭을 이 위치에 놓을 수 있는지 판단한다.
     *
     * <p>보드 범위를 벗어나거나 이미 고정된 블럭과 겹치면 놓을 수 없다.
     * <p>이동, 회전, 새 블럭 생성의 가능 판정을 모두 이 메소드로 통일한다.
     */
    public boolean canPlace(Block block) {
        for (int r = 0; r < BlockType.SIZE; r++) {
            for (int c = 0; c < BlockType.SIZE; c++) {
                if (!block.isFilled(r, c)) continue;    // 채워지지 않은 칸은 검사 제외

                int boardRow = block.boardRow(r);
                int boardCol = block.boardCol(c);

                if (boardRow < 0 || boardRow >= HEIGHT) return false; // 보드 위아래 범위
                if (boardCol < 0 || boardCol >= WIDTH) return false; // 보드 좌우 범위
                if (grid[boardRow][boardCol] != BlockType.EMPTY_CODE) return false; // 이미 고정된 블럭이 존재
            }
        }
        return true;
    }

    /**
     * 블럭을 보드에 고정한다.
     *
     * <p>호출 전에 {@link #canPlace(Block)}로 확인해야 한다.
     * 놓을 수 없는 위치에 호출하면 예외를 던진다.
     */
    public void place(Block block) {
        if (!canPlace(block)) {
            throw new IllegalArgumentException("놓을 수 없는 위치입니다: " + block);
        }

        for (int r = 0; r < BlockType.SIZE; r++) {
            for (int c = 0; c < BlockType.SIZE; c++) {
                if (block.isFilled(r, c)) {
                    grid[block.boardRow(r)][block.boardCol(c)] = block.codeAt(r, c);
                }
            }
        }
    }

    /**
     * 가득 찬 줄을 모두 삭제하고 위의 줄들을 아래로 내린다.
     *
     * @return 삭제한 줄의 수
     */
    public int clearFullLines() {
        List<Integer> full = fullRows();
        removeRows(full);
        return full.size();
    }

    /**
     * 지정한 칸이 비어 있는지 여부. 범위를 벗어나면 비어 있지 않은 것으로 본다.
     */
    public boolean isEmpty(int row, int col) {
        if (row < 0 || row >= HEIGHT || col < 0 || col >= WIDTH) {
            return false;
        }
        return grid[row][col] == BlockType.EMPTY_CODE;
    }

    /** 지정한 칸의 칸 번호. 테트로미노 또는 아이템 번호이며, 빈 칸이면 {@link seoultech.se.tetris.blocks.CellCode#EMPTY}. 해석은 {@link seoultech.se.tetris.blocks.CellCode}를 거친다. */
    public int codeAt(int row, int col) {
        return grid[row][col];
    }

    /**
     * 화면에 보이는 영역의 복사본.
     *
     * <p>숨은 줄은 포함하지 않으므로 항상 {@link #VISIBLE_HEIGHT}줄을 반환한다.
     * <p>받은 쪽에서 수정해도 보드는 바뀌지 않는다.
     */
    public int[][] snapshot() {
        int[][] copy = new int[VISIBLE_HEIGHT][WIDTH];
        for (int r = 0; r < VISIBLE_HEIGHT; r++) {
            System.arraycopy(grid[r + BUFFER_HEIGHT], 0, copy[r], 0, WIDTH);
        }
        return copy;
    }

    /**
     * 지정한 칸을 비운다. 위의 칸은 내려오지 않는다.
     *
     * <p>무게추와 Tall Boy처럼 줄이 아닌 칸을 지우는 아이템이 쓴다.
     *
     * @throws IllegalArgumentException 보드 범위를 벗어난 칸인 경우
     */
    public void clearCell(int row, int col) {
        if (row < 0 || row >= HEIGHT || col < 0 || col >= WIDTH) {
            throw new IllegalArgumentException("보드 범위를 벗어난 칸입니다: " + row + ", " + col);
        }
        grid[row][col] = BlockType.EMPTY_CODE;
    }

    /** 지정한 열에서 topRow보다 위에 있는 칸들을 한 덩어리로, 아래가 막힐 때까지 내린다. */
    public void dropAbove(int col, int topRow) {
        if (col < 0 || col >= WIDTH || topRow < 0 || topRow > HEIGHT) {
            throw new IllegalArgumentException("보드 범위를 벗어났습니다: 열 " + col + ", 행 " + topRow);
        }
        int gap = 0;
        while (topRow + gap < HEIGHT && grid[topRow + gap][col] == BlockType.EMPTY_CODE) {
            gap++;
        }
        if (gap == 0) {
            return;
        }
        for (int row = topRow - 1; row >= 0; row--) {
            grid[row + gap][col] = grid[row][col];
            grid[row][col] = BlockType.EMPTY_CODE;
        }
    }

    /** 보드를 모두 비운다. */
    public void reset() {
        for (int r = 0; r < HEIGHT; r++) {
            java.util.Arrays.fill(grid[r], BlockType.EMPTY_CODE);
        }
    }

    private boolean isFullLine(int row) {
        for (int c = 0; c < WIDTH; c++) {
            if (grid[row][c] == BlockType.EMPTY_CODE) {
                return false;
            }
        }
        return true;
    }

    /**
     * 가득 찬 줄의 행 번호. 아래쪽 줄부터 담는다.
     *
     * <p>줄 삭제 애니메이션 동안 지울 줄을 표시하기 위해, 삭제와 분리해 찾기만 한다.
     */
    public List<Integer> fullRows() {
        List<Integer> rows = new ArrayList<>();
        for (int row = HEIGHT - 1; row >= 0; row--) {
            if (isFullLine(row)) {
                rows.add(row);
            }
        }
        return rows;
    }

    /**
     * 지정한 줄들을 삭제하고 그 위의 줄들을 삭제한 수만큼 아래로 내린다.
     *
     * <p>가득 찼는지는 확인하지 않는다. 줄 삭제 아이템처럼 꽉 차지 않은 줄을 지우는 경우에도 쓴다.
     * 같은 행이 여러 번 들어 있어도 한 번만 삭제한다.
     *
     * @throws IllegalArgumentException 보드 범위를 벗어난 행이 있는 경우
     */
    public void removeRows(Collection<Integer> rows) {
        boolean[] removed = new boolean[HEIGHT];
        for (int row : rows) {
            if (row < 0 || row >= HEIGHT) {
                throw new IllegalArgumentException("보드 범위를 벗어난 행입니다: " + row);
            }
            removed[row] = true;
        }

        // 아래에서 위로 훑으며 남길 줄만 아래쪽부터 채운다. 남은 위쪽 줄은 빈 줄이 된다.
        int write = HEIGHT - 1;
        for (int read = HEIGHT - 1; read >= 0; read--) {
            if (removed[read]) {
                continue;
            }
            if (write != read) {
                System.arraycopy(grid[read], 0, grid[write], 0, WIDTH);
            }
            write--;
        }
        for (; write >= 0; write--) {
            java.util.Arrays.fill(grid[write], BlockType.EMPTY_CODE);
        }
    }
}
