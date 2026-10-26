package seoultech.se.tetris.engine;

/**
 * 점수를 계산한다. 규칙은 다음과 같다.
 *
 * <pre>
 * 낙하 점수    = 1 × 레벨           (블럭이 한 칸 내려갈 때마다, 자동/수동 무관)
 * 줄 삭제 점수 = 기본 배점 × 레벨     (기본 배점: 1줄 100, 2줄 300, 3줄 600, 4줄 1000)
 * 백투백       = 줄 삭제 점수 × 2    (4줄 동시 삭제를 연속 2회 이상 성공한 경우)
 * </pre>
 *
 * <p>레벨 배율은 모든 점수에 적용된다. 백투백 배수는 줄 삭제 점수에만 적용된다.
 *
 * <p>상태를 갖지 않는다. 백투백 여부는 호출하는 쪽이 판단해서 넘긴다.
 * 점수 규칙을 이 클래스에 모아두었으므로, 이후 요구사항에서 배점이 바뀌면 여기만 고치면 된다.
 */
public class ScoreCalculator {

    /* 점수 계산에 사용되는 상수들. 세부사항 변경 시 아래 변수들을 조정하여 변경 가능    */
    /** 블럭이 한 칸 내려갈 때 얻는 기본 점수 */
    public static final int DROP_SCORE_PER_CELL = 1;

    /**
     * 삭제한 줄 수에 따른 기본 배점.
     *
     * <p>인덱스가 줄 수다. 0줄은 0점.
     */
    private static final int[] LINE_SCORES = {0, 100, 300, 600, 1000};

    /** 백투백 성립 시 줄 삭제 점수에 곱하는 배수 */
    public static final int BACK_TO_BACK_MULTIPLIER = 2;

    /** 백투백 판정 대상이 되는 줄 수. 이 수만큼 동시에 지워야 한다. */
    public static final int BACK_TO_BACK_LINES = 4;

    /** 아이템이 지운 칸 하나의 점수 */
    public static final int ITEM_SCORE_PER_CELL = 10;

    /**
     * 블럭이 아래로 내려간 거리에 대한 점수.
     *
     * <p>자동 낙하와 수동 조작을 구분하지 않는다.
     *
     * @param cells 내려간 칸 수
     * @param level 현재 레벨
     */
    public int dropScore(int cells, int level) {
        requireNotNegative(cells, "내려간 칸 수");
        requireValidLevel(level);

        return DROP_SCORE_PER_CELL * cells * level;
    }

    /**
     * 줄을 삭제했을 때의 점수.
     *
     * @param lines      한 번에 삭제한 줄 수 (0 이상 {@value #BACK_TO_BACK_LINES} 이하)
     * @param level      현재 레벨
     * @param backToBack 직전 삭제에도 {@value #BACK_TO_BACK_LINES}줄을 동시에 삭제했고
     *                   이번 삭제에도 그런 경우 {@code true}
     *                   {@value #BACK_TO_BACK_LINES} 미만 줄을 삭제하면 {@code false}
     */
    public int lineClearScore(int lines, int level, boolean backToBack) {
        requireValidLineCount(lines);
        requireValidLevel(level);

        int score = LINE_SCORES[lines] * level;
        if (backToBack && lines == BACK_TO_BACK_LINES) {
            score *= BACK_TO_BACK_MULTIPLIER;
        }
        return score;
    }

    /** 아이템이 칸을 지웠을 때의 점수. 칸당 10점에 레벨 배율을 적용한다. */
    public int itemScore(int cells, int level) {
        requireNotNegative(cells, "칸 수");
        requireValidLevel(level);

        return ITEM_SCORE_PER_CELL * cells * level;
    }

    /** 해당 줄 수가 백투백 판정 대상인지 여부 */
    public boolean isBackToBackEligible(int lines) {
        return lines == BACK_TO_BACK_LINES;
    }

    /** 한 번에 삭제 가능한 최대 줄 수 */
    public int maxLinesAtOnce() {
        return LINE_SCORES.length - 1;
    }

    private static void requireValidLineCount(int lines) {
        if (lines < 0 || lines >= LINE_SCORES.length) {
            throw new IllegalArgumentException(
                    "삭제한 줄 수는 0 이상 " + (LINE_SCORES.length - 1) + " 이하여야 합니다: " + lines);
        }
    }

    private static void requireValidLevel(int level) {
        if (level < LevelPolicy.INITIAL_LEVEL) {
            throw new IllegalArgumentException(
                    "레벨은 " + LevelPolicy.INITIAL_LEVEL + " 이상이어야 합니다: " + level);
        }
    }

    private static void requireNotNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + "는 음수일 수 없습니다: " + value);
        }
    }
}
