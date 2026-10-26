package seoultech.se.tetris.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ScoreCalculator")
class ScoreCalculatorTest {

    private ScoreCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new ScoreCalculator();
    }

    @Nested
    @DisplayName("dropScore()")
    class DropScore {

        @Test
        @DisplayName("레벨 1에서는 한 칸당 1점이다")
        void onePointPerCellAtInitialLevel() {
            assertEquals(1, calculator.dropScore(1, 1));
            assertEquals(5, calculator.dropScore(5, 1));
        }

        @Test
        @DisplayName("레벨 배율이 적용된다")
        void appliesLevelMultiplier() {
            assertEquals(3, calculator.dropScore(1, 3));
            assertEquals(15, calculator.dropScore(5, 3));
        }

        @Test
        @DisplayName("내려가지 않았으면 0점이다")
        void zeroWhenNoDrop() {
            assertEquals(0, calculator.dropScore(0, 5));
        }

        @Test
        @DisplayName("음수 칸 수면 예외를 던진다")
        void throwsOnNegativeCells() {
            assertThrows(IllegalArgumentException.class, () -> calculator.dropScore(-1, 1));
        }

        @Test
        @DisplayName("레벨이 1 미만이면 예외를 던진다")
        void throwsOnInvalidLevel() {
            assertThrows(IllegalArgumentException.class, () -> calculator.dropScore(1, 0));
        }
    }

    @Nested
    @DisplayName("lineClearScore()")
    class LineClearScore {

        @Test
        @DisplayName("레벨 1에서의 기본 배점")
        void baseScoresAtInitialLevel() {
            assertEquals(100, calculator.lineClearScore(1, 1, false));
            assertEquals(300, calculator.lineClearScore(2, 1, false));
            assertEquals(600, calculator.lineClearScore(3, 1, false));
            assertEquals(1000, calculator.lineClearScore(4, 1, false));
        }

        @Test
        @DisplayName("지운 줄이 없으면 0점이다")
        void zeroWhenNoLineCleared() {
            assertEquals(0, calculator.lineClearScore(0, 5, false));
        }

        @Test
        @DisplayName("레벨 배율이 적용된다")
        void appliesLevelMultiplier() {
            assertEquals(300, calculator.lineClearScore(1, 3, false));
            assertEquals(3000, calculator.lineClearScore(4, 3, false));
        }

        @Test
        @DisplayName("줄 수가 많을수록 한 줄당 점수가 높다")
        void moreLinesGiveBetterRate() {
            // 한 번에 많이 지우는 것이 유리해야 한다
            assertTrue(calculator.lineClearScore(4, 1, false)
                    > calculator.lineClearScore(1, 1, false) * 4);
            assertTrue(calculator.lineClearScore(2, 1, false)
                    > calculator.lineClearScore(1, 1, false) * 2);
        }
    }

    @Nested
    @DisplayName("백투백")
    class BackToBack {

        @Test
        @DisplayName("4줄 삭제가 연속되면 2배가 된다")
        void doublesOnConsecutiveFourLines() {
            int normal = calculator.lineClearScore(4, 1, false);
            int doubled = calculator.lineClearScore(4, 1, true);

            assertEquals(normal * 2, doubled);
        }

        @Test
        @DisplayName("레벨 배율과 함께 적용된다")
        void stacksWithLevelMultiplier() {
            // 1000 × 레벨 3 × 2배
            assertEquals(6000, calculator.lineClearScore(4, 3, true));
        }

        @Test
        @DisplayName("4줄이 아니면 배수가 적용되지 않는다")
        void onlyAppliesToFourLines() {
            assertEquals(calculator.lineClearScore(1, 1, false),
                    calculator.lineClearScore(1, 1, true));
            assertEquals(calculator.lineClearScore(3, 1, false),
                    calculator.lineClearScore(3, 1, true));
        }

        @Test
        @DisplayName("4줄만 백투백 판정 대상이다")
        void onlyFourLinesAreEligible() {
            assertFalse(calculator.isBackToBackEligible(0));
            assertFalse(calculator.isBackToBackEligible(1));
            assertFalse(calculator.isBackToBackEligible(3));
            assertTrue(calculator.isBackToBackEligible(4));
        }
    }

    @Nested
    @DisplayName("입력 검증")
    class Validation {

        @Test
        @DisplayName("지울 수 있는 최대 줄 수는 4다")
        void maxLinesIsFour() {
            assertEquals(4, calculator.maxLinesAtOnce());
        }

        @Test
        @DisplayName("줄 수가 범위를 벗어나면 예외를 던진다")
        void throwsOnInvalidLineCount() {
            assertThrows(IllegalArgumentException.class,
                    () -> calculator.lineClearScore(-1, 1, false));
            assertThrows(IllegalArgumentException.class,
                    () -> calculator.lineClearScore(5, 1, false));
        }

        @Test
        @DisplayName("레벨이 1 미만이면 예외를 던진다")
        void throwsOnInvalidLevel() {
            assertThrows(IllegalArgumentException.class,
                    () -> calculator.lineClearScore(1, 0, false));
        }
    }

    @Nested
    @DisplayName("itemScore()")
    class ItemScore {

        @Test
        @DisplayName("칸당 10점에 레벨 배율을 적용한다")
        void tenPointsPerCellTimesLevel() {
            assertEquals(40, calculator.itemScore(4, 1));
            assertEquals(250, calculator.itemScore(25, 1));
            assertEquals(120, calculator.itemScore(4, 3));
        }

        @Test
        @DisplayName("0칸이면 0점이다")
        void zeroCells() {
            assertEquals(0, calculator.itemScore(0, 5));
        }

        @Test
        @DisplayName("음수 칸이나 잘못된 레벨이면 예외를 던진다")
        void rejectsInvalidArguments() {
            assertThrows(IllegalArgumentException.class, () -> calculator.itemScore(-1, 1));
            assertThrows(IllegalArgumentException.class, () -> calculator.itemScore(1, 0));
        }
    }
}
