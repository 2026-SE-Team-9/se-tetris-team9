package seoultech.se.tetris.engine;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import seoultech.se.tetris.blocks.Block;

/**
 * 블럭을 고정할 때 아이템 효과가 쓰는 게임 문맥 객체. (2차 팀 결정)
 *
 * <p>효과는 보드를 직접 고치지 않고 이 객체를 통해 보드를 읽고, 지울 칸을 {@link ClearPlan}에 기록한다.
 * 블럭을 놓는 것만 바로 반영한다. 놓인 블럭도 애니메이션에서 지워질 칸과 함께 보여야 하기 때문이다.
 */
final class LockContext {

    private final Board board;
    private final Block block;
    private final ClearPlan plan = new ClearPlan();

    LockContext(Board board, Block block) {
        this.board = Objects.requireNonNull(board, "Board는 null일 수 없습니다.");
        this.block = Objects.requireNonNull(block, "블럭은 null일 수 없습니다.");
    }

    /** 고정되는 블럭. 더 내려갈 수 없는 위치에 있다. */
    Block block() {
        return block;
    }

    /** 지정한 칸이 비어 있는지. 보드 밖이면 비어 있지 않은 것으로 본다. */
    boolean isEmpty(int row, int col) {
        return board.isEmpty(row, col);
    }

    /** 현재 보드의 가득 찬 줄. 아래쪽부터. */
    List<Integer> fullRows() {
        return board.fullRows();
    }

    /** 고정되는 블럭을 지금 위치에 놓는다. */
    void placeBlock() {
        board.place(block);
    }

    /** 지정한 줄을 줄 삭제로 지운다. 점수, 10줄 카운트, 백투백에 반영된다. 비어 있으면 아무 일도 하지 않는다. */
    void clearLines(Collection<Integer> rows) {
        if (!rows.isEmpty()) {
            plan.setLineClear(board, rows);
        }
    }

    /**
     * 지정한 칸을 지운다. 위의 칸은 내려오지 않는다. 줄 삭제로 세지 않는다.
     *
     * @param cells      지울 칸 {행, 열}
     * @param scoreCells 아이템 점수(칸당 10점, 레벨 배율)를 줄 칸 수
     */
    void removeCells(Collection<int[]> cells, int scoreCells) {
        plan.addRemovedCells(cells, scoreCells);
    }

    /** 칸을 지운 뒤 해당 열에서 {@code topRow}보다 위의 칸들을 아래가 막힐 때까지 내린다. */
    void dropAbove(int col, int topRow) {
        plan.addDropColumn(col, topRow);
    }

    /** 지정한 줄을 통째로 지우고 위를 내린다. 줄 삭제로 세지 않고 점수도 없다. */
    void crushRows(Collection<Integer> rows) {
        plan.addCrushedRows(rows);
    }

    /** 삭제를 마친 뒤 보드에 놓을 블럭을 정한다. */
    void placeAfterRemoval(Block after) {
        plan.setPlaceAfterRemoval(after);
    }

    ClearPlan plan() {
        return plan;
    }
}
