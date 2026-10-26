package seoultech.se.tetris.blocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("BlockType")
class BlockTypeTest {

    /** 지정한 회전 상태에서 채워진 칸의 수를 센다. */
    private static int countFilled(BlockType type, int rotation) {
        int count = 0;
        for (int r = 0; r < BlockType.SIZE; r++) {
            for (int c = 0; c < BlockType.SIZE; c++) {
                if (type.isFilled(rotation, r, c)) {
                    count++;
                }
            }
        }
        return count;
    }

    // ------------------------------------------------------------------
    // 모양 데이터
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("모양 데이터")
    class Shapes {

        @Test
        @DisplayName("모든 블럭의 모든 회전 상태는 정확히 4칸을 차지한다")
        void everyRotationHasFourCells() {
            for (BlockType type : BlockType.values()) {
                for (int rotation = 0; rotation < BlockType.ROTATIONS; rotation++) {
                    assertEquals(4, countFilled(type, rotation),
                            type + " 블럭의 회전 상태 " + rotation + "의 칸 수가 4가 아닙니다.");
                }
            }
        }

        @Test
        @DisplayName("O 블럭은 회전해도 모양이 같다")
        void oBlockDoesNotChangeOnRotation() {
            for (int rotation = 1; rotation < BlockType.ROTATIONS; rotation++) {
                for (int r = 0; r < BlockType.SIZE; r++) {
                    for (int c = 0; c < BlockType.SIZE; c++) {
                        assertEquals(
                                BlockType.O.isFilled(0, r, c),
                                BlockType.O.isFilled(rotation, r, c),
                                "O 블럭의 회전 상태 " + rotation + "이 상태 0과 다릅니다.");
                    }
                }
            }
        }

        @Test
        @DisplayName("I 블럭은 가로와 세로 모양이 서로 다르다")
        void iBlockChangesOnRotation() {
            // 모양 데이터를 복사해 붙여넣다 생기는 실수를 잡기 위한 확인
            assertFalse(java.util.Arrays.deepEquals(
                    BlockType.I.shape(0), BlockType.I.shape(1)));
        }
    }

    // ------------------------------------------------------------------
    // 표시 문자
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("symbol()")
    class Symbol {

        @Test
        @DisplayName("블럭마다 서로 다른 문자를 가진다")
        void everySymbolIsDistinct() {
            Set<Character> seen = new HashSet<>();
            for (BlockType type : BlockType.values()) {
                assertTrue(seen.add(type.symbol()),
                        "표시 문자가 중복됩니다: " + type.symbol());
            }
        }

        @Test
        @DisplayName("빈 칸 표시와 겹치지 않는다")
        void symbolIsNotDot() {
            for (BlockType type : BlockType.values()) {
                assertNotEquals('.', type.symbol());
            }
        }

        @Test
        @DisplayName("O형은 O, L형은 V로 표시한다 (2차 요구사항 정리)")
        void usesSymbolsDecidedForSecondRequirements() {
            assertEquals('O', BlockType.O.symbol());
            assertEquals('V', BlockType.L.symbol());
        }

        @Test
        @DisplayName("아이템과 고스트 블럭의 문자와 겹치지 않는다")
        void doesNotUseReservedSymbols() {
            // 아이템: L(줄 삭제), A(무게추), D(Double or Nothing), B(Tall Boy), C(Crusher) / 고스트: #
            String reserved = "LADBC#";
            for (BlockType type : BlockType.values()) {
                assertTrue(reserved.indexOf(type.symbol()) < 0,
                        type + "의 표시 문자가 예약된 문자와 겹칩니다: " + type.symbol());
            }
        }
    }

    // ------------------------------------------------------------------
    // 회전 상태 정규화
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("normalize()")
    class Normalize {

        @Test
        @DisplayName("범위 안의 값은 그대로 둔다")
        void keepsValueInRange() {
            assertEquals(0, BlockType.normalize(0));
            assertEquals(3, BlockType.normalize(3));
        }

        @Test
        @DisplayName("범위를 넘으면 순환한다")
        void wrapsAround() {
            assertEquals(0, BlockType.normalize(4));
            assertEquals(1, BlockType.normalize(5));
        }

        @Test
        @DisplayName("음수도 순환한다")
        void wrapsNegative() {
            // 반시계 회전을 위해 필요하다. Math.floorMod를 쓰지 않으면 -1이 나온다.
            assertEquals(3, BlockType.normalize(-1));
            assertEquals(0, BlockType.normalize(-4));
        }
    }

    // ------------------------------------------------------------------
    // 번호 변환
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("code() / fromCode()")
    class Code {

        @Test
        @DisplayName("빈 칸 번호와 겹치지 않는다")
        void codeIsNotEmptyCode() {
            for (BlockType type : BlockType.values()) {
                assertNotEquals(BlockType.EMPTY_CODE, type.code());
            }
        }

        @Test
        @DisplayName("블럭마다 서로 다른 번호를 가진다")
        void everyCodeIsDistinct() {
            Set<Integer> seen = new HashSet<>();
            for (BlockType type : BlockType.values()) {
                assertTrue(seen.add(type.code()), "번호가 중복됩니다: " + type.code());
            }
        }

        @Test
        @DisplayName("번호로 되돌리면 원래 블럭이 나온다")
        void roundTrip() {
            for (BlockType type : BlockType.values()) {
                assertEquals(type, BlockType.fromCode(type.code()));
            }
        }

        @Test
        @DisplayName("빈 칸 번호는 null을 반환한다")
        void emptyCodeReturnsNull() {
            assertNull(BlockType.fromCode(BlockType.EMPTY_CODE));
        }

        @Test
        @DisplayName("정의되지 않은 번호면 예외를 던진다")
        void throwsOnUnknownCode() {
            assertThrows(IllegalArgumentException.class, () -> BlockType.fromCode(99));
            assertThrows(IllegalArgumentException.class, () -> BlockType.fromCode(-1));
        }
    }

    // ------------------------------------------------------------------
    // 모양 조회
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("isFilled() / shape()")
    class Access {

        @Test
        @DisplayName("격자 밖을 물으면 비어 있는 것으로 답한다")
        void outsideGridIsEmpty() {
            assertFalse(BlockType.T.isFilled(0, -1, 0));
            assertFalse(BlockType.T.isFilled(0, BlockType.SIZE, 0));
            assertFalse(BlockType.T.isFilled(0, 0, -1));
            assertFalse(BlockType.T.isFilled(0, 0, BlockType.SIZE));
        }

        @Test
        @DisplayName("회전 상태가 범위를 넘어도 순환해서 답한다")
        void normalizesRotation() {
            for (int r = 0; r < BlockType.SIZE; r++) {
                for (int c = 0; c < BlockType.SIZE; c++) {
                    assertEquals(
                            BlockType.S.isFilled(1, r, c),
                            BlockType.S.isFilled(5, r, c));
                }
            }
        }

        @Test
        @DisplayName("반환한 모양을 수정해도 원본은 바뀌지 않는다")
        void shapeReturnsDefensiveCopy() {
            boolean[][] shape = BlockType.T.shape(0);
            boolean before = BlockType.T.isFilled(0, 0, 0);

            shape[0][0] = !shape[0][0];

            assertEquals(before, BlockType.T.isFilled(0, 0, 0));
        }

        @Test
        @DisplayName("shape()와 isFilled()의 결과가 일치한다")
        void shapeMatchesIsFilled() {
            for (BlockType type : BlockType.values()) {
                for (int rotation = 0; rotation < BlockType.ROTATIONS; rotation++) {
                    boolean[][] shape = type.shape(rotation);
                    for (int r = 0; r < BlockType.SIZE; r++) {
                        for (int c = 0; c < BlockType.SIZE; c++) {
                            assertEquals(shape[r][c], type.isFilled(rotation, r, c));
                        }
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // 회전할 때 칸의 대응
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("rotateCellClockwise() / rotateCellCounterClockwise()")
    class CellRotation {

        @Test
        @DisplayName("채워진 칸은 회전한 뒤에도 채워진 칸으로 옮겨간다")
        void filledCellStaysFilledAfterRotation() {
            for (BlockType type : BlockType.values()) {
                for (int rotation = 0; rotation < BlockType.ROTATIONS; rotation++) {
                    for (int r = 0; r < BlockType.SIZE; r++) {
                        for (int c = 0; c < BlockType.SIZE; c++) {
                            if (!type.isFilled(rotation, r, c)) {
                                continue;
                            }
                            int[] cw = type.rotateCellClockwise(r, c);
                            int[] ccw = type.rotateCellCounterClockwise(r, c);
                            String where = type + " 회전 " + rotation + " (" + r + ", " + c + ")";
                            assertTrue(type.isFilled(rotation + 1, cw[0], cw[1]), where + " 시계방향");
                            assertTrue(type.isFilled(rotation - 1, ccw[0], ccw[1]), where + " 반시계방향");
                        }
                    }
                }
            }
        }

        @Test
        @DisplayName("같은 방향으로 네 번 돌리면 처음 칸으로 돌아온다")
        void fourRotationsReturnToStart() {
            for (BlockType type : BlockType.values()) {
                for (int r = 0; r < BlockType.SIZE; r++) {
                    for (int c = 0; c < BlockType.SIZE; c++) {
                        if (!type.isFilled(0, r, c)) {
                            continue;
                        }
                        int[] cell = {r, c};
                        for (int i = 0; i < BlockType.ROTATIONS; i++) {
                            cell = type.rotateCellClockwise(cell[0], cell[1]);
                        }
                        assertEquals(r, cell[0], type.toString());
                        assertEquals(c, cell[1], type.toString());
                    }
                }
            }
        }

        @Test
        @DisplayName("반시계방향은 시계방향의 반대로 옮긴다")
        void counterClockwiseUndoesClockwise() {
            for (BlockType type : BlockType.values()) {
                for (int r = 0; r < BlockType.SIZE; r++) {
                    for (int c = 0; c < BlockType.SIZE; c++) {
                        int[] cw = type.rotateCellClockwise(r, c);
                        int[] back = type.rotateCellCounterClockwise(cw[0], cw[1]);
                        assertEquals(r, back[0], type.toString());
                        assertEquals(c, back[1], type.toString());
                    }
                }
            }
        }

        @Test
        @DisplayName("O형은 2×2 정사각형 안에서 시계방향으로 한 칸씩 돈다")
        void oBlockRotatesWithinSquare() {
            // .OO.  왼쪽 위(0, 1) → 오른쪽 위(0, 2) → 오른쪽 아래(1, 2) → 왼쪽 아래(1, 1) → 왼쪽 위
            // .OO.
            int[][] expected = {{0, 2}, {1, 2}, {1, 1}, {0, 1}};
            int[] cell = {0, 1};
            for (int[] next : expected) {
                cell = BlockType.O.rotateCellClockwise(cell[0], cell[1]);
                assertEquals(next[0], cell[0]);
                assertEquals(next[1], cell[1]);
            }
        }
    }
}
