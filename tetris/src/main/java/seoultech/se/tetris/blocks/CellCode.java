package seoultech.se.tetris.blocks;

/**
 * 보드 칸 번호를 해석한다.
 *
 * <p>보드의 각 칸에는 번호 하나가 저장된다.
 * <pre>
 * 0        빈 칸
 * 1 ~ 7    테트로미노 7종 ({@link BlockType#code()})
 * 8 ~ 12   아이템 5종 ({@link ItemType#code()})
 * </pre>
 *
 * <p><b>화면 쪽은 번호를 직접 비교하지 않고 이 클래스를 거친다.</b>
 * 번호 범위가 바뀌어도 이 클래스만 고치면 되고, 대응이 어긋나 엉뚱한 문자가 그려지는 일을 막는다.
 */
public final class CellCode {

    /** 빈 칸 번호 */
    public static final int EMPTY = BlockType.EMPTY_CODE;

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
        throw new IllegalArgumentException("표시 문자가 없는 칸 번호입니다: " + code);
    }
}
