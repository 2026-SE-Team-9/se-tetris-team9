package seoultech.se.tetris.engine;

import java.util.ArrayList;
import java.util.List;
import seoultech.se.tetris.blocks.Block;
import seoultech.se.tetris.blocks.BlockType;

/**
 * Crusher ({@code C}).
 *
 * <p>고정되면 자신보다 아래에 있는 줄을 보드 맨 아래부터 최대 3줄까지 통째로 지우고,
 * 지운 줄 수만큼 위의 줄들(자신 포함)이 내려온다. 자신은 보드에 남는다.
 * 점수는 없고 줄 삭제로 세지 않는다. (2차 팀 결정)
 */
final class CrusherEffect implements ItemEffect {

    /** 한 번에 짓누르는 최대 줄 수 */
    static final int MAX_ROWS = 3;

    @Override
    public void onLock(LockContext context) {
        context.placeBlock();
        int below = Board.HEIGHT - 1 - lowestRow(context.block());
        List<Integer> rows = new ArrayList<>();
        for (int i = 0; i < Math.min(MAX_ROWS, below); i++) {
            rows.add(Board.HEIGHT - 1 - i);
        }
        context.crushRows(rows);
    }

    private static int lowestRow(Block block) {
        int lowest = -1;
        for (int r = 0; r < BlockType.SIZE; r++) {
            for (int c = 0; c < BlockType.SIZE; c++) {
                if (block.isFilled(r, c)) {
                    lowest = Math.max(lowest, block.boardRow(r));
                }
            }
        }
        return lowest;
    }
}
