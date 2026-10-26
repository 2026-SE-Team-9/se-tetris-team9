package seoultech.se.tetris.ui.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import seoultech.se.tetris.blocks.BlockType;
import seoultech.se.tetris.blocks.CellCode;
import seoultech.se.tetris.blocks.ItemType;

class BlockPaletteTest {

    @Test
    void 테트로미노_칸_번호의_색은_블럭_종류의_색과_같다() {
        for (BlockType type : BlockType.values()) {
            assertEquals(BlockPalette.colorOf(type), BlockPalette.colorOf(type.code()));
        }
    }

    @Test
    void 아이템_칸_번호마다_색이_있다() {
        for (ItemType item : ItemType.values()) {
            assertNotNull(BlockPalette.colorOf(item.code()), item.name());
        }
    }

    @Test
    void 아이템_색은_서로_다르고_기본_블럭_색과도_다르다() {
        Set<Color> colors = new HashSet<>();
        for (BlockType type : BlockType.values()) {
            colors.add(BlockPalette.colorOf(type));
        }
        int blocks = colors.size();
        for (ItemType item : ItemType.values()) {
            colors.add(BlockPalette.colorOf(item.code()));
        }

        assertEquals(blocks + ItemType.values().length, colors.size());
    }

    @Test
    void 색이_정해진_모든_번호는_표시_문자도_있다() {
        int max = BlockType.values().length + ItemType.values().length;
        for (int code = 1; code <= max; code++) {
            assertNotNull(BlockPalette.colorOf(code));
            assertTrue(CellCode.symbolOf(code) != 0);
        }
    }

    @Test
    void 빈_칸이나_정의되지_않은_번호는_색을_정할_수_없다() {
        assertThrows(IllegalArgumentException.class, () -> BlockPalette.colorOf(CellCode.EMPTY));
        assertThrows(IllegalArgumentException.class, () -> BlockPalette.colorOf(-1));
        assertThrows(IllegalArgumentException.class,
                () -> BlockPalette.colorOf(BlockType.values().length + ItemType.values().length + 1));
    }

    // ------------------------------------------------------------------
    // 색맹 모드
    // ------------------------------------------------------------------

    private static Color cb(int code) {
        return BlockPalette.colorOf(code, true);
    }

    @Test
    void 색맹_모드를_지정하지_않으면_기본_색표를_쓴다() {
        int max = BlockType.values().length + ItemType.values().length;
        for (int code = 1; code <= max; code++) {
            assertEquals(BlockPalette.colorOf(code), BlockPalette.colorOf(code, false));
        }
    }

    @Test
    void 색맹_모드의_블럭_7종은_Okabe_Ito_색이다() {
        assertEquals(new Color(0x56B4E9), cb(BlockType.I.code()));
        assertEquals(new Color(0xF0E442), cb(BlockType.O.code()));
        assertEquals(new Color(0xCC79A7), cb(BlockType.T.code()));
        assertEquals(new Color(0x009E73), cb(BlockType.S.code()));
        assertEquals(new Color(0xD55E00), cb(BlockType.Z.code()));
        assertEquals(new Color(0x0072B2), cb(BlockType.J.code()));
        assertEquals(new Color(0xE69F00), cb(BlockType.L.code()));
    }

    @Test
    void 색맹_모드의_줄_삭제와_Double_or_Nothing은_진보라와_진갈색이다() {
        assertEquals(new Color(0x6600CC), cb(ItemType.LINE_CLEAR.code()));
        assertEquals(new Color(0x663311), cb(ItemType.DOUBLE_OR_NOTHING.code()));
    }

    @Test
    void 색맹_모드의_톨보이_무게추_크러셔는_어두운_순서대로_밝기가_다른_회색이다() {
        Color weight = cb(ItemType.WEIGHT.code());
        Color tallBoy = cb(ItemType.TALL_BOY.code());
        Color crusher = cb(ItemType.CRUSHER.code());
        for (Color gray : new Color[] {weight, tallBoy, crusher}) {
            assertEquals(gray.getRed(), gray.getGreen());
            assertEquals(gray.getGreen(), gray.getBlue());
        }
        assertTrue(tallBoy.getRed() < weight.getRed() && weight.getRed() < crusher.getRed());
    }

    @Test
    void 색맹_모드의_색은_모두_다르고_깜빡임_색인_흰색을_쓰지_않는다() {
        Set<Color> colors = new HashSet<>();
        int max = BlockType.values().length + ItemType.values().length;
        for (int code = 1; code <= max; code++) {
            assertNotEquals(GamePanel.FLASH_COLOR, cb(code));
            colors.add(cb(code));
        }
        assertEquals(max, colors.size());
    }

    @Test
    void 글자색은_바탕이_어두우면_흰색_밝으면_검정이다() {
        assertEquals(Color.WHITE, BlockPalette.textColorOf(BlockType.J.code(), false));   // 진파랑
        assertEquals(Color.BLACK, BlockPalette.textColorOf(BlockType.O.code(), false));   // 노랑
        assertEquals(Color.WHITE, BlockPalette.textColorOf(ItemType.LINE_CLEAR.code(), true));
        assertEquals(Color.WHITE, BlockPalette.textColorOf(ItemType.DOUBLE_OR_NOTHING.code(), true));
        assertEquals(Color.WHITE, BlockPalette.textColorOf(ItemType.TALL_BOY.code(), true));
        assertEquals(Color.BLACK, BlockPalette.textColorOf(ItemType.WEIGHT.code(), true));
        assertEquals(Color.BLACK, BlockPalette.textColorOf(ItemType.CRUSHER.code(), true));
    }

    @Test
    void 테두리는_색맹_모드의_아이템_칸에만_있다() {
        assertNull(BlockPalette.borderColorOf(ItemType.WEIGHT.code(), false));
        assertNull(BlockPalette.borderColorOf(BlockType.T.code(), true));
        for (ItemType item : ItemType.values()) {
            assertNotNull(BlockPalette.borderColorOf(item.code(), true), item.name());
        }
    }

    @Test
    void 테두리는_어두운_칸에는_밝게_밝은_칸에는_어둡게_그린다() {
        for (ItemType dark : new ItemType[] {ItemType.LINE_CLEAR, ItemType.DOUBLE_OR_NOTHING, ItemType.TALL_BOY}) {
            assertTrue(BlockPalette.borderColorOf(dark.code(), true).getRed() > 200, dark.name());
        }
        for (ItemType light : new ItemType[] {ItemType.WEIGHT, ItemType.CRUSHER}) {
            assertTrue(BlockPalette.borderColorOf(light.code(), true).getRed() < 60, light.name());
        }
    }
}
