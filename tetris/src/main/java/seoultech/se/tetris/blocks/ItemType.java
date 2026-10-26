package seoultech.se.tetris.blocks;

/**
 * 아이템 모드의 아이템 5종
 *
 * <p>아이템은 두 가지 형태로 등장한다.
 * <ul>
 *   <li><b>포함형</b>(줄 삭제 {@code L}, Double or Nothing {@code D}):
 *       테트로미노의 칸 하나에 붙는다. 자기 모양은 없으며 {@link Block#withItem}으로 붙인다.</li>
 *   <li><b>독립형</b>(무게추 {@code A}, Tall Boy {@code B}, Crusher {@code C}):
 *       자기 모양을 가진 조각이다. {@link Shape}로서 {@link Block}에 그대로 들어간다.</li>
 * </ul>
 *
 * <p>이 enum은 아이템의 <b>정체(문자, 칸 번호, 모양)</b>만 가진다.
 * 고정 시 무엇을 하는지 같은 동작은 게임 규칙이므로 엔진 쪽에서 정한다.
 *
 * <p>독립형 아이템은 회전하지 않는다. 모든 회전 상태에 같은 모양을 쓰므로 회전 조작을 해도 그대로다.
 *
 * <p><b>상수 순서를 바꾸면 칸 번호가 바뀐다.</b> 칸 번호는 저장하지 않으므로 파일 호환 문제는 없지만,
 * 화면 쪽이 번호를 직접 비교하지 않고 {@link CellCode}를 거치도록 한다.
 */
public enum ItemType implements Shape {

    /** 줄 삭제 (원문). 고정되면 이 칸이 있는 줄을 지운다. */
    LINE_CLEAR('L', null),

    /** 무게추 (원문). 너비 4칸, 위 2칸 · 아래 4칸. */
    WEIGHT('A', new String[] {
            ".AA.",
            "AAAA",
            "....",
            "...."
    }),

    /** Double or Nothing. 줄 삭제로 지워지면 점수 2배 */
    DOUBLE_OR_NOTHING('D', null),

    /** Tall Boy. 1×1. 2칸 파고든 뒤 5×5를 날린다. */
    TALL_BOY('B', new String[] {
            ".B..",
            "....",
            "....",
            "...."
    }),

    /** Crusher. 3×3. 아래쪽 줄을 최대 3줄까지 짓눌러 없앤다. */
    CRUSHER('C', new String[] {
            "CCC.",
            "CCC.",
            "CCC.",
            "...."
    });

    private final char symbol;

    /** 독립형 아이템의 모양. 포함형이면 {@code null}. */
    private final boolean[][] shape;

    ItemType(char symbol, String[] pattern) {
        this.symbol = symbol;
        this.shape = pattern == null ? null : parse(pattern);
    }

    /** 테트로미노에 붙는 포함형 아이템인지 여부 */
    public boolean isEmbedded() {
        return shape == null;
    }

    @Override
    public char symbol() {
        return symbol;
    }

    /**
     * 보드 칸에 저장할 번호. 테트로미노 번호 바로 뒤부터 이어서 붙인다.
     *
     * <p>포함형 아이템도 번호를 가진다. 붙은 칸이 보드에 고정되면 그 칸에 이 번호가 저장된다.
     */
    @Override
    public int code() {
        return BlockType.values().length + 1 + ordinal();
    }

    /**
     * 독립형 아이템의 모양. 회전 상태와 상관없이 같은 모양이다.
     *
     * @throws IllegalStateException 포함형 아이템인 경우. 포함형은 자기 모양이 없다.
     */
    @Override
    public boolean isFilled(int rotation, int row, int col) {
        if (isEmbedded()) {
            throw new IllegalStateException(this + "는 블럭에 붙는 아이템이라 자기 모양이 없습니다.");
        }
        if (row < 0 || row >= BlockType.SIZE || col < 0 || col >= BlockType.SIZE) {
            return false;
        }
        return shape[row][col];
    }

    private static boolean[][] parse(String[] pattern) {
        if (pattern.length != BlockType.SIZE) {
            throw new IllegalArgumentException("모양은 " + BlockType.SIZE + "줄이어야 합니다.");
        }
        boolean[][] result = new boolean[BlockType.SIZE][BlockType.SIZE];
        for (int r = 0; r < BlockType.SIZE; r++) {
            if (pattern[r].length() != BlockType.SIZE) {
                throw new IllegalArgumentException("모양의 각 줄은 " + BlockType.SIZE + "칸이어야 합니다.");
            }
            for (int c = 0; c < BlockType.SIZE; c++) {
                result[r][c] = pattern[r].charAt(c) != '.';
            }
        }
        return result;
    }
}
