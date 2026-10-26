package seoultech.se.tetris.engine;

import java.util.ArrayList;
import java.util.List;
import seoultech.se.tetris.blocks.Block;

/**
 * 줄 삭제 아이템 ({@code L}).
 *
 * <p>블럭이 고정되면 꽉 찼는지와 관계없이 {@code L}이 있는 줄을 지운다.
 * 그 줄이 동시에 꽉 찬 줄이면 한 번만 지운다.
 * 점수는 L 줄과 꽉 찬 줄을 합친 총 줄 수로 계산하며, 10줄 카운트에 포함된다. (2차 팀 결정)
 */
final class LineClearEffect implements ItemEffect {

    @Override
    public void onLock(LockContext context) {
        context.placeBlock();
        Block block = context.block();
        List<Integer> rows = new ArrayList<>(context.fullRows());
        rows.add(block.boardRow(block.itemRow()));   // 겹치면 계획에서 한 번만 남는다
        context.clearLines(rows);
    }
}
