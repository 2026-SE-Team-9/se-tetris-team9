package seoultech.se.tetris.blocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ItemFactory")
class ItemFactoryTest {

    /** 시행 횟수와 허용 상대 오차. 블럭 생성 확률 테스트와 같은 기준이다. (2차 요구사항 정리) */
    private static final int TRIALS = 100_000;
    private static final double RELATIVE_TOLERANCE = 0.05;

    /** 고르는 아이템만 고정하고, 붙는 칸은 실제 코드대로 무작위로 고르는 생성기. */
    private static ItemFactory always(ItemType item, long seed) {
        return new ItemFactory(new Random(seed)) {
            @Override
            public ItemType nextItem() {
                return item;
            }
        };
    }

    private static void assertWithinTolerance(String what, int count, double expected) {
        double actual = count / (double) TRIALS;
        double relativeError = Math.abs(actual - expected) / expected;
        assertTrue(relativeError <= RELATIVE_TOLERANCE,
                String.format("%s의 확률이 %.4f입니다. 기대값 %.4f의 상대 오차 %.1f%%를 벗어납니다.",
                        what, actual, expected, relativeError * 100));
    }

    @Test
    @DisplayName("Random이 null이면 예외를 던진다")
    void throwsOnNullRandom() {
        assertThrows(NullPointerException.class, () -> new ItemFactory(null));
    }

    @Test
    @DisplayName("블럭 종류가 null이면 예외를 던진다")
    void throwsOnNullBase() {
        assertThrows(NullPointerException.class, () -> new ItemFactory(new Random(42)).create(null));
    }

    @Test
    @DisplayName("같은 시드면 같은 아이템 블럭이 나온다")
    void sameSeedProducesSameBlocks() {
        ItemFactory first = new ItemFactory(new Random(7));
        ItemFactory second = new ItemFactory(new Random(7));

        for (int i = 0; i < 50; i++) {
            assertEquals(first.create(BlockType.T), second.create(BlockType.T));
        }
    }

    @Nested
    @DisplayName("확률 (시행 10만 회, 상대 오차 ±5%)")
    class Distribution {

        @Test
        @DisplayName("아이템 5종이 균등한 확률로 나온다")
        void itemsAreUniform() {
            ItemFactory factory = new ItemFactory(new Random(42));
            Map<ItemType, Integer> counts = new EnumMap<>(ItemType.class);
            for (int i = 0; i < TRIALS; i++) {
                counts.merge(factory.nextItem(), 1, Integer::sum);
            }

            double expected = 1.0 / ItemType.values().length;
            for (ItemType item : ItemType.values()) {
                assertWithinTolerance(item.toString(), counts.getOrDefault(item, 0), expected);
            }
        }

        @Test
        @DisplayName("포함형 아이템은 블럭의 4칸에 균등한 확률로 붙는다")
        void embeddedCellIsUniform() {
            ItemFactory factory = always(ItemType.LINE_CLEAR, 42);
            Map<String, Integer> counts = new HashMap<>();
            for (int i = 0; i < TRIALS; i++) {
                Block block = factory.create(BlockType.I);
                counts.merge(block.itemRow() + "," + block.itemCol(), 1, Integer::sum);
            }

            // 가로 I는 격자 1행의 0~3열을 차지한다
            assertEquals(4, counts.size());
            for (int c = 0; c < 4; c++) {
                assertWithinTolerance("I형 (1, " + c + ") 칸", counts.getOrDefault("1," + c, 0), 0.25);
            }
        }
    }

    @Nested
    @DisplayName("만드는 블럭")
    class Created {

        @Test
        @DisplayName("포함형 아이템은 주어진 블럭에 붙는다")
        void embeddedAttachesToBase() {
            Block block = always(ItemType.DOUBLE_OR_NOTHING, 1).create(BlockType.S);

            assertSame(BlockType.S, block.shape());
            assertSame(ItemType.DOUBLE_OR_NOTHING, block.item());
            assertEquals(ItemType.DOUBLE_OR_NOTHING.code(), block.codeAt(block.itemRow(), block.itemCol()));
        }

        @Test
        @DisplayName("독립형 아이템은 주어진 블럭 대신 아이템 조각이 된다")
        void standaloneReplacesBase() {
            for (ItemType item : new ItemType[] {ItemType.WEIGHT, ItemType.TALL_BOY, ItemType.CRUSHER}) {
                Block block = always(item, 1).create(BlockType.S);

                assertSame(item, block.shape());
                assertSame(item, block.item());
            }
        }

        @Test
        @DisplayName("회전하지 않은 상태로 격자의 좌측 상단이 (0, 0)에 놓인다")
        void createdAtOrigin() {
            ItemFactory factory = new ItemFactory(new Random(3));
            for (int i = 0; i < 20; i++) {
                Block block = factory.create(BlockType.T);

                assertNotNull(block.item());
                assertEquals(0, block.rotation());
                assertEquals(0, block.row());
                assertEquals(0, block.col());
            }
        }
    }
}
