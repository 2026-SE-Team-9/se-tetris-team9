package seoultech.se.tetris.engine;

import seoultech.se.tetris.blocks.ItemType;

/**
 * 아이템 종류와 동작을 연결한다.
 *
 * <p>{@code switch}로 모든 아이템을 나열하므로, {@link ItemType}에 아이템을 추가하고
 * 여기에 동작을 등록하지 않으면 컴파일 오류가 나서 빠뜨릴 수 없다.
 */
final class ItemEffects {

    /** 아이템이 없는 일반 블럭의 동작. 인터페이스의 기본 동작을 그대로 쓴다. */
    static final ItemEffect NONE = new ItemEffect() { };

    private static final ItemEffect LINE_CLEAR = new LineClearEffect();
    private static final ItemEffect WEIGHT = new WeightEffect();
    private static final ItemEffect DOUBLE_OR_NOTHING = new DoubleOrNothingEffect();
    private static final ItemEffect TALL_BOY = new TallBoyEffect();
    private static final ItemEffect CRUSHER = new CrusherEffect();

    private ItemEffects() {
    }

    /** 아이템의 동작. {@code null}(아이템 없음)이면 일반 블럭의 동작. */
    static ItemEffect of(ItemType item) {
        if (item == null) {
            return NONE;
        }
        return switch (item) {
            case LINE_CLEAR -> LINE_CLEAR;
            case WEIGHT -> WEIGHT;
            case DOUBLE_OR_NOTHING -> DOUBLE_OR_NOTHING;
            case TALL_BOY -> TALL_BOY;
            case CRUSHER -> CRUSHER;
        };
    }
}
