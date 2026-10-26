package seoultech.se.tetris.blocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import seoultech.se.tetris.engine.Difficulty;

@DisplayName("BlockFactory")
class BlockFactoryTest {

    @Test
    @DisplayName("Random이 null이면 예외를 던진다")
    void throwsOnNullRandom() {
        assertThrows(NullPointerException.class, () -> new BlockFactory(null));
    }

    @Test
    @DisplayName("항상 블럭 종류를 반환한다")
    void alwaysReturnsType() {
        BlockFactory factory = new BlockFactory(new Random(42));

        for (int i = 0; i < 100; i++) {
            assertNotNull(factory.next());
        }
    }

    @Test
    @DisplayName("7종이 모두 등장한다")
    void producesAllTypes() {
        BlockFactory factory = new BlockFactory(new Random(42));
        Set<BlockType> seen = EnumSet.noneOf(BlockType.class);

        for (int i = 0; i < 1000; i++) {
            seen.add(factory.next());
        }

        assertEquals(BlockType.values().length, seen.size(),
                "등장하지 않은 블럭이 있습니다: " + EnumSet.complementOf(EnumSet.copyOf(seen)));
    }

    @Test
    @DisplayName("7종이 동일한 확률로 등장한다")
    void producesUniformDistribution() {
        BlockFactory factory = new BlockFactory(new Random(42));
        Map<BlockType, Integer> counts = new EnumMap<>(BlockType.class);

        int trials = 70_000;
        for (int i = 0; i < trials; i++) {
            counts.merge(factory.next(), 1, Integer::sum);
        }

        int expected = trials / BlockType.values().length;   // 10,000
        int tolerance = (int) (expected * 0.05);             // ±5%

        for (BlockType type : BlockType.values()) {
            int count = counts.getOrDefault(type, 0);
            assertTrue(Math.abs(count - expected) <= tolerance,
                    type + " 블럭이 " + count + "번 등장했습니다. "
                            + "기대값 " + expected + " ± " + tolerance + " 범위를 벗어납니다.");
        }
    }

    @Test
    @DisplayName("같은 시드면 같은 순서로 나온다")
    void sameSeedProducesSameSequence() {
        BlockFactory first = new BlockFactory(new Random(2026));
        BlockFactory second = new BlockFactory(new Random(2026));

        for (int i = 0; i < 100; i++) {
            assertEquals(first.next(), second.next());
        }
    }

    // ------------------------------------------------------------------
    // 난이도별 확률 (2차 요구사항)
    // ------------------------------------------------------------------

    /** 시행 횟수. 상대 오차 ±5%를 사실상 어떤 시드로도 통과하도록 10만 회로 정했다. (2차 요구사항 정리) */
    private static final int TRIALS = 100_000;

    /** 허용하는 상대 오차. 기대 확률의 5%다. */
    private static final double RELATIVE_TOLERANCE = 0.05;

    /**
     * 요구사항에 적힌 I형 블럭의 가중치. 다른 블럭은 모두 10이다.
     *
     * <p>생성기의 값을 가져오지 않고 요구사항 수치를 직접 적는다.
     * 생성기 쪽 수치가 틀렸을 때 테스트도 같이 틀려서 통과하는 일을 막기 위함이다.
     */
    private static int specIWeight(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> 12;
            case NORMAL -> 10;
            case HARD -> 8;
        };
    }

    /** 요구사항 가중치로 계산한 기대 확률. */
    private static double expectedProbability(BlockType type, Difficulty difficulty) {
        int iWeight = specIWeight(difficulty);
        int total = iWeight + 10 * (BlockType.values().length - 1);
        return (type == BlockType.I ? iWeight : 10) / (double) total;
    }

    /** 실제 게임 생성기로 {@link #TRIALS}번 뽑아 모든 블럭이 상대 오차 안에 드는지 검증한다. */
    private static void assertDistribution(Difficulty difficulty, long seed) {
        BlockFactory factory = new BlockFactory(new Random(seed), difficulty);
        Map<BlockType, Integer> counts = new EnumMap<>(BlockType.class);
        for (int i = 0; i < TRIALS; i++) {
            counts.merge(factory.next(), 1, Integer::sum);
        }

        for (BlockType type : BlockType.values()) {
            double expected = expectedProbability(type, difficulty);
            double actual = counts.getOrDefault(type, 0) / (double) TRIALS;
            double relativeError = Math.abs(actual - expected) / expected;
            assertTrue(relativeError <= RELATIVE_TOLERANCE,
                    String.format("%s 난이도에서 %s 블럭의 확률이 %.4f입니다. 기대값 %.4f의 상대 오차 %.1f%%를 벗어납니다.",
                            difficulty, type, actual, expected, relativeError * 100));
        }
    }

    @Nested
    @DisplayName("난이도별 확률 (시행 10만 회, 상대 오차 ±5%)")
    class DifficultyDistribution {

        @Test
        @DisplayName("쉬움: I형 12/72, 나머지 각 10/72")
        void easy() {
            assertDistribution(Difficulty.EASY, 42);
        }

        @Test
        @DisplayName("중간: 7종 모두 10/70")
        void normal() {
            assertDistribution(Difficulty.NORMAL, 42);
        }

        @Test
        @DisplayName("어려움: I형 8/68, 나머지 각 10/68")
        void hard() {
            assertDistribution(Difficulty.HARD, 42);
        }

        @Test
        @DisplayName("다른 시드로도 통과한다")
        void passesWithOtherSeeds() {
            for (long seed : new long[] {1, 2026, 123_456_789}) {
                for (Difficulty difficulty : Difficulty.values()) {
                    assertDistribution(difficulty, seed);
                }
            }
        }
    }

    @Nested
    @DisplayName("난이도별 가중치")
    class Weights {

        @Test
        @DisplayName("I형 블럭의 가중치는 쉬움 12, 중간 10, 어려움 8이다")
        void iWeightByDifficulty() {
            for (Difficulty difficulty : Difficulty.values()) {
                BlockFactory factory = new BlockFactory(new Random(0), difficulty);
                assertEquals(specIWeight(difficulty), factory.weightOf(BlockType.I), difficulty.toString());
            }
        }

        @Test
        @DisplayName("I형이 아닌 블럭의 가중치는 난이도와 관계없이 10이다")
        void otherWeightsAreTen() {
            for (Difficulty difficulty : Difficulty.values()) {
                BlockFactory factory = new BlockFactory(new Random(0), difficulty);
                for (BlockType type : BlockType.values()) {
                    if (type != BlockType.I) {
                        assertEquals(10, factory.weightOf(type), difficulty + " " + type);
                    }
                }
            }
        }

        @Test
        @DisplayName("난이도를 지정하지 않으면 중간 난이도다")
        void defaultsToNormal() {
            assertEquals(specIWeight(Difficulty.NORMAL), new BlockFactory(new Random(0)).weightOf(BlockType.I));
        }

        @Test
        @DisplayName("난이도가 null이면 예외를 던진다")
        void throwsOnNullDifficulty() {
            assertThrows(NullPointerException.class, () -> new BlockFactory(new Random(0), null));
        }
    }
}
