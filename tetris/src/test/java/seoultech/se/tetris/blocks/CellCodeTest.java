package seoultech.se.tetris.blocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CellCode")
class CellCodeTest {

    @Test
    @DisplayName("빈 칸 번호는 0이고 블럭도 아이템도 아니다")
    void emptyCode() {
        assertEquals(0, CellCode.EMPTY);
        assertTrue(CellCode.isEmpty(CellCode.EMPTY));
        assertNull(CellCode.blockTypeOf(CellCode.EMPTY));
        assertNull(CellCode.itemOf(CellCode.EMPTY));
        assertFalse(CellCode.isItem(CellCode.EMPTY));
    }

    @Test
    @DisplayName("기본 블럭 번호는 그 블럭으로, 표시 문자는 블럭의 문자로 해석한다")
    void blockCodes() {
        for (BlockType type : BlockType.values()) {
            assertEquals(type, CellCode.blockTypeOf(type.code()));
            assertNull(CellCode.itemOf(type.code()));
            assertEquals(type.symbol(), CellCode.symbolOf(type.code()));
        }
    }

    @Test
    @DisplayName("아이템 번호는 그 아이템으로, 표시 문자는 아이템의 문자로 해석한다")
    void itemCodes() {
        for (ItemType item : ItemType.values()) {
            assertEquals(item, CellCode.itemOf(item.code()));
            assertTrue(CellCode.isItem(item.code()));
            assertNull(CellCode.blockTypeOf(item.code()));
            assertEquals(item.symbol(), CellCode.symbolOf(item.code()));
        }
    }

    @Test
    @DisplayName("빈 칸이나 정의되지 않은 번호의 표시 문자를 물으면 예외를 던진다")
    void rejectsCodesWithoutSymbol() {
        assertThrows(IllegalArgumentException.class, () -> CellCode.symbolOf(CellCode.EMPTY));
        assertThrows(IllegalArgumentException.class, () -> CellCode.symbolOf(99));
        assertThrows(IllegalArgumentException.class, () -> CellCode.symbolOf(-1));
    }
}
