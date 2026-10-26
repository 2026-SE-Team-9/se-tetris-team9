package seoultech.se.tetris.blocks;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import seoultech.se.tetris.engine.Difficulty;

/**
 * 다음에 등장할 블럭 종류를 무작위로 결정한다.
 *
 * <p>블럭마다 등장 확률에 가중치를 두어 난이도별로 I형 블럭의 확률을 다르게 한다.
 * Roulette Wheel Selection을 바탕으로 아래의 확률로 동작한다.
 *
 * <pre>
 * 난이도   I형 가중치   I형 확률
 * 쉬움        12        12/72 ≈ 16.7%
 * 중간        10        10/70 ≈ 14.3%   (1차 요구사항의 "동일한 확률")
 * 어려움       8         8/68 ≈ 11.8%
 * </pre>
 *
 * <p>{@link Random}을 생성자로 주입받는다. 내부에서 직접 만들면 결과를 재현할 수 없어
 * 분포를 검증하는 테스트를 작성할 수 없기 때문이다. 테스트에서는 시드를 고정한
 * {@code new Random(42)}를 넘기고, 실제 게임에서는 {@code new Random()}을 넘긴다.
 */
public class BlockFactory {

    /** 기본 가중치 */
    private static final int BASE_WEIGHT = 10;

    private static final BlockType[] TYPES = BlockType.values();

    private final Random random;

    /** 생성 시 지정한 난이도 */
    private final Difficulty difficulty;

    /** 블럭 종류별 가중치. 생성 시 난이도에 따라 결정한다. */
    private final Map<BlockType, Integer> weights = new EnumMap<>(BlockType.class);

    /** 가중치 총합. Roulette Wheel Selection의 전체 길이가 된다. */
    private final int totalWeight;

    /**
     * Random만 주입받는 경우 중간 난이도로 설정한다.
     * 테스트 검증을 위해 {@link Random}을 생성자로 따로 주입받는다.
     */
    public BlockFactory(Random random) {
        this(random, Difficulty.NORMAL);
    }

    /** 지정된 난이도에 따라 가중치를 설정한다. */
    public BlockFactory(Random random, Difficulty difficulty) {
        this.random = Objects.requireNonNull(random, "Random은 null일 수 없습니다.");
        this.difficulty = Objects.requireNonNull(difficulty, "난이도는 null일 수 없습니다.");

        int total = 0;
        for (BlockType type : TYPES) {
            int weight = weightOf(type, difficulty);
            weights.put(type, weight);
            total += weight;
        }

        this.totalWeight = total;
    }

    /**
     * 난이도에 따라 블럭의 등장 확률을 조절한다.
     * 특히 요구사항에 명시되어 있는 부분이므로 확실한 검증을 요한다.
     */
    public BlockType next() {
        int point = random.nextInt(totalWeight);

        // 마지막 블럭 앞까지 구간을 확인한다. 어디에도 속하지 않았다면 남은 구간은 마지막 블럭의 것이다
        for (int i = 0; i < TYPES.length - 1; i++) {
            point -= weights.get(TYPES[i]);
            if (point < 0) {
                return TYPES[i];
            }
        }
        return TYPES[TYPES.length - 1];
    }

    /** 생성할 때 지정한 난이도 반환 */
    public Difficulty difficulty() {
        return difficulty;
    }

    /** 이 생성기에서 해당 블럭의 가중치. */
    int weightOf(BlockType type) {
        return weights.get(type);
    }

    /**
     * 난이도에 따른 블럭의 가중치
     *
     * <p>I형 블럭은 쉬움에서 20% 더(12), 어려움에서 20% 덜(8) 등장한다.
     */
    private static int weightOf(BlockType type, Difficulty difficulty) {
        if (type != BlockType.I) {
            return BASE_WEIGHT;
        }
        return switch (difficulty) {
            case EASY -> 12;
            case NORMAL -> BASE_WEIGHT;
            case HARD -> 8;
        };
    }
}
