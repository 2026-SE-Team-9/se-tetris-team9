package seoultech.se.tetris.ui.game;

import java.awt.Color;
import java.util.EnumMap;
import java.util.Map;

import seoultech.se.tetris.blocks.BlockType;
import seoultech.se.tetris.blocks.CellCode;
import seoultech.se.tetris.blocks.ItemType;

/**
 * 블럭 종류와 아이템별 색을 정한다. 모델은 색을 모르므로 색 결정은 여기서만 한다.
 * 기본 색은 세계 표준(Tetris Guideline)을 따르며, 색 외에 칸 안의 글자({@link BlockType#symbol()})로도 구분한다.
 *
 * <p><b>색맹 모드</b>에서는 다른 색표를 쓴다.
 * <ul>
 *   <li>블럭 7종: 색각이상에서도 구분되도록 만든 Okabe–Ito 팔레트의 7색</li>
 *   <li>Tall Boy·무게추·Crusher: 모양으로도 구분되므로 어두운 순서대로 밝기만 다른 회색 3단계</li>
 *   <li>줄 삭제(L)·Double or Nothing(D): 블럭 한 칸에 붙어 모양으로 구분할 수 없으므로,
 *       정상·적색맹·녹색맹·청색맹 시야 모두에서 다른 색과의 색차가 가장 크도록 고른 진보라와 진갈색</li>
 * </ul>
 * 색맹 모드의 아이템 칸에는 안쪽 테두리를 그려 블럭 칸과 구분한다.
 *
 * <p>칸 안의 글자는 두 모드 모두 바탕색에 따라 검정과 흰색 중 대비가 큰 쪽을 쓴다.
 */
public final class BlockPalette {

    private static final Map<BlockType, Color> STANDARD = new EnumMap<>(BlockType.class);
    private static final Map<ItemType, Color> ITEMS = new EnumMap<>(ItemType.class);
    private static final Map<BlockType, Color> COLOR_BLIND = new EnumMap<>(BlockType.class);
    private static final Map<ItemType, Color> COLOR_BLIND_ITEMS = new EnumMap<>(ItemType.class);

    static {
        STANDARD.put(BlockType.I, new Color(0x00, 0xF0, 0xF0));
        STANDARD.put(BlockType.O, new Color(0xF0, 0xF0, 0x00));
        STANDARD.put(BlockType.T, new Color(0xA0, 0x00, 0xF0));
        STANDARD.put(BlockType.S, new Color(0x00, 0xF0, 0x00));
        STANDARD.put(BlockType.Z, new Color(0xF0, 0x00, 0x00));
        STANDARD.put(BlockType.J, new Color(0x00, 0x00, 0xF0));
        STANDARD.put(BlockType.L, new Color(0xF0, 0xA0, 0x00));

        // 아이템은 칸 안의 글자로 구분하고, 색은 기본 블럭 7종과 겹치지 않게 따로 정한다.
        // 포함형(L, D)은 붙은 칸만 이 색이므로 같은 블럭의 나머지 칸과 구분된다.
        ITEMS.put(ItemType.LINE_CLEAR, new Color(0xFF, 0x69, 0xB4));
        ITEMS.put(ItemType.WEIGHT, new Color(0xC0, 0xC0, 0xC0));
        ITEMS.put(ItemType.DOUBLE_OR_NOTHING, new Color(0xDD, 0xA0, 0xDD));
        ITEMS.put(ItemType.TALL_BOY, new Color(0xA0, 0x72, 0x4A));
        ITEMS.put(ItemType.CRUSHER, new Color(0x78, 0x90, 0x9C));

        // 색맹 모드. 블럭은 표준색과 비슷한 느낌이 나도록 Okabe–Ito 색을 대응시켰다.
        COLOR_BLIND.put(BlockType.I, new Color(0x56, 0xB4, 0xE9));   // 하늘
        COLOR_BLIND.put(BlockType.O, new Color(0xF0, 0xE4, 0x42));   // 노랑
        COLOR_BLIND.put(BlockType.T, new Color(0xCC, 0x79, 0xA7));   // 자주
        COLOR_BLIND.put(BlockType.S, new Color(0x00, 0x9E, 0x73));   // 청록
        COLOR_BLIND.put(BlockType.Z, new Color(0xD5, 0x5E, 0x00));   // 주홍
        COLOR_BLIND.put(BlockType.J, new Color(0x00, 0x72, 0xB2));   // 파랑
        COLOR_BLIND.put(BlockType.L, new Color(0xE6, 0x9F, 0x00));   // 주황

        // 흰색은 줄 삭제 애니메이션의 깜빡임 색이라 쓰지 않는다.
        COLOR_BLIND_ITEMS.put(ItemType.LINE_CLEAR, new Color(0x66, 0x00, 0xCC));          // 진보라
        COLOR_BLIND_ITEMS.put(ItemType.DOUBLE_OR_NOTHING, new Color(0x66, 0x33, 0x11));   // 진갈색
        // 회색 3단계는 밝기 간격을 같게 두고, Tall Boy가 가장 어둡다.
        COLOR_BLIND_ITEMS.put(ItemType.TALL_BOY, new Color(0x55, 0x55, 0x55));
        COLOR_BLIND_ITEMS.put(ItemType.WEIGHT, new Color(0x88, 0x88, 0x88));
        COLOR_BLIND_ITEMS.put(ItemType.CRUSHER, new Color(0xBB, 0xBB, 0xBB));
    }

    private BlockPalette() {
    }

    public static Color colorOf(BlockType type) {
        return STANDARD.get(type);
    }

    /**
     * 칸 번호에 해당하는 색. 테트로미노 칸과 아이템 칸을 모두 다룬다.
     *
     * <p>번호의 종류는 {@link CellCode}로 구분하므로 번호 범위가 바뀌어도 이 메서드는 바뀌지 않는다.
     *
     * @throws IllegalArgumentException 빈 칸이거나 정의되지 않은 번호인 경우
     */
    public static Color colorOf(int code) {
        return colorOf(code, false);
    }

    /**
     * 칸 번호에 해당하는 색. 색맹 모드이면 색맹용 색표를 쓴다.
     *
     * @throws IllegalArgumentException 빈 칸이거나 정의되지 않은 번호인 경우
     */
    public static Color colorOf(int code, boolean colorBlind) {
        BlockType type = CellCode.blockTypeOf(code);
        if (type != null) {
            return (colorBlind ? COLOR_BLIND : STANDARD).get(type);
        }
        ItemType item = CellCode.itemOf(code);
        if (item != null) {
            return (colorBlind ? COLOR_BLIND_ITEMS : ITEMS).get(item);
        }
        throw new IllegalArgumentException("색이 없는 칸 번호입니다: " + code);
    }

    /** 칸 안 글자의 색. 바탕색에 대해 검정과 흰색 중 명암 대비가 큰 쪽이다. */
    public static Color textColorOf(int code, boolean colorBlind) {
        return isDark(colorOf(code, colorBlind)) ? Color.WHITE : Color.BLACK;
    }

    /**
     * 칸 안쪽에 그릴 테두리의 색. 색맹 모드의 아이템 칸에만 있고, 그 외에는 {@code null}이다.
     *
     * <p>어두운 칸에는 밝은 테두리, 밝은 칸에는 어두운 테두리를 쓴다.
     */
    public static Color borderColorOf(int code, boolean colorBlind) {
        if (!colorBlind || !CellCode.isItem(code)) {
            return null;
        }
        return isDark(colorOf(code, true)) ? new Color(0xEB, 0xEB, 0xEB) : new Color(0x28, 0x28, 0x28);
    }

    /**
     * 흰 글자의 대비가 검은 글자보다 큰지. WCAG의 상대 휘도로 두 대비를 비교한다.
     */
    static boolean isDark(Color color) {
        double luminance = 0.2126 * linear(color.getRed())
                + 0.7152 * linear(color.getGreen())
                + 0.0722 * linear(color.getBlue());
        double contrastWithWhite = 1.05 / (luminance + 0.05);
        double contrastWithBlack = (luminance + 0.05) / 0.05;
        return contrastWithWhite > contrastWithBlack;
    }

    private static double linear(int channel) {
        double c = channel / 255.0;
        return c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }
}
