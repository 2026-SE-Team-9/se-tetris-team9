package seoultech.se.tetris.engine;

import java.util.ArrayList;
import java.util.List;
import seoultech.se.tetris.blocks.Block;
import seoultech.se.tetris.blocks.BlockType;

/**
 * Tall Boy ({@code B}).
 *
 * <p>고정되면 자신 아래로 2칸 파고든 뒤(바닥까지 2칸이 안 되면 갈 수 있는 만큼), 자신을 중심으로
 * 5×5 범위의 보드 안쪽 칸을 지우고 자신도 사라진다. 폭발 범위 위쪽의 블럭은 열마다 한 덩어리로,
 * 아래 블럭이나 바닥에 닿을 때까지 내려온다. 떠 있는 블럭이 남지 않게 하기 위함이다.
 * 점수는 블럭이 있던 칸 수 × 10점에 레벨 배율을 적용하며, 자기 칸을 한 번 포함한다(최대 250점).
 * 줄 삭제로 세지 않는다. (2차 팀 결정)
 */
final class TallBoyEffect implements ItemEffect {

    /** 아래로 파고드는 칸 수 */
    static final int DIG_DEPTH = 2;

    /** 중심에서 폭발 범위 끝까지의 거리. 5×5이므로 2. */
    static final int RADIUS = 2;

    @Override
    public void onLock(LockContext context) {
        int[] self = onlyCell(context.block());
        int centerRow = self[0] + Math.min(DIG_DEPTH, Board.HEIGHT - 1 - self[0]);
        int centerCol = self[1];

        List<int[]> cells = new ArrayList<>();
        int filledExceptCenter = 0;
        for (int row = centerRow - RADIUS; row <= centerRow + RADIUS; row++) {
            for (int col = centerCol - RADIUS; col <= centerCol + RADIUS; col++) {
                if (row < 0 || row >= Board.HEIGHT || col < 0 || col >= Board.WIDTH) {
                    continue;
                }
                if (context.isEmpty(row, col)) {
                    continue;
                }
                cells.add(new int[] {row, col});
                if (row != centerRow || col != centerCol) {
                    filledExceptCenter++;
                }
            }
        }
        // 자기 칸을 한 번 더한다. 파고든 자리에 블럭이 있었어도 그 칸은 자기 칸으로 한 번만 센다
        context.removeCells(cells, filledExceptCenter + 1);
        // 폭발한 열마다 범위 맨 위 행보다 위의 칸을 내린다. 내려온 칸이 줄을 채우면 엔진이 일반 줄 삭제로 처리한다
        int top = Math.max(0, centerRow - RADIUS);
        for (int col = Math.max(0, centerCol - RADIUS); col <= Math.min(Board.WIDTH - 1, centerCol + RADIUS); col++) {
            context.dropAbove(col, top);
        }
    }

    /** 1×1 블럭이 차지한 보드 칸 {행, 열} */
    private static int[] onlyCell(Block block) {
        for (int r = 0; r < BlockType.SIZE; r++) {
            for (int c = 0; c < BlockType.SIZE; c++) {
                if (block.isFilled(r, c)) {
                    return new int[] {block.boardRow(r), block.boardCol(c)};
                }
            }
        }
        throw new IllegalStateException("채워진 칸이 없는 블럭입니다: " + block);
    }
}
