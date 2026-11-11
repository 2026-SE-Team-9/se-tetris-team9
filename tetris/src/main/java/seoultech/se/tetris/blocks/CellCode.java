package seoultech.se.tetris.blocks;

/**
 * 보드 칸 번호를 해석한다.
 *
 * <p>보드의 각 칸에는 번호 하나가 저장된다.
 * <pre>
 * 0        빈 칸
 * 1 ~ 7    테트로미노 7종 ({@link BlockType#code()})
 * 8 ~ 12   아이템 5종 ({@link ItemType#code()})
 * 13       공격 칸 ({@link #GARBAGE})
 * </pre>
 *
 * <p><b>화면 쪽은 번호를 직접 비교하지 않고 이 클래스를 거친다.</b>
 * 번호 범위가 바뀌어도 이 클래스만 고치면 되고, 대응이 어긋나 엉뚱한 문자가 그려지는 일을 막는다.
 */
public final class CellCode {

    /** 빈 칸 번호 */
    public static final int EMPTY = BlockType.EMPTY_CODE;

    /**
     * 공격 칸 번호. 대전 모드에서 상대에게서 넘어온 줄의 칸이다.
     *
     * <p>아이템 번호 바로 뒤에 붙인다.
     * 넘어온 줄의 칸은 원래 블럭이나 아이템과 관계없이 모두 이 번호가 된다.
     */
    public static final int GARBAGE = BlockType.values().length + ItemType.values().length + 1;

    /** 공격 칸의 표시 문자 */
    public static final char GARBAGE_SYMBOL = 'X';

    private CellCode() {
    }

    public static boolean isEmpty(int code) {
        return code == EMPTY;
    }

    /** 테트로미노 칸이면 그 종류, 아니면 {@code null} */
    public static BlockType blockTypeOf(int code) {
        for (BlockType type : BlockType.values()) {
            if (type.code() == code) {
                return type;
            }
        }
        return null;
    }

    /** 아이템 칸이면 그 아이템, 아니면 {@code null} */
    public static ItemType itemOf(int code) {
        for (ItemType item : ItemType.values()) {
            if (item.code() == code) {
                return item;
            }
        }
        return null;
    }

    public static boolean isItem(int code) {
        return itemOf(code) != null;
    }

    public static boolean isGarbage(int code) {
        return code == GARBAGE;
    }

    /**
     * 칸에 표시할 문자.
     *
     * @throws IllegalArgumentException 빈 칸이거나 정의되지 않은 번호인 경우
     */
    public static char symbolOf(int code) {
        BlockType type = blockTypeOf(code);
        if (type != null) {
            return type.symbol();
        }
        ItemType item = itemOf(code);
        if (item != null) {
            return item.symbol();
        }
        if (code == GARBAGE) {
            return GARBAGE_SYMBOL;
        }
        throw new IllegalArgumentException("표시 문자가 없는 칸 번호입니다: " + code);
    }
}
