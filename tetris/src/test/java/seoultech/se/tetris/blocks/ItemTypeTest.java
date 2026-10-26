package seoultech.se.tetris.blocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ItemType")
class ItemTypeTest {

    /** 독립형 아이템의 모양을 '.'과 '#'으로 그린다. */
    private static String drawn(ItemType item) {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < BlockType.SIZE; r++) {
            for (int c = 0; c < BlockType.SIZE; c++) {
                sb.append(item.isFilled(0, r, c) ? '#' : '.');
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    @Nested
    @DisplayName("표시 문자와 번호")
    class SymbolsAndCodes {

        @Test
        @DisplayName("요구사항 문서의 아이템 문자표대로 표시한다")
        void usesDocumentedSymbols() {
            assertEquals('L', ItemType.LINE_CLEAR.symbol());
            assertEquals('A', ItemType.WEIGHT.symbol());
            assertEquals('D', ItemType.DOUBLE_OR_NOTHING.symbol());
            assertEquals('B', ItemType.TALL_BOY.symbol());
            assertEquals('C', ItemType.CRUSHER.symbol());
        }

        @Test
        @DisplayName("아이템끼리, 그리고 기본 블럭·고스트와 문자가 겹치지 않는다")
        void symbolsAreUnique() {
            Set<Character> seen = new HashSet<>();
            for (BlockType type : BlockType.values()) {
                seen.add(type.symbol());
            }
            seen.add('#');
            for (ItemType item : ItemType.values()) {
                assertTrue(seen.add(item.symbol()), item + "의 문자가 겹칩니다: " + item.symbol());
            }
        }

        @Test
        @DisplayName("칸 번호가 빈 칸·기본 블럭 번호와 겹치지 않고 서로 다르다")
        void codesAreUnique() {
            Set<Integer> seen = new HashSet<>();
            seen.add(CellCode.EMPTY);
            for (BlockType type : BlockType.values()) {
                seen.add(type.code());
            }
            for (ItemType item : ItemType.values()) {
                assertTrue(seen.add(item.code()), item + "의 번호가 겹칩니다: " + item.code());
            }
        }
    }

    @Nested
    @DisplayName("형태")
    class Forms {

        @Test
        @DisplayName("줄 삭제와 Double or Nothing만 블럭에 붙는 포함형이다")
        void embeddedItems() {
            assertTrue(ItemType.LINE_CLEAR.isEmbedded());
            assertTrue(ItemType.DOUBLE_OR_NOTHING.isEmbedded());
            assertFalse(ItemType.WEIGHT.isEmbedded());
            assertFalse(ItemType.TALL_BOY.isEmbedded());
            assertFalse(ItemType.CRUSHER.isEmbedded());
        }

        @Test
        @DisplayName("무게추는 위 2칸, 아래 4칸으로 너비가 4칸이다")
        void weightShape() {
            assertEquals(".##.\n####\n....\n....\n", drawn(ItemType.WEIGHT));
        }

        @Test
        @DisplayName("Tall Boy는 1×1이다")
        void tallBoyShape() {
            assertEquals(".#..\n....\n....\n....\n", drawn(ItemType.TALL_BOY));
        }

        @Test
        @DisplayName("Crusher는 3×3 사각형이다")
        void crusherShape() {
            assertEquals("###.\n###.\n###.\n....\n", drawn(ItemType.CRUSHER));
        }

        @Test
        @DisplayName("독립형 아이템은 회전해도 모양이 같다")
        void standaloneDoesNotRotate() {
            for (ItemType item : ItemType.values()) {
                if (item.isEmbedded()) {
                    continue;
                }
                for (int rotation = 1; rotation < BlockType.ROTATIONS; rotation++) {
                    for (int r = 0; r < BlockType.SIZE; r++) {
                        for (int c = 0; c < BlockType.SIZE; c++) {
                            assertEquals(item.isFilled(0, r, c), item.isFilled(rotation, r, c), item.toString());
                        }
                    }
                }
            }
        }

        @Test
        @DisplayName("격자 밖을 물으면 비어 있는 것으로 답한다")
        void outsideGridIsEmpty() {
            assertFalse(ItemType.CRUSHER.isFilled(0, -1, 0));
            assertFalse(ItemType.CRUSHER.isFilled(0, 0, BlockType.SIZE));
        }

        @Test
        @DisplayName("포함형 아이템의 모양을 물으면 예외를 던진다")
        void embeddedHasNoShape() {
            assertThrows(IllegalStateException.class, () -> ItemType.LINE_CLEAR.isFilled(0, 0, 0));
        }
    }
}
