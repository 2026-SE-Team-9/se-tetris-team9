package seoultech.se.tetris.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;
import seoultech.se.tetris.blocks.Block;
import seoultech.se.tetris.blocks.CellCode;
import seoultech.se.tetris.blocks.ItemType;

/**
 * 블럭을 고정한 뒤 애니메이션이 끝나면 할 일의 계획.
 *
 * <p>계획을 먼저 세우고(지울 칸 표시) 애니메이션 뒤에 적용하므로, 일반 줄 삭제와 아이템 효과가
 * 모두 같은 흐름을 탄다. 계획에는 두 종류의 삭제가 있다.
 * <ul>
 *   <li><b>줄 삭제</b>: 점수, 10줄 카운트, 백투백에 반영된다. 일반 줄 삭제와 줄 삭제 아이템(L)</li>
 *   <li><b>아이템 효과</b>: 반영되지 않는다. 무게추·Tall Boy의 칸 제거(Tall Boy는 위 칸 내리기 포함),
 *       Crusher의 줄 짓누르기</li>
 * </ul>
 */
final class ClearPlan {

    /** 줄 삭제로 지울 줄. 아래쪽부터. */
    private final List<Integer> lineRows = new ArrayList<>();

    /** 줄 삭제 점수에 곱할 배수. Double or Nothing이 2로 만든다. */
    private int lineScoreMultiplier = 1;

    /** 아이템 효과로 통째로 지우고 위를 내릴 줄. 줄 삭제로 세지 않는다. */
    private final List<Integer> crushedRows = new ArrayList<>();

    /** 아이템 효과로 지울 칸 {행, 열}. 위의 칸은 내려오지 않는다. */
    private final List<int[]> removedCells = new ArrayList<>();

    /** 칸 제거 뒤 위의 칸을 내릴 열 {열, 제거 범위의 맨 위 행}. */
    private final List<int[]> dropColumns = new ArrayList<>();

    /** 아이템 점수를 계산할 칸 수 */
    private int itemScoreCells;

    /** 삭제를 마친 뒤 보드에 놓을 블럭. 무게추가 바닥에 내려앉을 때 쓴다. */
    private Block placeAfterRemoval;

    /**
     * 줄 삭제 계획을 정한다.
     *
     * <p>지울 줄 중 <b>가득 찬 줄</b>에 있는 아이템 칸의 배수를 계산해 둔다. 가득 차지 않은 줄
     * (줄 삭제 아이템으로 지우는 줄)의 아이템은 배수를 주지 않는다. 계획은 애니메이션 전에 세우므로
     * 보드가 바뀌기 전의 상태로 판단한다.
     */
    void setLineClear(Board board, Collection<Integer> rows) {
        lineRows.clear();
        lineRows.addAll(new TreeSet<>(rows).descendingSet());
        lineScoreMultiplier = 1;
        List<Integer> full = board.fullRows();
        for (int row : lineRows) {
            if (!full.contains(row)) {
                continue;
            }
            for (int col = 0; col < Board.WIDTH; col++) {
                ItemType item = CellCode.itemOf(board.codeAt(row, col));
                if (item != null) {
                    lineScoreMultiplier = Math.max(lineScoreMultiplier,
                            ItemEffects.of(item).lineClearMultiplier());
                }
            }
        }
    }

    void addRemovedCells(Collection<int[]> cells, int scoreCells) {
        removedCells.addAll(cells);
        itemScoreCells += scoreCells;
    }

    void addDropColumn(int col, int topRow) {
        dropColumns.add(new int[] {col, topRow});
    }

    List<int[]> dropColumns() {
        return dropColumns;
    }

    void addCrushedRows(Collection<Integer> rows) {
        crushedRows.addAll(rows);
    }

    void setPlaceAfterRemoval(Block block) {
        placeAfterRemoval = block;
    }

    /** 줄 삭제 계획인지 여부 */
    boolean isLineClear() {
        return !lineRows.isEmpty();
    }

    /** 지울 칸이 있어 애니메이션이 필요한지 여부 */
    boolean hasRemoval() {
        return !lineRows.isEmpty() || !crushedRows.isEmpty() || !removedCells.isEmpty();
    }

    List<Integer> lineRows() {
        return lineRows;
    }

    int lineScoreMultiplier() {
        return lineScoreMultiplier;
    }

    List<Integer> crushedRows() {
        return crushedRows;
    }

    List<int[]> removedCells() {
        return removedCells;
    }

    int itemScoreCells() {
        return itemScoreCells;
    }

    Block placeAfterRemoval() {
        return placeAfterRemoval;
    }

    /** 애니메이션 동안 표시할, 지워질 칸. 보드 전체 크기다. */
    boolean[][] cellsToClear() {
        boolean[][] cells = new boolean[Board.HEIGHT][Board.WIDTH];
        for (int row : lineRows) {
            java.util.Arrays.fill(cells[row], true);
        }
        for (int row : crushedRows) {
            java.util.Arrays.fill(cells[row], true);
        }
        for (int[] cell : removedCells) {
            cells[cell[0]][cell[1]] = true;
        }
        return cells;
    }
}
