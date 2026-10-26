package seoultech.se.tetris.blocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Block")
class BlockTest {

    // ------------------------------------------------------------------
    // 생성
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("생성")
    class Creation {

        @Test
        @DisplayName("of()는 회전하지 않은 상태로 만든다")
        void ofCreatesUnrotated() {
            Block block = Block.of(BlockType.T, 5, 3);

            assertEquals(BlockType.T, block.type());
            assertEquals(0, block.rotation());
            assertEquals(5, block.row());
            assertEquals(3, block.col());
        }

        @Test
        @DisplayName("회전 상태가 범위를 넘으면 정규화한다")
        void normalizesRotation() {
            assertEquals(1, new Block(BlockType.T, 5, 0, 0).rotation());
            assertEquals(3, new Block(BlockType.T, -1, 0, 0).rotation());
        }

        @Test
        @DisplayName("보드 밖의 위치도 만들 수 있다")
        void allowsPositionOutsideBoard() {
            // 유효성 판단은 Board가 한다. Block 혼자서는 보드 크기를 모른다.
            Block block = Block.of(BlockType.I, -5, -50);

            assertEquals(-5, block.row());
            assertEquals(-50, block.col());
        }

        @Test
        @DisplayName("블럭 종류가 null이면 예외를 던진다")
        void throwsOnNullType() {
            assertThrows(NullPointerException.class, () -> Block.of(null, 0, 0));
        }
    }

    // ------------------------------------------------------------------
    // 불변성
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("불변성")
    class Immutability {

        @Test
        @DisplayName("회전해도 원본은 바뀌지 않는다")
        void rotatedDoesNotMutateOriginal() {
            Block original = Block.of(BlockType.T, 5, 3);

            Block rotated = original.rotated();

            assertEquals(0, original.rotation());
            assertEquals(1, rotated.rotation());
        }

        @Test
        @DisplayName("이동해도 원본은 바뀌지 않는다")
        void movedDoesNotMutateOriginal() {
            Block original = Block.of(BlockType.T, 5, 3);

            Block moved = original.moved(1, -1);

            assertEquals(5, original.row());
            assertEquals(3, original.col());
            assertEquals(6, moved.row());
            assertEquals(2, moved.col());
        }

        @Test
        @DisplayName("회전해도 위치는 유지된다")
        void rotationKeepsPosition() {
            Block rotated = Block.of(BlockType.T, 5, 3).rotated();

            assertEquals(5, rotated.row());
            assertEquals(3, rotated.col());
        }

        @Test
        @DisplayName("이동해도 종류와 회전 상태는 유지된다")
        void moveKeepsTypeAndRotation() {
            Block moved = new Block(BlockType.S, 2, 5, 3).moved(1, 1);

            assertEquals(BlockType.S, moved.type());
            assertEquals(2, moved.rotation());
        }
    }

    // ------------------------------------------------------------------
    // 회전
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("회전")
    class Rotation {

        @Test
        @DisplayName("네 번 회전하면 원래 상태로 돌아온다")
        void fourRotationsReturnToStart() {
            Block original = Block.of(BlockType.J, 5, 3);

            Block result = original.rotated().rotated().rotated().rotated();

            assertEquals(original, result);
        }

        @Test
        @DisplayName("반시계 회전은 시계 회전의 반대다")
        void counterClockwiseIsOpposite() {
            Block original = Block.of(BlockType.L, 5, 3);

            assertEquals(original, original.rotated().rotatedCounterClockwise());
            assertEquals(3, original.rotatedCounterClockwise().rotation());
        }

        @Test
        @DisplayName("회전하면 모양이 바뀐다")
        void rotationChangesShape() {
            Block upright = Block.of(BlockType.I, 5, 3);
            Block sideways = upright.rotated();

            // I 상태0은 가로, 상태1은 세로다
            assertTrue(upright.isFilled(1, 0));
            assertFalse(sideways.isFilled(1, 0));
        }
    }

    // ------------------------------------------------------------------
    // 좌표
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("좌표 변환")
    class Coordinates {

        @Test
        @DisplayName("격자 좌표를 보드 좌표로 바꾼다")
        void convertsToBoardCoordinates() {
            Block block = Block.of(BlockType.T, 5, 3);

            assertEquals(5, block.boardRow(0));
            assertEquals(8, block.boardRow(3));
            assertEquals(3, block.boardCol(0));
            assertEquals(6, block.boardCol(3));
        }

        @Test
        @DisplayName("isFilled()는 격자 좌표를 쓴다")
        void isFilledUsesLocalCoordinates() {
            Block block = Block.of(BlockType.T, 10, 5);

            // 위치와 무관하게 T 상태0의 모양은 .T.. / TTT.
            assertTrue(block.isFilled(0, 1));
            assertTrue(block.isFilled(1, 0));
            assertFalse(block.isFilled(0, 0));
        }

        @Test
        @DisplayName("이동하면 보드 좌표가 따라 바뀐다")
        void boardCoordinatesFollowMovement() {
            Block moved = Block.of(BlockType.T, 5, 3).moved(2, 1);

            assertEquals(7, moved.boardRow(0));
            assertEquals(4, moved.boardCol(0));
        }
    }

    // ------------------------------------------------------------------
    // 동치성
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("equals() / hashCode()")
    class Equality {

        @Test
        @DisplayName("네 값이 모두 같으면 같은 블럭이다")
        void equalWhenAllFieldsMatch() {
            Block a = new Block(BlockType.Z, 2, 5, 3);
            Block b = new Block(BlockType.Z, 2, 5, 3);

            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        @DisplayName("값이 하나라도 다르면 다른 블럭이다")
        void notEqualWhenAnyFieldDiffers() {
            Block base = new Block(BlockType.Z, 2, 5, 3);

            assertNotEquals(base, new Block(BlockType.S, 2, 5, 3));
            assertNotEquals(base, new Block(BlockType.Z, 1, 5, 3));
            assertNotEquals(base, new Block(BlockType.Z, 2, 6, 3));
            assertNotEquals(base, new Block(BlockType.Z, 2, 5, 4));
        }

        @Test
        @DisplayName("정규화된 회전 상태가 같으면 같은 블럭이다")
        void equalAfterRotationNormalization() {
            assertEquals(
                    new Block(BlockType.Z, 1, 5, 3),
                    new Block(BlockType.Z, 5, 5, 3));
        }

        @Test
        @DisplayName("붙은 아이템이나 그 위치가 다르면 다른 블럭이다")
        void notEqualWhenItemDiffers() {
            Block plain = Block.of(BlockType.I, 0, 0);
            Block lineClear = plain.withItem(ItemType.LINE_CLEAR, 1, 0);

            assertNotEquals(plain, lineClear);
            assertNotEquals(lineClear, plain.withItem(ItemType.DOUBLE_OR_NOTHING, 1, 0));
            assertNotEquals(lineClear, plain.withItem(ItemType.LINE_CLEAR, 1, 1));
            assertEquals(lineClear, plain.withItem(ItemType.LINE_CLEAR, 1, 0));
        }
    }

    // ------------------------------------------------------------------
    // 아이템
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("포함형 아이템")
    class EmbeddedItem {

        @Test
        @DisplayName("붙인 칸은 아이템 번호, 나머지 채워진 칸은 블럭 번호, 빈 칸은 빈 칸 번호다")
        void codeAtDistinguishesItemCell() {
            Block block = Block.of(BlockType.I, 0, 0).withItem(ItemType.LINE_CLEAR, 1, 2);

            assertEquals(ItemType.LINE_CLEAR.code(), block.codeAt(1, 2));
            assertEquals(BlockType.I.code(), block.codeAt(1, 0));
            assertEquals(CellCode.EMPTY, block.codeAt(0, 0));
            assertEquals(ItemType.LINE_CLEAR, block.item());
        }

        @Test
        @DisplayName("회전하면 붙은 칸도 같은 칸을 따라간다")
        void itemFollowsRotation() {
            // 가로 I의 맨 왼쪽 칸(1, 0)은 세로가 되면 맨 위 칸(0, 2)이 된다
            Block block = Block.of(BlockType.I, 0, 0).withItem(ItemType.LINE_CLEAR, 1, 0);

            Block rotated = block.rotated();

            assertEquals(ItemType.LINE_CLEAR.code(), rotated.codeAt(0, 2));
            assertEquals(0, rotated.itemRow());
            assertEquals(2, rotated.itemCol());
            assertEquals(block, rotated.rotatedCounterClockwise());
        }

        @Test
        @DisplayName("O형에 붙은 아이템도 회전하면 옆 칸으로 옮겨간다")
        void itemMovesOnOBlock() {
            // 모양은 그대로지만 블럭은 돌았으므로 붙은 칸이 왼쪽 위(0, 1)에서 오른쪽 위(0, 2)로 간다
            Block block = Block.of(BlockType.O, 0, 0).withItem(ItemType.LINE_CLEAR, 0, 1);

            Block rotated = block.rotated();

            assertEquals(ItemType.LINE_CLEAR.code(), rotated.codeAt(0, 2));
            assertEquals(BlockType.O.code(), rotated.codeAt(0, 1));
        }

        @Test
        @DisplayName("네 번 회전하면 붙은 칸이 처음 자리로 돌아온다")
        void itemReturnsAfterFullTurn() {
            Block block = Block.of(BlockType.T, 0, 0).withItem(ItemType.DOUBLE_OR_NOTHING, 0, 1);

            assertEquals(block, block.rotated().rotated().rotated().rotated());
        }

        @Test
        @DisplayName("이동해도 붙은 칸은 그대로다")
        void itemStaysWhenMoved() {
            Block block = Block.of(BlockType.S, 0, 0).withItem(ItemType.LINE_CLEAR, 0, 1);

            Block moved = block.moved(3, 2);

            assertEquals(ItemType.LINE_CLEAR.code(), moved.codeAt(0, 1));
        }

        @Test
        @DisplayName("일반 블럭은 아이템이 없다")
        void plainBlockHasNoItem() {
            Block block = Block.of(BlockType.T, 0, 0);

            assertNull(block.item());
            assertEquals(-1, block.itemRow());
            assertEquals(-1, block.itemCol());
        }

        @Test
        @DisplayName("붙일 수 없는 경우에는 예외를 던진다")
        void rejectsInvalidAttachment() {
            Block block = Block.of(BlockType.I, 0, 0);

            assertThrows(IllegalArgumentException.class,
                    () -> block.withItem(ItemType.LINE_CLEAR, 0, 0));        // 빈 칸
            assertThrows(IllegalArgumentException.class,
                    () -> block.withItem(ItemType.WEIGHT, 1, 0));            // 독립형 아이템
            assertThrows(IllegalArgumentException.class,
                    () -> block.withItem(ItemType.LINE_CLEAR, 1, 0)
                            .withItem(ItemType.DOUBLE_OR_NOTHING, 1, 1));    // 이미 붙어 있음
            assertThrows(NullPointerException.class,
                    () -> block.withItem(null, 1, 0));
        }
    }

    @Nested
    @DisplayName("독립형 아이템")
    class StandaloneItem {

        @Test
        @DisplayName("아이템 모양으로 블럭을 만들고 모든 칸이 아이템 번호다")
        void createsBlockFromItemShape() {
            Block block = Block.of(ItemType.CRUSHER, 0, 0);

            assertEquals(ItemType.CRUSHER, block.shape());
            assertEquals(ItemType.CRUSHER, block.item());
            assertEquals(ItemType.CRUSHER.code(), block.codeAt(0, 0));
            assertEquals(CellCode.EMPTY, block.codeAt(0, 3));
        }

        @Test
        @DisplayName("테트로미노 종류를 물으면 예외를 던진다")
        void typeIsNotAvailable() {
            Block block = Block.of(ItemType.TALL_BOY, 0, 0);

            assertThrows(IllegalStateException.class, block::type);
        }

        @Test
        @DisplayName("독립형 아이템에는 다른 아이템을 붙일 수 없다")
        void cannotAttachToStandalone() {
            Block block = Block.of(ItemType.WEIGHT, 0, 0);

            assertThrows(IllegalArgumentException.class,
                    () -> block.withItem(ItemType.LINE_CLEAR, 1, 0));
        }

        @Test
        @DisplayName("포함형 아이템으로는 블럭을 만들 수 없다")
        void embeddedItemCannotBeShape() {
            assertThrows(IllegalArgumentException.class, () -> Block.of(ItemType.LINE_CLEAR, 0, 0));
        }
    }
}
