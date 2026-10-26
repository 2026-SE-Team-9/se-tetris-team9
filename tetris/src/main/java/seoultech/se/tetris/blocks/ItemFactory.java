package seoultech.se.tetris.blocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * 아이템 모드에서 아이템 블럭을 만든다.
 *
 * <p>아이템 5종 중 하나를 <b>균등한 확률</b>로 고른다. (2차 팀 결정)
 * <ul>
 *   <li>포함형(L, D)이면 주어진 테트로미노의 채워진 4칸 중 한 칸을 무작위로 골라 붙인다.</li>
 *   <li>독립형(A, B, C)이면 테트로미노 대신 아이템 조각을 낸다.</li>
 * </ul>
 *
 * <p>{@link BlockFactory}와 같은 이유로 {@link Random}을 주입받는다.
 * 시드를 고정하면 같은 아이템 순서가 재현되어 테스트에서 활용할 수 있다.
 */
public class ItemFactory {

    private static final ItemType[] ITEMS = ItemType.values();

    private final Random random;

    public ItemFactory(Random random) {
        this.random = Objects.requireNonNull(random, "Random은 null일 수 없습니다.");
    }

    /**
     * 아이템 블럭을 만든다. 회전하지 않은 상태로, 격자의 좌측 상단이 (0, 0)에 놓인다.
     *
     * @param base 포함형 아이템이 붙을 테트로미노. 독립형 아이템이 고르면 쓰지 않는다
     */
    public Block create(BlockType base) {
        Objects.requireNonNull(base, "블럭 종류는 null일 수 없습니다.");
        ItemType item = nextItem();
        if (!item.isEmbedded()) {
            return Block.of(item, 0, 0);
        }
        Block block = Block.of(base, 0, 0);
        int[] cell = randomFilledCell(block);
        return block.withItem(item, cell[0], cell[1]);
    }

    /**
     * 아이템 하나를 균등한 확률로 고른다.
     *
     * <p>요구사항에 정한 확률이므로 실제 게임에서 쓰는 이 메서드를 그대로 테스트한다.
     */
    public ItemType nextItem() {
        return ITEMS[random.nextInt(ITEMS.length)];
    }

    /** 블럭의 채워진 칸 중 하나를 균등한 확률로 고른다. */
    private int[] randomFilledCell(Block block) {
        List<int[]> cells = new ArrayList<>();
        for (int r = 0; r < BlockType.SIZE; r++) {
            for (int c = 0; c < BlockType.SIZE; c++) {
                if (block.isFilled(r, c)) {
                    cells.add(new int[] {r, c});
                }
            }
        }
        return cells.get(random.nextInt(cells.size()));
    }
}
