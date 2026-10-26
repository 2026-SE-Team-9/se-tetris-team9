package seoultech.se.tetris.engine;

import java.util.ArrayList;
import java.util.List;
import seoultech.se.tetris.blocks.Block;
import seoultech.se.tetris.blocks.BlockType;

/**
 * 무게추 ({@code A}).
 *
 * <p>바닥이나 블럭에 닿으면 좌우로 움직일 수 없다. 고정되면 자기 아래에 있는 칸을 바닥까지 모두 지우고,
 * 바닥까지 내려가 보드에 남는다. 점수는 지운 칸 수 × 10점에 레벨 배율을 적용한다. 줄 삭제로 세지 않는다.
 * (2차 팀 결정)
 */
final class WeightEffect implements ItemEffect {

    @Override
    public boolean canSlideWhenLanded() {
        return false;
    }

    @Override
    public void onLock(LockContext context) {
        Block weight = context.block();
        List<int[]> cells = new ArrayList<>();
        int lowestRow = -1;

        for (int c = 0; c < BlockType.SIZE; c++) {
            int bottom = bottomFilledRow(weight, c);
            if (bottom < 0) {
                continue;
            }
            int boardCol = weight.boardCol(c);
            int boardBottom = weight.boardRow(bottom);
            lowestRow = Math.max(lowestRow, boardBottom);
            for (int row = boardBottom + 1; row < Board.HEIGHT; row++) {
                if (!context.isEmpty(row, boardCol)) {
                    cells.add(new int[] {row, boardCol});
                }
            }
        }

        context.removeCells(cells, cells.size());
        context.placeAfterRemoval(weight.moved(Board.HEIGHT - 1 - lowestRow, 0));
    }

    /** 격자의 해당 열에서 가장 아래 채워진 행. 없으면 -1. */
    private static int bottomFilledRow(Block block, int col) {
        for (int r = BlockType.SIZE - 1; r >= 0; r--) {
            if (block.isFilled(r, col)) {
                return r;
            }
        }
        return -1;
    }
}
